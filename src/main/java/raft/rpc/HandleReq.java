package raft.rpc;

import client.KVReq;
import raft.entity.AppEntryParam;
import raft.entity.ReqVoteParam;
import raft.impl.NodeIMPL;

import java.rmi.Remote;
import java.util.logging.Logger;

public class HandleReq implements Remote {
    public static final Logger LOGGER = Logger.getLogger(HandleReq.class.getName());

    public RPCResp handleReq(RPCReq rpcReq, NodeIMPL node) {
        boolean result = false;
        switch (rpcReq.getRequestType()) {
            case REQ_VOTE:
                result = node.handleReqVote((ReqVoteParam) rpcReq.getParam()).isVoteGranted();
                break;
            case APP_ENTRY:
                result = node.handleAppEntry((AppEntryParam) rpcReq.getParam()).isSuccess();
                break;
            case KV:
                result = node.handleClientReq((KVReq) rpcReq.getParam()).isSuccess();
                break;
            default:
                LOGGER.severe("Unsupported request type");
        }

        return RPCResp.builder()
                .req(rpcReq)
                .result(result)
                .build();

    }
}
