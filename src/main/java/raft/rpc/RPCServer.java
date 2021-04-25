package raft.rpc;

import client.KVReq;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import raft.entity.AppEntryParam;
import raft.entity.ReqVoteParam;
import raft.impl.NodeIMPL;

import java.util.logging.Logger;


public class RPCServer {
    NodeIMPL node;
    RpcServer rpcServer;
    public static final Logger logger = Logger.getLogger(RPCClient.class.getName());

    public RPCServer(int port, NodeIMPL node) {
        this.node = node;
        rpcServer = new RpcServer(port);
        rpcServer.registerUserProcessor(new SyncUserProcessor<RPCReq>() {
            @Override
            public Object handleRequest(BizContext bizContext, RPCReq rpcReq) throws Exception {
                return handleRequest(bizContext, rpcReq);
            }

            @Override
            public String interest() {
                return null;
            }
        });
    }

    public void start() {
        rpcServer.start();
    }

    public void stop() {
        rpcServer.stop();
    }

    public RPCResp handleRequest(BizContext bizContext, RPCReq rpcReq) {
        boolean result = false;
        switch (rpcReq.getRequest()) {
            case "REQ_VOTE":
                result = node.handleReqVote((ReqVoteParam) rpcReq.getParam()).isVoteGranted();
                break;
            case "APP_ENTRY":
                result = node.handlerAppEntry((AppEntryParam) rpcReq.getParam()).isSuccess();
                break;
            case "KV":
                result = node.handleClientReq((KVReq) rpcReq.getParam()).isSuccess();
                break;
            default:
                logger.severe("Unsupported request type");
        }

        return RPCResp.builder()
                .req(rpcReq)
                .result(result)
                .build();

    }
}
