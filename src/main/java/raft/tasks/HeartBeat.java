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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static raft.concurrent.RaftConcurrent.RaftThreadPool;

public class HeartBeat implements Runnable {
    private final static Logger LOGGER = Logger.getLogger(HeartBeat.class.getName());

    NodeIMPL node;
    // Addresses of alive peers
    Set<Peer> alivePeers;

    ExecutorService exs;

    public HeartBeat(NodeIMPL node) {
        this.node = node;
        this.exs = Executors.newFixedThreadPool(4);     // 1 self +  4 peers = 5 nodes in total
    }

    @Override
    public void run() {
        // Only leader sends Append Entry RPC with empty entries as heartbeat
        if (node.getStatus() != NodeStatus.LEADER) {
            return;
        }

        Set<Peer> peerSet = node.getPeerSet();
        alivePeers = new HashSet<>();

        int timeout = NodeIMPL.HEARTBEAT_TICK;

        CompletableFuture[] cfs = peerSet.stream()
                .map(peer -> CompletableFuture.supplyAsync(() -> sendHBReq(peer), this.exs)
                        .thenAccept(this::handleHBResp))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(cfs).join();

        if (node.getStatus() == NodeStatus.FOLLOWER) {
            RaftThreadPool.submit(new LeaderElection(node));
            return;
        }

//        System.out.println("LEADER: " + node.getAddr());
//        alivePeers.forEach(p -> System.out.println("ALIVE FOLLOWER" + p.getAddr()));

        // remove dead peers
        if (node.getStatus() == NodeStatus.LEADER) {
            peerSet.retainAll(alivePeers);
            node.setPeerSet(peerSet);
        }

//        exs.shutdown();
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
        RPCResp rpcResp = node.getRpcClient().sendReq(rpcReq);
//        System.out.println(rpcResp);
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

//        System.out.println(entryResult.getPeerAddr() + ":" + entryResult.isSuccess());
        if (entryResult.isSuccess()) {
            String addr = entryResult.getPeerAddr();
            String redisAddr = Peer.getIP(addr) + (Peer.getPort(addr) - 100);
            alivePeers.add(new Peer(addr, redisAddr));
        } else {
            // peer's term > self term and start a new election
            node.setStatus(NodeStatus.FOLLOWER);
            node.getScheduledHeartBeatTask().cancel(true);
        }
    }


}
