package raft.tasks;

import raft.common.NodeStatus;
import raft.common.Peer;
import raft.common.ReqType;
import raft.entity.AppEntryParam;
import raft.entity.AppEntryResult;
import raft.impl.NodeIMPL;
import raft.rpc.RPCReq;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
                        .completeOnTimeout(AppEntryResult.fail(null), timeout, TimeUnit.MILLISECONDS).
                                thenAccept(entryResult -> handleHBResp(entryResult)))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(cfs).join();

        // remove dead peers
        if (node.getStatus() == NodeStatus.LEADER) {
            peerSet.retainAll(alivePeers);
            node.setPeerSet(peerSet);
        }

        exs.shutdown();
    }

    AppEntryResult sendHBReq(Peer peer) {
        AppEntryParam appEntryParam = AppEntryParam.builder()
                .term(node.getCurrentTerm())
                .leaderId(node.getAddr())
                .logEntries(null)
                .build();

        RPCReq req = RPCReq.builder()
                .addr(peer.getAddr())
                .param(appEntryParam)
                .requestType(ReqType.APP_ENTRY)
                .build();
        // Send heartbeats to all peers exclude self
        AppEntryResult entryResult = (AppEntryResult) node.getRpcClient().sendReq(req).getResult();
        return entryResult;
    }

    void handleHBResp(AppEntryResult entryResult) {
        if (entryResult.isSuccess()) {
            alivePeers.add(new Peer(entryResult.getPeerAddr()));
        } else {
            // peer's term > self term and start a new election
            node.setStatus(NodeStatus.FOLLOWER);
            node.getScheduledHeartBeatTask().cancel(true);
            RaftThreadPool.submit(new LeaderElection(node));
        }
    }


}
