package client;

import chainUtils.Block;
import chainUtils.NoobChain;
import com.alipay.remoting.exception.RemotingException;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.common.ReqType;
import raft.entity.LogEntry;
import raft.rpc.RPCClient;
import raft.rpc.RPCReq;
import raft.rpc.RPCResp;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class BlockChainClient{
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockChainClient.class);


    private final static RPCClient client = new RPCClient();

    static String addr = "localhost:6480";
    static List<String> list = Lists.newArrayList("localhost:6481", "localhost:6480", "localhost" +
            ":6483");

    public static void main(String[] args) throws RemotingException, InterruptedException {


        NoobChain nc = new NoobChain();
        KVReq obj = KVReq.builder().key("b").value("3").type(0).noobChain(new NoobChain()).build();



        RPCReq r=RPCReq.builder().requestType(ReqType.KV).addr(addr).param(obj).build();

        RPCResp response;
        try {
            response = client.sendReq(r);
        } catch (Exception e) {
            // r.setAddr(list.get((int) ((count.incrementAndGet()) % list.size())));
            response = client.sendReq(r);
        }

//        LOGGER.info("request content : {}, url : {}, put response : {}",
//                        obj.key + "=" + obj.getValue(), r.getAddr(), response.getResult());

        // SleepHelper.sleep(1000);

        //obj = KVReq.newBuilder().key("hello:" + i).type(KVReq.GET).build();

//                addr = list.get(index);
//                addr = list.get(index);
//                r.setAddr(addr);
//                r.setParam(obj);
//
//                RPCResp<LogEntry> response2;
//                try {
//                    response2 = client.sendReq(r);
//                } catch (Exception e) {
//                    r.setAddr(list.get((int) ((count.incrementAndGet()) % list.size())));
//                    response2 = client.sendReq(r);
//                }
//
//                LOGGER.info("request content : {}, url : {}, get response : {}",
//                        obj.key + "=" + obj.getValue(), r.getAddr(), response2.getResult());
//            } catch (Exception e) {
//                e.printStackTrace();
//                i = i - 1;
//            }

        //SleepHelper.sleep(5000);
//        }


    }

}
