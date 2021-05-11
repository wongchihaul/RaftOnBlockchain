package raft.common;

import redis.clients.jedis.JedisPoolConfig;

public class RedisPool {



    private static int index = 1;

    public static JedisPoolConfig setConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();

        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(1000 * 60);
        poolConfig.setMaxWaitMillis(1000 * 10);
        poolConfig.setTestOnBorrow(true);

        return poolConfig;
    }

    public static String generateKey() {

        return Thread.currentThread().getId() + "_" + (index++);

    }
}
