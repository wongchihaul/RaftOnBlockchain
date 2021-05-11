package client;


import chainUtils.Block;
import chainUtils.NoobChain;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import raft.common.RDBParser;
import raft.common.ReqType;
import raft.rpc.RPCClient;
import raft.rpc.RPCReq;
import raft.rpc.RPCResp;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Scanner;

import static client.KVReq.GET;
import static client.KVReq.PUT;

public class BlockChainTestClient {
    private static String addr = "localhost:6481";
    private final static RPCClient client = new RPCClient();


    public static void main(String[] args) throws ParseException {
        Scanner kbd = new Scanner(System.in);
        int option =0;

        while(option !=3) {
            System.out.println("Welcome to Raft BlockChain, please choose from the following options" +
                    "(1/2), or enter 3 to EXIT" + "\n" + "1.GET \n2.PUT \n3.EXIT");
            option = kbd.nextInt();

            NoobChain nc = getCurrentChain(addr);


            switch (option) {
                case 2:
                    putOption(kbd, nc);
                    break;
                case 1:
                    getOption(kbd, nc);
                    break;
                case 3:
                    System.out.println("Bye Bye!!!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Wrong option!!");

            }
        }

    }

    public static void getOption(Scanner sc, NoobChain nc){

        String s = "Y";
        RPCResp responseg;
        RPCReq rg;
        while(!s.equalsIgnoreCase("N")){
            System.out.println("Please input a Key you want to search");
            String key = sc.next();
            KVReq get = KVReq.builder().reqKey(key).type(GET).noobChain(nc).build();
            rg = RPCReq.builder().requestType(ReqType.KV).addr(addr).param(get).build();
            responseg = client.sendReq(rg);
            var result = (KVAck) responseg.getResult();

            if(result.getVal() != null){
                System.out.println("The value of the key is " + result.getVal());
            }
            else{
                System.out.println("The key not exist");
            }
            System.out.println("Do you want to search another key? (Y/N)");
            s = sc.next();
       }
    }


    public static void putOption(Scanner sc, NoobChain nc ){
        String s = "Y";
        ArrayList<String> test_key = new ArrayList<>();
        ArrayList<String> test_value = new ArrayList<>();

        while(!s.equalsIgnoreCase("N")){
        System.out.println("Please input a Key Value pair to add to the transaction: (eg. hello " +
                "3)");
            String key = sc.next();
            String value = sc.next();
            test_key.add(key);
            test_value.add(value);
            System.out.println("Do you want to add another transaction? (Y/N)");
            s = sc.next();
        }
        Block newBlock = generateBlock(nc,test_key, test_value);
        System.out.println("The new Block Created: "+newBlock.toString());

        nc.addBlock(newBlock);
        KVReq obj = KVReq.builder().key(test_key).value(test_value).type(PUT).noobChain(nc).build();
        System.out.println("Adding the new Block to the Chain...........");
        RPCReq r = RPCReq.builder().requestType(ReqType.KV).addr(addr).param(obj).build();

        RPCResp response;
        try {
            response = client.sendReq(r);
            var result = (KVAck) response.getResult();
            System.out.println("Updating BlockChain result: "+ result.isSuccess());
        } catch (Exception e) {
            // r.setAddr(list.get((int) ((count.incrementAndGet()) % list.size())));
            response = client.sendReq(r);
        }

    }

    public static Block generateBlock(NoobChain nc, ArrayList<String> test_key,
                                      ArrayList<String> test_value){
        Block newBlock;
        //if current BlockChain is empty, the new block has previous hash 0
        if (nc.getBlockchain().size() == 0) {
            newBlock = new Block("0");

        }
        //get the last hash as previous hash
        else {
            newBlock = new Block(nc.getBlockchain().get(nc.getBlockchain().size() - 1).hash);
        }
        for(int i =0;i<test_key.size();i++){
            newBlock.addTransaction(test_key.get(i)+":"+test_value.get(i));
        }
        return newBlock;
    }



    public static NoobChain getCurrentChain(String addr) throws ParseException {
        NoobChain nc = new NoobChain();
        //reading from state machine, get the newest blockchain
        String rdbPath = "redisConfigs/redis-" + "6381" + "/dump.rdb";
        File rdbFile = new File(rdbPath);
        //System.out.println(RDBParser.getVal(rdbFile, addr));
        if (rdbFile.exists()) {
            if (RDBParser.getVal(rdbFile, addr) != null) {
                //System.out.println("Getting data from State Machine...");
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
        return nc;
    }


}