package demo;

import client.KVAck;
import client.KVReq;
import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.protocol.AbstractUserProcessor;
import raft.common.Peer;
import raft.common.PeerSet;
import raft.common.RDBParser;
import raft.rpc.RPCClient;
import raft.rpc.RPCReq;
import raft.rpc.RPCResp;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static client.KVReq.PUT;
import static raft.common.RedisPool.setConfig;


public class NoRaftPool {
    static final int FIRST = 6380;
    static final int LAST = 6386;
    static JedisPool[] jedisPools = new JedisPool[LAST - FIRST + 1];
    static RpcServer[] rpcServers = new RpcServer[LAST - FIRST + 1];

    public static void main(String[] args) {

        for (int i = FIRST; i <= LAST; i++) {
            String addr = "localhost:" + (i + 100);
            String redisAddr = "localhost:" + i;
            Peer peer = new Peer(addr, redisAddr);
            PeerSet.peerSet.add(peer);
            JedisPool jedisPool = new JedisPool(setConfig(), Peer.getIP(redisAddr), Peer.getPort(redisAddr));
            jedisPools[i - FIRST] = jedisPool;
        }

        for (int i = FIRST; i <= LAST; i++) {
            String addr = "localhost:" + (i + 100);
            String redisAddr = "localhost:" + i;
            RpcServer rpcServer = new RpcServer(i + 100);
            rpcServers[i - FIRST] = rpcServer;
            if (i == FIRST) {
                rpcServer.registerUserProcessor(new AbstractUserProcessor<RPCReq>() {
                    @Override
                    public void handleRequest(BizContext bizContext, AsyncContext asyncContext, RPCReq rpcReq) {
                    }

                    @Override
                    public RPCResp handleRequest(BizContext bizContext, RPCReq rpcReq) {
                        return leader(rpcReq);
                    }

                    @Override
                    public String interest() {
                        return RPCReq.class.getName();
                    }
                });
            } else {
                rpcServer.registerUserProcessor(new AbstractUserProcessor<RPCReq>() {
                    @Override
                    public void handleRequest(BizContext bizContext, AsyncContext asyncContext, RPCReq rpcReq) {
                    }

                    @Override
                    public RPCResp handleRequest(BizContext bizContext, RPCReq rpcReq) {
                        return follower(rpcReq);
                    }

                    @Override
                    public String interest() {
                        return RPCReq.class.getName();
                    }
                });
            }
            if (rpcServer.start()) {
                System.out.println("server start ok!");
            } else {
                System.out.println("server start failed!");
            }
        }
    }

    static RPCResp follower(RPCReq rpcReq) {
        KVReq req = (KVReq) rpcReq.getParam();

        int redisPort = Peer.getPort(rpcReq.getAddr()) - 100;
        String reqValue = null;
        JedisPool jedisPool = jedisPools[redisPort - FIRST];
        Jedis jedis = null;


        if (req.getType() == PUT) {
            try {
                ArrayList<String> key = req.getKey();
                ArrayList<String> putValue  = req.getValue();
                for (int i = 0; i < key.size(); i++) {
                    jedis = jedisPool.getResource();
                    jedis.set(key.get(i), putValue.get(i));
                    jedis.save();
                }

            } catch (JedisException e) {
                e.printStackTrace();
            } finally {
                jedis.close();
            }
        } else {
            String rdbPath = "redisConfigs/redis-" + redisPort + "/dump.rdb";
            File rdbFile = new File(rdbPath);
            if (rdbFile.exists()) {
                reqValue = RDBParser.getVal(rdbFile, req.getReqKey());
            }
        }
        KVAck ack = KVAck.builder().success(true).val(reqValue).build();
        return RPCResp.builder().req(rpcReq).result(ack).build();
    }

    static RPCResp leader(RPCReq rpcReq) {
        KVReq req = (KVReq) rpcReq.getParam();

        String reqValue = null;
        int port = Peer.getPort(rpcReq.getAddr());
        int redisPort = port - 100;

        if (req.getType() == PUT) {
            ArrayList<String> key = req.getKey();
            ArrayList<String> putValue  = req.getValue();
            JedisPool jedisPool = jedisPools[redisPort - FIRST];
            Jedis jedis = null;
            try {
                for (int i = 0; i < key.size(); i++) {
                    jedis = jedisPool.getResource();
                    jedis.set(key.get(i), putValue.get(i));
                    jedis.save();
                }

            } catch (JedisException e) {
                e.printStackTrace();
            } finally {
                jedis.close();
            }

            RPCClient client = new RPCClient();
            Peer self = new Peer("localhost:" + port, "localhost:" + redisPort);

            CompletableFuture[] cfs = PeerSet.getOthers(self).stream()
                    .map(peer -> CompletableFuture.supplyAsync(() -> {
                        try {
                            Thread.sleep(1000 * 10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        rpcReq.setAddr(peer.getAddr());
                        System.out.println(rpcReq);
                        RPCResp response = client.sendReq(rpcReq);
                        return response;
                    }).thenAccept(response -> {
                        System.out.println(((KVAck) response.getResult()).isSuccess());
                    }))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.anyOf(cfs);

        } else {
            String rdbPath = "redisConfigs/redis-" + redisPort + "/dump.rdb";
            File rdbFile = new File(rdbPath);
            String key = req.getReqKey();

            if (rdbFile.exists()) {
                reqValue = RDBParser.getVal(rdbFile, key);
            }
        }
        KVAck ack = KVAck.builder().success(true).val(reqValue).build();
        return RPCResp.builder().req(rpcReq).result(ack).build();
    }
}

