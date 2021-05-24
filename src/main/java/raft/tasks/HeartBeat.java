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

import static raft.concurrent.RaftConcurrent.RaftThreadPool;

public class HeartBeat implements Runnable {
    private final static Logger logger = LogManager.getLogger(HeartBeat.class.getName());

    NodeIMPL node;
    ExecutorService exs;

    public HeartBeat(NodeIMPL node) {
        this.node = node;
        this.exs = Executors.newFixedThreadPool(6);     // 1 self +  4 peers = 5 nodes in total
    }

    @Override
    public void run() {
        // Only leader sends Append Entry RPC with empty entries as heartbeat
        if (node.getStatus() != NodeStatus.LEADER) {
            node.getScheduledHeartBeatTask().cancel(true);
            return;
        }

        Set<Peer> peerSet = PeerSet.getOthers(node.getPeer());

        CompletableFuture[] cfs = peerSet.stream()
                .map(peer -> CompletableFuture.supplyAsync(() -> sendHBReq(peer), this.exs)
                        .thenAccept(this::handleHBResp))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(cfs).join();

        if (node.getStatus() == NodeStatus.FOLLOWER) {
            RaftThreadPool.submit(new LeaderElection(node));
            return;
        }

        if (node.getStatus() == NodeStatus.LEADER) {
            node.setPeerSet(peerSet);
        }

    }

    RPCResp sendHBReq(Peer peer) {
        AppEntryParam appEntryParam = AppEntryParam.builder()
                .term(node.getCurrentTerm())
                .leaderId(node.getAddr())
                .logEntries(new ArrayList<LogEntry>())
                .build();

        RPCReq rpcReq = RPCReq.builder()
                .addr(peer.getAddr())
                .param(appEntryParam)
                .requestType(ReqType.APP_ENTRY)
                .build();

        // Send heartbeats to all peers exclude self
//        logger.info(String.format("node{%s} send heartbeat to node{%s}", node.getAddr(), peer.getAddr()));
        RPCResp rpcResp = node.getRpcClient().sendReq(rpcReq);
        return rpcResp;
    }

    void handleHBResp(RPCResp rpcResp) {
        if (rpcResp == null) {
            return;
        }
        AppEntryResult entryResult = (AppEntryResult) rpcResp.getResult();

        if (entryResult == null) {
            return;
        }

        if (entryResult.isSuccess()) {
            String addr = entryResult.getPeerAddr();
            String redisAddr = Peer.getIP(addr) + (Peer.getPort(addr) - 100);
        } else {
            // peer's term > self term and start a new election
            node.setStatus(NodeStatus.FOLLOWER);
            node.getScheduledHeartBeatTask().cancel(true);
        }
    }


}
