package client;


import chainUtils.Block;
import chainUtils.NoobChain;
//import com.alibaba.fastjson.JSONObject;
import com.alipay.remoting.exception.RemotingException;
import com.google.common.collect.Lists;

//import org.json.JSONArray;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.common.Peer;
import raft.common.RDBParser;
import raft.common.ReqType;
import raft.entity.LogEntry;
import raft.impl.NodeIMPL;
import raft.impl.StateMachineIMPL;
import raft.rpc.RPCClient;
import raft.rpc.RPCReq;
import raft.rpc.RPCResp;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static client.KVReq.GET;
import static client.KVReq.PUT;

public class BlockChainClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockChainClient.class);


    private final static RPCClient client = new RPCClient();

    static String addr = "localhost:6481";
    static String redisAddr = "localhost:6381";
    static List<String> list = Lists.newArrayList("localhost:6481", "localhost:6480", "localhost" +
            ":6483");

    public static void main(String[] args) throws RemotingException, InterruptedException, ParseException {


        NoobChain nc = new NoobChain();


        //reading from state machine, get the newest blockchain
        String rdbPath = "redisConfigs/redis-" + "6381" + "/dump.rdb";
        File rdbFile = new File(rdbPath);
        //System.out.println(RDBParser.getVal(rdbFile, addr));


        if (rdbFile.exists()) {
            if (RDBParser.getVal(rdbFile, addr) != null) {
                System.out.println("Getting data from State Machine...");
                JSONParser parser = new JSONParser();
                JSONObject jsonObj = (JSONObject) parser.parse(RDBParser.getVal(rdbFile, addr));
                JSONObject transaction = (JSONObject) jsonObj.get("transaction");
                JSONObject noobChain = (JSONObject) transaction.get("noobChain");
                JSONArray blockChain = (JSONArray) noobChain.get("blockchain");
                //System.out.println(blockChain);

                for (int i = 0; i < blockChain.size(); i++) {
                    JSONObject bc = (JSONObject) blockChain.get(i);
                    ArrayList<String> list = new ArrayList<>();
                    JSONArray transactionList = (JSONArray) bc.get("transactions");

                    if (transactionList != null) {
                        for (int j = 0; j < transactionList.size(); j++) {
                            String trans = (String) transactionList.get(j);
                            list.add(trans);
                        }
                    }

                    //ArrayList<String> list1 = (ArrayList<String>) transactionList;
                    Block b = new Block(bc.get("hash").toString(), bc.get("previousHash").toString(),
                            bc.get("previousHash").toString(), list,
                            Long.parseLong(bc.get("timeStamp").toString()));

                    nc.addBlock(b);
                }
            }
        }
        System.out.println("========================\n Current BlockChain" + nc);


        Block newBlock;
        //if current BlockChain is empty, the new block has previous hash 0
        if (nc.getBlockchain().size() == 0) {
            newBlock = new Block("0");

        }
        //get the last hash as previous hash
        else {
            newBlock = new Block(nc.getBlockchain().get(nc.getBlockchain().size() - 1).hash);
        }
        ArrayList<String> test_key = new ArrayList<>();
        ArrayList<String> test_value = new ArrayList<>();
        test_key.add("c");
        test_key.add("d");
        test_value.add("3");
        test_value.add("4");
        for(int i =0;i<test_key.size();i++){
            newBlock.addTransaction(test_key.get(i)+":"+test_value.get(i));
        }
        nc.addBlock(newBlock);
//        System.out.println(nc);


        //add the new block and create a new blockchain
        KVReq obj = KVReq.builder().key(test_key).value(test_value).type(PUT).noobChain(nc).build();
        System.out.println("========================");
        System.out.println("New BlockChain successfully created " + nc + " sending to server...");


        RPCReq r = RPCReq.builder().requestType(ReqType.KV).addr(addr).param(obj).build();

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


        KVReq get = KVReq.builder().reqKey("a").type(GET).noobChain(nc).build();
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

        LOGGER.info("request content : {}, url : {}, put response : {}",
                        obj.key + "=" + obj.getValue(), r.getAddr(), response.getResult());

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

    public static LogEntry StringToObject(String s) throws ParseException {
        NoobChain nc = new NoobChain();
        JSONParser parser = new JSONParser();
        JSONObject jsonObj = (JSONObject) parser.parse(s);
        Long term = (Long) jsonObj.get("term");
        Long index = (Long) jsonObj.get("index");
        JSONObject transaction = (JSONObject) jsonObj.get("transaction");
        JSONObject noobChain = (JSONObject) transaction.get("noobChain");
        JSONArray blockChain = (JSONArray) noobChain.get("blockchain");
        //System.out.println(blockChain);

        for (int i = 0; i < blockChain.size(); i++) {
            JSONObject bc = (JSONObject) blockChain.get(i);
            ArrayList<String> list = new ArrayList<>();
            JSONArray transactionList = (JSONArray) bc.get("transactions");

            if (transactionList != null) {
                for (int j = 0; j < transactionList.size(); j++) {
                    String trans = (String) transactionList.get(j);
                    list.add(trans);
                }
            }

            //ArrayList<String> list1 = (ArrayList<String>) transactionList;
            Block b = new Block(bc.get("hash").toString(), bc.get("previousHash").toString(),
                    bc.get("previousHash").toString(), list,
                    Long.parseLong(bc.get("timeStamp").toString()));

            nc.addBlock(b);


        }
        LogEntry logEntry = new LogEntry(term,
                index, null, nc);
        return logEntry;
    }
}