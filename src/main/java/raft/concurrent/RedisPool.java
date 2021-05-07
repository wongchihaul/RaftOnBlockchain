package raft.concurrent;

import raft.common.Peer;
import raft.common.PeerSet;
import raft.impl.NodeIMPL;
import raft.impl.StateMachineIMPL;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;

public class RedisPool {


//    static{
//
//        JedisPoolConfig config =new JedisPoolConfig();//Jedis池配置
//
//        config.setMaxTotal(10);//最大活动的对象个数
//
//        config.setMaxIdle(1000 * 60);//对象最大空闲时间
//
//        config.setMaxWaitMillis(1000 * 10);//获取对象时最大等待时间
//
//        config.setTestOnBorrow(true);
//
////        String hostA = "10.10.224.44";
//
//        int portA = 6379;
//
////        String hostB = "10.10.224.48";
//
//        int portB = 6380;
//
//
//        int portC = 6381;
//
//        List<JedisShardInfo> jdsInfoList =new ArrayList<>(3);
//
//        JedisShardInfo infoA = new JedisShardInfo("localhost", portA);
//
////        infoA.setPassword("redis.360buy");
//
//        JedisShardInfo infoB = new JedisShardInfo("localhost", portB);
//
////        infoB.setPassword("redis.360buy");
//
//        JedisShardInfo infoC = new JedisShardInfo("localhost", portC);
//
////        jdsInfoList.add(infoA);
//
//        jdsInfoList.add(infoB);
//
//        jdsInfoList.add(infoC);
//
//        jdsInfoList.add(new JedisShardInfo("localhost", 6382));
//        jdsInfoList.add(new JedisShardInfo("localhost", 6383));
//        jdsInfoList.add(new JedisShardInfo("localhost", 6384));
//
//
//        pool = new ShardedJedisPool(config, jdsInfoList, Hashing.MURMUR_HASH,
//                Sharded.DEFAULT_KEY_TAG_PATTERN);
//        //传入连接池配置、分布式redis服务器主机信息、分片规则（存储到哪台redis服务器）
//    }

    private static int index = 1;

    public static JedisPoolConfig setConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();//Jedis池配置

        poolConfig.setMaxTotal(10);//最大活动的对象个数\
        poolConfig.setMaxIdle(1000 * 60);//对象最大空闲时间
        poolConfig.setMaxWaitMillis(1000 * 10);//获取对象时最大等待时间
        poolConfig.setTestOnBorrow(true);

        return poolConfig;
    }

    /**
     * @param args
     */

    public static void main(String[] args) {

//        for(int i=0; i<10000; i++){
//            String key =generateKey();
//            //key += "{aaa}";
//            ShardedJedis jds =null;
//            try {
//                jds =pool.getResource();
//                System.out.println(key+":"+jds.getShard(key).getClient().getHost());
//                System.out.println(jds.set(key,"1111111111111111111111111111111"));
//            }catch (Exception e) {
//                e.printStackTrace();
//            }
//            finally{
//                jds.close();
//            }
//
//        }

        ArrayList<NodeIMPL> nodeList = new ArrayList<>();
        NodeIMPL nodeIMPL = null;
        for (int i = 6380; i <= 6384; i++) {
            String addr = "localhost:" + i;
            Peer peer = new Peer(addr);
            PeerSet.peerSet.add(peer);
        }
        for (int i = 6380; i <= 6384; i++) {
            String addr = "localhost:" + i;
            nodeIMPL = new NodeIMPL(addr);
            nodeList.add(nodeIMPL);
        }

        nodeList.forEach(NodeIMPL::init);

        try {
            Thread.sleep(1000 * 4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        nodeList.forEach(node -> System.out.println(node.getLeader()));

        try {
            Thread.sleep(1000 * 4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        nodeIMPL = nodeList.get(2);
        StateMachineIMPL stateMachineIMPL = new StateMachineIMPL(nodeIMPL);
        System.out.println(stateMachineIMPL.getVal("1_034"));

    }

    public static String generateKey() {

        return Thread.currentThread().getId() + "_" + (index++);

    }
}
