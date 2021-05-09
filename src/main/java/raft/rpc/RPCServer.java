package raft.rpc;

import client.KVReq;
import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.protocol.AbstractUserProcessor;
import jdk.jfr.consumer.RecordedStackTrace;
import raft.entity.AppEntryParam;
import raft.entity.ReqVoteParam;
import raft.entity.ReqVoteResult;
import raft.impl.NodeIMPL;

import java.sql.SQLOutput;
import java.util.logging.Logger;

@SuppressWarnings("unchecked")
public class RPCServer {
    NodeIMPL node;
    RpcServer rpcServer;
    public static final Logger logger = Logger.getLogger(RPCServer.class.getName());

    CONNECTEventProcessor serverConnectProcessor = new CONNECTEventProcessor();
    DISCONNECTEventProcessor serverDisConnectProcessor = new DISCONNECTEventProcessor();

    public RPCServer(int port, NodeIMPL node) {
        this.node = node;
        rpcServer = new RpcServer(port);
//        rpcServer.addConnectionEventProcessor(ConnectionEventType.CONNECT, serverConnectProcessor);
//        rpcServer.addConnectionEventProcessor(ConnectionEventType.CLOSE, serverDisConnectProcessor);
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
            System.out.println("server start ok!");
        } else {
            System.out.println("server start failed!");
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
                    logger.info(String.format("node{%s, status=%s} vote node{%s} for %s", node.getAddr(), node.getStatus(),
                            voteParam.getCandidateId(), true));
                } else {
                    logger.warning(String.format("node{%s, status=%s} vote node{%s} for %s", node.getAddr(), node.getStatus(),
                            voteParam.getCandidateId(), false));
                }
                break;
            case APP_ENTRY:
                result = node.handleAppEntry((AppEntryParam) rpcReq.getParam());
                break;
            case KV:
                //System.out.println("KVok!!");
                result = node.handleClientReq((KVReq) rpcReq.getParam());
                System.out.println("Successfully get result" + result);
                break;
            default:
                logger.severe("Unsupported request type");
        }

//        System.out.println(node.getAddr() + " is sending back RPC response");
        return RPCResp.builder()
                .req(rpcReq)
                .result(result)
                .build();

    }
}
