package raft.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import raft.common.NodeStatus;
import raft.common.Peer;
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

    public Replication(NodeIMPL node, LogEntry logEntryToSent) {
        this.node = node;
        this.logEntryToSent = logEntryToSent;
        this.logEntryToSent.setIndex(this.node.getLogModule().getLastIndex());
        this.exs = Executors.newFixedThreadPool(4); // 1 self +  4 peers = 5 nodes in total
    }


    @Override
    public void run() {
        if (node.getStatus() != NodeStatus.LEADER) {
            return;
        }

        Set<Peer> peerSet = node.getPeerSet();


        int timeout = NodeIMPL.REPLICATION_TIMEOUT;

        CompletableFuture[] cfs = peerSet.stream()
                .map(peer -> CompletableFuture.supplyAsync(() -> sendReplication(peer, logEntryToSent), this.exs)
                        //.completeOnTimeout(false, timeout, TimeUnit.MILLISECONDS)
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


        // 如果存在一个满足N > commitIndex的 N，并且大多数的matchIndex[i] ≥ N成立，
        // 并且log[N].term == currentTerm成立，那么令 commitIndex 等于这个 N （5.3 和 5.4 节）
//        var indexes = node.getLatestIndexes().values().toArray(new Long[0]);
//        Arrays.sort(indexes);
//        Long median = indexes.length >= 2 ? indexes[indexes.length / 2 - 1] : 0;
//        System.out.println("cuocuo"+ node.getLogModule().read(median));
//        if (node.getLogModule().read(median).getTerm() == node.getCurrentTerm()
//                && median > node.getCommitIndex()) {
//            node.setCommitIndex(median);
//        }
        System.out.println(replicaCount.get());
        if (replicaCount.get() > (peerSet.size() / 2 + 1)) {
            logger.info("Replication voting OK");
            logger.info("logEntryToSent index: " + logEntryToSent.getIndex());

            node.setCommitIndex(node.getLogModule().getLastIndex());
            logger.info("Leader apply entry to statemachine now, entry is "+ logEntryToSent);
            node.getStateMachine().apply(logEntryToSent);
            node.setLastApplied(logEntryToSent.getIndex());
//            return ClientKVAck.ok();
        } else {
            // 回滚已经提交的日志.
            node.getLogModule().removeLogs(logEntryToSent.getIndex());
            logger.warn("fail apply local state  machine,  logEntry info : " + logEntryToSent.toString());
            // TODO 不应用到状态机,但已经记录到日志中.由定时任务从重试队列取出,然后重复尝试,当达到条件时,应用到状态机.
            // 这里应该返回错误, 因为没有成功复制过半机器.
//            return ClientKVAck.fail();
        }
    }

    // After write to local logs
    Boolean sendReplication(Peer peer, LogEntry logEntry) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime;
        while(endTime - startTime < 3 * 1000L) {
            // The first time LEADER sends replication RPC to FOLLOWERs
            long nextIndex = node.getNextIndexes().get(peer);
            ArrayList<LogEntry> logEntriesToSend = new ArrayList<>();
//            LOGGER.info("nextIndex" + nextIndex + " EntriesToSend: " + logEntriesToSend);
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
//                LOGGER.info("LogEntriesToSend new  " + logEntriesToSend.toString());
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
                        //nextIndex = nextIndex == 0 ? 1 : nextIndex;
                        if(nextIndex==0){
                            break;
                        }

                        node.getNextIndexes().put(peer, nextIndex - 1);
                        logger.info(String.format("Append log entries: %s to follower: %s failed, index not matches", logEntry, replicaResult.getPeerAddr()));
                    }
                }
            }
            endTime = System.currentTimeMillis();
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
           // entry = LogEntry.builder().index(0L).term(0).transaction(null).noobChain(null)
            // .build();
            entry = new LogEntry(0,0L,null,null);
        }
        else{

            entry = node.getLogModule().read(logEntry.getIndex() - 1);
            logger.warn("previous entry not null, is "+ entry);
        }


//        if (entry == null) {
//            LOGGER.warning("get perLog is null , parameter logEntry : " + logEntry);
//            entry = LogEntry.builder().index(0L).term(0).transaction(null).build();
//        }
        return entry;
    }


}
