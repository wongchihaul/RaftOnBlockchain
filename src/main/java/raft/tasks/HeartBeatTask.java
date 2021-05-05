package raft.tasks;

import raft.LogModule;
import raft.common.NodeStatus;
import raft.common.Peer;
import raft.common.ReqType;
import raft.concurrent.RaftThreadPool;
import raft.entity.AppEntryParam;
import raft.entity.AppEntryResult;
import raft.impl.NodeIMPL;
import raft.rpc.RPCReq;
import raft.rpc.RPCResp;

import java.util.ArrayList;
import java.util.logging.Logger;

public class HeartBeatTask implements Runnable {
    private final static Logger logger = Logger.getLogger(HeartBeatTask.class.getName());
    private NodeIMPL node;

    public HeartBeatTask(NodeIMPL node) {
        this.node = node;
    }

    @Override
    public void run() {
        // Only leader sends Append Entry RPC with empty entries as heartbeat
        if (node.getStatus() != NodeStatus.LEADER) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - node.getLastHeartBeatTime() < NodeIMPL.HEARTBEAT_TICK) {
            return;
        }
        node.setLastHeartBeatTime(System.currentTimeMillis());

        // send heartbeat
        for (Peer peer: node.getPeerSet()) {
            LogModule logModule = node.getLogModule();
            AppEntryParam appEntryParam = AppEntryParam.builder()
                    .term(node.getCurrentTerm())
                    .leaderId(node.getAddr())
                    .prevLogIndex(logModule.getLastIndex())
                    .prevLogTerm(logModule.getLast().getTerm())
                    .logEntries(new ArrayList<>())
                    .leaderCommit(node.getCommitIndex())
                    .build();
            RPCReq<AppEntryParam> request = new RPCReq<>(
                    ReqType.APP_ENTRY,
                    peer.getAddr(),
                    appEntryParam
            );

            RaftThreadPool.execute(() -> {
                try {

                } catch (Exception e) {
                    logger.severe(String.format("Heartbeat task fail %s", request.getAddr()));
                }
                RPCResp response = node.getRpcClient().sendReq(request);
                if (response.getReq().getRequestType() != ReqType.APP_ENTRY) {
                    // Unknown ReqType
                    logger.warning(String.format("Incorrect request type for heartbeat response:\n %s", response.getReq()));
                } else {
                    if (!((AppEntryResult) response.getResult()).isSuccess()) {
                        // here it should get term from appEntryResult
//                        node.setCurrentTerm();
                        node.setStatus(NodeStatus.FOLLOWER);
                    }
                }
            }, false);

        }
    }
}
