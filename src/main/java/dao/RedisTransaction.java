package dao;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.security.PublicKey;
import java.util.List;

public class RedisTransaction extends chainUtils.Transaction {

    public RedisTransaction(PublicKey from, String input) {
        super(from, input);
    }


    // Just a template
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

    // A demo
    public static int doubleAccount(Jedis jedis, String key) {
        while (true) {
            jedis.watch(key);
            int value = Integer.parseInt(jedis.get(key));
            value *= 2;
            Transaction tx = jedis.multi();
            tx.set(key, String.valueOf(value));
            List<Object> res = tx.exec();
            if (res != null) {
                break;
            }
        }
        return Integer.parseInt(jedis.get(key));
    }
}
