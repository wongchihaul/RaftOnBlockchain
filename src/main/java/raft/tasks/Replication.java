package raft.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import raft.common.NodeStatus;
import raft.common.Peer;
import raft.common.PeerSet;
import raft.common.ReqType;
import raft.entity.AppEntryParam;
import raft.entity.AppEntryResult;
import raft.entity.LogEntry;
import raft.impl.NodeIMPL;
import raft.rpc.RPCReq;
import raft.rpc.RPCResp;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static raft.concurrent.RaftConcurrent.RaftThreadPool;


public class Replication implements Runnable {
    private final static Logger logger = LogManager.getLogger(HeartBeat.class.getName());

    NodeIMPL node;

    ExecutorService exs;

    AtomicInteger replicaCount = new AtomicInteger(0);

    LogEntry logEntryToSent;

    boolean demoVersion;

    public Replication(NodeIMPL node, LogEntry logEntryToSent, boolean demoVersion) {
        this.node = node;
        this.logEntryToSent = logEntryToSent;
        this.logEntryToSent.setIndex(this.node.getLogModule().getLastIndex());
        this.exs = Executors.newFixedThreadPool(6); // 1 self +  4 peers = 5 nodes in total
        this.demoVersion = demoVersion;
    }


    @Override
    public void run() {
        if (node.getStatus() != NodeStatus.LEADER) {
            return;
        }

        Set<Peer> peerSet = PeerSet.getOthers(node.getPeer());;



        CompletableFuture[] cfs = peerSet.stream()
                .map(peer -> CompletableFuture.supplyAsync(() -> sendReplication(peer, logEntryToSent, demoVersion), this.exs)
                        .thenAccept(replicaResult -> {
                            if (replicaResult)
                                replicaCount.incrementAndGet();
                        }))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(cfs).join();


        if (node.getStatus() == NodeStatus.FOLLOWER) {
            RaftThreadPool.submit(new LeaderElection(node));
            return;
        }


        System.out.println(replicaCount.get());
        if (replicaCount.get() > (peerSet.size() / 2 + 1)) {
            logger.info("Replication voting OK");
            logger.info("logEntryToSent index: " + logEntryToSent.getIndex());

            node.setCommitIndex(node.getLogModule().getLastIndex());
            logger.info("Leader apply entry to statemachine now, entry is " + logEntryToSent);
            node.getStateMachine().apply(logEntryToSent);
            node.setLastApplied(logEntryToSent.getIndex());
        } else {
            // rollback committed logs
            node.getLogModule().removeLogs(logEntryToSent.getIndex());
            logger.warn("fail apply local state  machine,  logEntry info : " + logEntryToSent.toString());
        }
    }


    Boolean sendReplication(Peer peer, LogEntry logEntry, boolean demoVersion) {
        if (demoVersion) {
            try {
                Thread.sleep(1000 * 10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return sendReplication(peer, logEntry);
        } else {
            return sendReplication(peer, logEntry);
        }

    }


    // After write to local logs
    Boolean sendReplication(Peer peer, LogEntry logEntry) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime;
        while (endTime - startTime < 3 * 1000L) {
            // The first time LEADER sends replication RPC to FOLLOWERs
            long nextIndex = node.getNextIndexes().get(peer);
            ArrayList<LogEntry> logEntriesToSend = new ArrayList<>();
            if (logEntry.getIndex() > nextIndex) {
                System.out.println("logEntry Index larger");
                for (long i = nextIndex; i <= logEntry.getIndex(); i++) {
                    LogEntry recordEntry = node.getLogModule().read(i);
                    if (recordEntry != null) {
                        logEntriesToSend.add(recordEntry);
                    }
                }
            } else {
                logEntriesToSend.add(logEntry);
            }

            LogEntry prevLog = getPrevLog(logEntry);

            AppEntryParam appEntryParam = AppEntryParam.builder()
                    .term(node.getCurrentTerm())
                    .leaderId(node.getAddr())
                    .leaderCommit(node.getCommitIndex())
                    .prevLogIndex(prevLog.getIndex())
                    .prevLogTerm(prevLog.getTerm())
                    .logEntries(logEntriesToSend)
                    .build();
            RPCReq rpcReq = RPCReq.builder()
                    .requestType(ReqType.APP_ENTRY)
                    .param(appEntryParam)
                    .addr(peer.getAddr())
                    .build();
            RPCResp replicaResp = node.getRpcClient().sendReq(rpcReq);
            System.out.println("replicate appentry " + appEntryParam);

            // handle response here
            if (replicaResp == null) {
                logger.warn("replica Response is null");
                return false;
            }
            logger.info("RPC response: " + replicaResp);
            AppEntryResult replicaResult = (AppEntryResult) replicaResp.getResult();

            logger.info("replicaResult: " + replicaResult);
            if (replicaResult != null) {
                if (replicaResult.isSuccess()) {
                    logger.info(String.format("Append log entries: %s to follower: %s success",
                            logEntry.toString(), replicaResult.getPeerAddr()));
                    node.getNextIndexes().put(peer, node.getLogModule().getLastIndex() + 1);
                    node.getLatestIndexes().put(peer, node.getLogModule().getLastIndex());
                    return true;
                } else {
                    // Peer has larger term, turn self to FOLLOWER and start a new election;
                    if (replicaResult.getTerm() > node.getCurrentTerm()) {
                        logger.info(String.format("The term of peer: %s is larger, becomes follower now", peer.getAddr()));
                        node.setCurrentTerm(replicaResult.getTerm());
                        node.setStatus(NodeStatus.FOLLOWER);
                        return false;
                    } else {
                        // Peer has smaller term, caused by mismatch index, try to decrease 1
                        if(nextIndex==0){
                            break;
                        }

                        node.getNextIndexes().put(peer, nextIndex - 1);
                        logger.info(String.format("Append log entries: %s to follower: %s failed, index not matches", logEntry, replicaResult.getPeerAddr()));
                    }
                }
            }
            return sendReplication(peer, logEntry);
        }
        logger.warn("replication failed because of timeout");
        return false;
    }


    private LogEntry getPrevLog(LogEntry logEntry) {
        logger.info("logEntry index is " + logEntry.getIndex());
        LogEntry entry;
        if(logEntry.getIndex()==0){
            logger.warn("get perLog is null , parameter logEntry : " + logEntry);
            entry = new LogEntry(0,0L,null,null);
        }
        else{

            entry = node.getLogModule().read(logEntry.getIndex() - 1);
            logger.warn("previous entry not null, is "+ entry);
        }
        return entry;
    }


}
