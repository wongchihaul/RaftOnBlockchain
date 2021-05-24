package raft.rpc;

import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;


public class RPCClient implements Serializable {
    public static final Logger logger = LogManager.getLogger(RPCClient.class.getName());

    /**
     * Initiate a RPC Client
     */
    private static final RpcClient rpcClient = new RpcClient();
    /**
     * default timeout
     */
    private final int timeout = 120000;

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
//            System.out.println(addr + " " + rpcClient.checkConnection(addr));
            rpcResp = (RPCResp) rpcClient.invokeSync(addr, rpcReq, timeout);

        } catch (RemotingException e) {
            logger.warn("RPC server host cannot be found: " + addr);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while trying to send data to the RPC server: " + addr);
        }
        return rpcResp;
    }
}
