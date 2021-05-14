package client;


import chainUtils.Block;
import chainUtils.NoobChain;
import com.alipay.remoting.exception.RemotingException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import raft.common.Peer;
import raft.common.ReqType;
import raft.entity.LogEntry;
import raft.rpc.RPCClient;
import raft.rpc.RPCReq;
import raft.rpc.RPCResp;

import java.io.File;
import java.util.ArrayList;

import static client.BlockChainTestClient.disableWarning;
import static client.KVReq.GET;
import static client.KVReq.PUT;


public class BlockChainAutoClient {
    private static final Logger LOGGER = LogManager.getLogger(BlockChainAutoClient.class);

    private final static RPCClient client = new RPCClient();

    static String addr = "localhost:6480";

    static String anotherAddr = "localhost:6483";

    public static void main(String[] args) throws RemotingException, InterruptedException, ParseException {

        disableWarning();

        Options options = new Options();
        options.addOption("demo", true, "in demo mode, raft leader will postpone the replication");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e1) {
            ;
        }

        boolean demo = false;
        if (cmd.hasOption("demo")) {
            try {
                demo = Boolean.valueOf(cmd.getOptionValue("demo"));
            } catch (Exception e) {
                ;
            }
        }

        //reading from state machine, get the newest blockchain
        String rdbPath = "redisConfigs/redis-" + (Peer.getPort(addr) - 100) + "/dump.rdb";
        File rdbFile = new File(rdbPath);

        NoobChain nc = BlockChainTestClient.getCurrentChain(addr, String.valueOf(Peer.getPort(addr) - 100));


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
        test_key.add("Pyke");
        test_value.add("Mid");
        for(int i =0;i<test_key.size();i++){
            newBlock.addTransaction(test_key.get(i)+":"+test_value.get(i));
        }
        nc.addBlock(newBlock);


        //add the new block and create a new blockchain
        KVReq obj = KVReq.builder().key(test_key).value(test_value).type(PUT).noobChain(nc).demoVersion(demo).build();
        System.out.println("========================");
        System.out.println("New BlockChain successfully created " + nc + " sending to server...");


        RPCReq r = RPCReq.builder().requestType(ReqType.KV).addr(addr).param(obj).build();

        RPCResp response;
        try {
            response = client.sendReq(r);
            var result = (KVAck) response.getResult();
            System.out.println(result.isSuccess());
        } catch (Exception e) {
            response = client.sendReq(r);
        }

        Thread.sleep(1000 * 5);


        KVReq get = KVReq.builder().reqKey(test_key.get(0)).type(GET).noobChain(nc).demoVersion(demo).build();
        RPCReq rg1 = RPCReq.builder().requestType(ReqType.KV).addr(addr).param(get).build();
        RPCReq rg2 = RPCReq.builder().requestType(ReqType.KV).addr(anotherAddr).param(get).build();
        RPCResp responseg1;
        RPCResp responseg2;
        try {
            responseg1 = client.sendReq(rg1);
            var result1 = (KVAck) responseg1.getResult();
            System.out.println("Value from (localhost:6480): " + result1.getVal());
            responseg2 = client.sendReq(rg2);
            var result2 = (KVAck) responseg2.getResult();
            System.out.println("Value from (localhost:6483): " + result2.getVal());
        } catch (Exception e) {
        }

        LOGGER.info("request content : {}, url : {}, put response : {}",
                        obj.key + "=" + obj.getValue(), r.getAddr(), response.getResult());

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