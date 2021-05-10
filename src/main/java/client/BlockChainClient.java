package client;

import chainUtils.Block;
import chainUtils.NoobChain;
import com.alipay.remoting.exception.RemotingException;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.common.ReqType;
import raft.rpc.RPCClient;
import raft.rpc.RPCReq;
import raft.rpc.RPCResp;

import java.util.List;

import static client.KVReq.GET;
import static client.KVReq.PUT;

public class BlockChainClient{
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockChainClient.class);


    private final static RPCClient client = new RPCClient();

    static String addr = "localhost:6481";
    static List<String> list = Lists.newArrayList("localhost:6481", "localhost:6480", "localhost" +
            ":6483");

    public static void main(String[] args) throws RemotingException, InterruptedException {


        NoobChain nc = new NoobChain();
        Block b1 = new Block(nc.getBlockchain().get(nc.getBlockchain().size()-1).hash);


        //Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        //Wallet walletA = new Wallet();


        b1.addTransaction("kkk:888");
        nc.addBlock(b1);
//        System.out.println(nc);

        KVReq obj = KVReq.builder().key("DA").value("chongchongchong").type(PUT).noobChain(nc).build();
        System.out.println("BlockChain successfully created " + nc + " sending to server...");



        RPCReq r=RPCReq.builder().requestType(ReqType.KV).addr(addr).param(obj).build();

        RPCResp response;
        try {
            response = client.sendReq(r);
            var result = (KVAck) response.getResult();
            System.out.println(result.isSuccess());
        } catch (Exception e) {
            // r.setAddr(list.get((int) ((count.incrementAndGet()) % list.size())));
            response = client.sendReq(r);
        }

        Thread.sleep(1000 * 10);


        KVReq get = KVReq.builder().key("DA").type(GET).noobChain(nc).build();
        RPCReq rg = RPCReq.builder().requestType(ReqType.KV).addr(addr).param(get).build();
        RPCResp responseg;
        try {
            responseg = client.sendReq(rg);
            var result = (KVAck) responseg.getResult();
            System.out.println(result.getVal());
        } catch (Exception e) {
            // r.setAddr(list.get((int) ((count.incrementAndGet()) % list.size())));
            responseg = client.sendReq(rg);
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
