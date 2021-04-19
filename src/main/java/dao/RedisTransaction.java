package dao;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

public class RedisTransaction {
    public static boolean transaction(Jedis jedis, String key) {
        while (true) {
            jedis.watch(key);
            Transaction tx = jedis.multi();
            // some operations
            List<Object> res = tx.exec();
            if (res != null) {
                break;
            }
        }
        return true;
    }
}
