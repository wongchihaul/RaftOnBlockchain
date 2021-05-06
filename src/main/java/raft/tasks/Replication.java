package raft.tasks;

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
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static raft.concurrent.RaftConcurrent.RaftThreadPool;

public class Replication implements Runnable {
    private final static Logger LOGGER = Logger.getLogger(HeartBeat.class.getName());

    NodeIMPL node;

    ExecutorService exs;

    AtomicInteger replicaCount;

    LogEntry logEntryToSent;

    public Replication(NodeIMPL node, LogEntry logEntryToSent) {
        this.node = node;
        this.logEntryToSent = logEntryToSent;
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
                        .completeOnTimeout(false, timeout, TimeUnit.MILLISECONDS)
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
        Long[] indexes = node.getLatestIndexes().values().toArray(new Long[0]);
        Arrays.sort(indexes);
        Long median = indexes.length >= 2 ? indexes[indexes.length / 2 - 1] : 0;
        if (node.getLogModule().read(median).getTerm() == node.getCurrentTerm()
                && median > node.getCommitIndex()) {
            node.setCommitIndex(median);
        }

        if (replicaCount.get() > peerSet.size() / 2 + 1) {
            node.setCommitIndex(logEntryToSent.getIndex());
            node.getStateMachine().apply(logEntryToSent);
            node.setLastApplied(logEntryToSent.getIndex());
//            return ClientKVAck.ok();
        } else {
            // 回滚已经提交的日志.
            node.getLogModule().removeLogs(logEntryToSent.getIndex());
            LOGGER.warning("fail apply local state  machine,  logEntry info : " + logEntryToSent.toString());
            // TODO 不应用到状态机,但已经记录到日志中.由定时任务从重试队列取出,然后重复尝试,当达到条件时,应用到状态机.
            // 这里应该返回错误, 因为没有成功复制过半机器.
//            return ClientKVAck.fail();
        }
    }

    // After write to local logs
    Boolean sendReplication(Peer peer, LogEntry logEntry) {
        // The first time LEADER sends replication RPC to FOLLOWERs
        long nextIndex = node.getNextIndexes().get(peer);
        ArrayList<LogEntry> logEntriesToSend = new ArrayList<>();
        if (logEntry.getIndex() >= nextIndex) {
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
                .prevLogIndex(prevLog.getIndex())
                .logEntries(logEntriesToSend)
                .build();
        RPCReq rpcReq = RPCReq.builder()
                .requestType(ReqType.APP_ENTRY)
                .param(appEntryParam)
                .addr(peer.getAddr())
                .build();
        RPCResp replicaResp = node.getRpcClient().sendReq(rpcReq);

        // handle response here
        if (replicaResp == null) {
            return false;
        }
        AppEntryResult replicaResult = (AppEntryResult) replicaResp.getResult();

        if (replicaResult != null) {
            if (replicaResult.isSuccess()) {
                LOGGER.info(String.format("Append log entries: %s to follower: %s success", logEntry, replicaResult.getPeerAddr()));
                node.getNextIndexes().put(peer, logEntry.getIndex() + 1);
                node.getLatestIndexes().put(peer, logEntry.getIndex());
                return true;
            } else {
                // Peer has larger term, turn self to FOLLOWER and start a new election;
                if (replicaResult.getTerm() > node.getCurrentTerm()) {
                    LOGGER.info(String.format("The term of peer: %s is larger, becomes follower now", peer.getAddr()));
                    node.setCurrentTerm(replicaResult.getTerm());
                    node.setStatus(NodeStatus.FOLLOWER);
                    return false;
                } else {
                    // Peer has smaller term, caused by mismatch index, try to decrease 1
                    nextIndex = nextIndex == 0 ? 1 : nextIndex;
                    node.getNextIndexes().put(peer, nextIndex - 1);
                    LOGGER.info(String.format("Append log entries: %s to follower: %s failed, index not matches", logEntry, replicaResult.getPeerAddr()));
                }
            }
        }
        return sendReplication(peer, logEntry);
    }


    private LogEntry getPrevLog(LogEntry logEntry) {
        LogEntry entry = node.getLogModule().read(logEntry.getIndex() - 1);

        if (entry == null) {
            LOGGER.warning("get perLog is null , parameter logEntry : " + logEntry);
            entry = LogEntry.builder().index(0L).term(0).transaction(null).build();
        }
        return entry;
    }


}
