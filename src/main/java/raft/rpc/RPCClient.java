package raft.rpc;

import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;

import java.util.logging.Logger;


public class RPCClient {
    public static final Logger logger = Logger.getLogger(RPCClient.class.getName());

    /**
     * default timeout
     */
    private int timeout = 120000;

    /**
     * Initiate a RPC Client
     */
    public static RpcClient rpcClient = new RpcClient();

    static {
        rpcClient.init();
    }

    public RPCResp sendReq(RPCReq rpcReq) {
        return sendReq(rpcReq, this.timeout);
    }

    public RPCResp sendReq(RPCReq rpcReq, int timeout) {
        RPCResp rpcResp = null;
        String addr = rpcReq.getAddr();
        try {
            rpcResp = (RPCResp) rpcClient.invokeSync(addr, rpcReq, timeout);
        } catch (RemotingException e) {
            logger.severe("RPC server host cannot be found: " + addr);
        } catch (InterruptedException e) {
            logger.info("Interrupted while trying to send data to the RPC server: " + addr);
        }
        return rpcResp;
    }
}
