package raft.rpc;

import client.KVReq;
import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.protocol.AbstractUserProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import raft.entity.AppEntryParam;
import raft.entity.ReqVoteParam;
import raft.entity.ReqVoteResult;
import raft.impl.NodeIMPL;



@SuppressWarnings("unchecked")
public class RPCServer {
    NodeIMPL node;
    RpcServer rpcServer;
    public static final Logger logger = LogManager.getLogger(RPCServer.class.getName());

    public RPCServer(int port, NodeIMPL node) {

        this.node = node;
        rpcServer = new RpcServer(port);
        rpcServer.registerUserProcessor(new AbstractUserProcessor<RPCReq>() {
            @Override
            public void handleRequest(BizContext bizContext, AsyncContext asyncContext, RPCReq rpcReq) {
            }

            @Override
            public RPCResp handleRequest(BizContext bizContext, RPCReq rpcReq) {
                return handleReq(rpcReq);
            }

            @Override
            public String interest() {
                return RPCReq.class.getName();
            }
        });
    }

    public void start() {
        if (rpcServer.start()) {
            logger.info(String.format("server{%s} start ok!", node.getAddr()));
        } else {
            logger.error(String.format("server{%s} start failed!", node.getAddr()));
        }
    }

    public void stop() {
        rpcServer.stop();
    }

    public RPCResp handleReq(RPCReq rpcReq) {
        Object result = false;
        switch (rpcReq.getRequestType()) {

            case REQ_VOTE:
                ReqVoteParam voteParam = (ReqVoteParam) rpcReq.getParam();
                result = node.handleReqVote(voteParam);
                boolean voteResult = ((ReqVoteResult) result).isVoteGranted();
                if (voteResult) {
                    logger.info(String.format("node{%s, status=%s} vote node{%s} for %s",
                            node.getAddr(), node.getStatus(), voteParam.getCandidateId(), true));
                } else {
                    logger.warn(String.format("node{%s, status=%s} vote node{%s} for %s, because it vote for node{%s}",
                            node.getAddr(), node.getStatus(),
                            voteParam.getCandidateId(), false, node.getVotedFor()));
                }
                break;

            case APP_ENTRY:
                result = node.handleAppEntry((AppEntryParam) rpcReq.getParam());
                break;

            case KV:
                result = node.handleClientReq((KVReq) rpcReq.getParam());
                System.out.println(node.getAddr() + " Successfully get result" + result);
                break;

            default:
                logger.error("Unsupported request type");
        }

//        System.out.println(node.getAddr() + " is sending back RPC response");
        return RPCResp.builder()
                .req(rpcReq)
                .result(result)
                .build();

    }
}
