package raft.impl;

import com.alibaba.fastjson.JSON;
import raft.StateMachine;
import raft.common.Peer;
import raft.common.RDBParser;
import raft.entity.LogEntry;
import raft.entity.Transaction;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.io.File;
import java.util.logging.Logger;

public class StateMachineIMPL implements StateMachine {
    //TODO: whether we should use disk-based database instead of Redis, since everytime we read
    // data from disk we should shutdown Redis, load RDB file and restart the Redis.

    public static final java.util.logging.Logger logger = Logger.getLogger(StateMachineIMPL.class.getName());

    NodeIMPL node;
    JedisPool jedisPool;

    /**
     * "redisConfigs/redis-${port}/dump.rdb"
     */
    String rdbPath;

    //    String confPath;


    public StateMachineIMPL(NodeIMPL node) {
        this.node = node;
        jedisPool = node.getJedisPool();
        rdbPath = "redisConfigs/redis-" + Peer.getPort(node.getRedisAddr()) + "/dump.rdb";
    }

    // TODO: I don't think synchronized is needed since Redis is single-threaded.
    @Override
    public void apply(LogEntry logEntry) {
        Transaction transaction = logEntry.getTransaction();
        if (transaction == null) {
            throw new IllegalArgumentException(logEntry + ": Command cannot be null");
        }
        String key = transaction.getKey();
        String value = transaction.getValue();
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.set(key, value);
            jedis.set(node.getAddr(), JSON.toJSONString(logEntry));
            // also save logs, because state machine module and log entry module share same jedis instance
            // should be optimized, e.g. use disk-based database, if data becomes huge.
            System.out.println("Saving now");
            jedis.save();
        } catch (JedisException e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }

    }


    //TODO: Plan (A) use third-party parsing library to parse RDB file and get value
    //      Plan (B) find out how to use a temporary Redis instance to load RDB file and get value
    @Override
    public String getVal(String key) {
        File rdbFile = new File(rdbPath);
        if (rdbFile.exists()) {
            return RDBParser.getVal(rdbFile, key);
        }
        return null;
    }

    @Override
    public void setVal(String key, String value) {

    }

    @Override
    public void delVal(String... key) {

    }
}
