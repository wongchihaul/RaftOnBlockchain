package raft.impl;

import client.KVAck;
import client.KVReq;
import raft.Node;
import raft.config.NodeConfig;
import raft.entity.AppEntryParam;
import raft.entity.AppEntryResult;
import raft.entity.ReqVoteParam;
import raft.entity.ReqVoteResult;

public class NodeIMPL implements Node {
    @Override
    public void setConfig(NodeConfig config) {

    }

    @Override
    public ReqVoteResult handleReqVote(ReqVoteParam param) {
        return null;
    }

    @Override
    public AppEntryResult handlerAppEntry(AppEntryParam param) {
        return null;
    }

    @Override
    public KVAck handleClientReq(KVReq req) {
        return null;
    }

    @Override
    public KVAck redirect(KVReq req) {
        return null;
    }
}
