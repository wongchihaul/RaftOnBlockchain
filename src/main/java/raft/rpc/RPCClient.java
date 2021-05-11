package raft.rpc;

import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import raft.common.ReqType;

import java.io.Serializable;
import java.util.logging.Logger;


public class RPCClient implements Serializable {
    public static final Logger logger = Logger.getLogger(RPCClient.class.getName());

    /**
     * Initiate a RPC Client
     */
    private static final RpcClient rpcClient = new RpcClient();
    /**
     * default timeout
     */
    private final int timeout = 120000;

    static {
//        rpcClient.addConnectionEventProcessor(ConnectionEventType.CONNECT, clientConnectProcessor);
//        rpcClient.addConnectionEventProcessor(ConnectionEventType.CLOSE, clientDisConnectProcessor);
        rpcClient.init();
    }


    public RPCResp sendReq(RPCReq rpcReq) {
        return sendReq(rpcReq, this.timeout);
    }

    public RPCResp sendReq(RPCReq rpcReq, int timeout) {
        RPCResp rpcResp = null;
        String addr = rpcReq.getAddr();
        try {
//            System.out.println(addr + " " + rpcClient.checkConnection(addr));
            if(rpcReq.getRequestType()== ReqType.KV){
                System.out.println("getting request from client....");
            }
            rpcResp = (RPCResp) rpcClient.invokeSync(addr, rpcReq, timeout);
            if(rpcReq.getRequestType()== ReqType.KV){
                System.out.println("got request from client" + rpcResp);
            }

        } catch (RemotingException e) {
            logger.severe("RPC server host cannot be found: " + addr);
            e.printStackTrace();
        } catch (InterruptedException e) {
            logger.severe("Interrupted while trying to send data to the RPC server: " + addr);
        }
        return rpcResp;
    }
}
