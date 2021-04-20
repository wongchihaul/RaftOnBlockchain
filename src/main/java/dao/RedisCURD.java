package dao;

import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Set;


public class RedisCURD {

    public Jedis jedis;

    public RedisCURD(Jedis jedis) {
        this.jedis = jedis;
    }

    public Integer get(String key) {
        if (jedis.get(key) != null) {
            return Integer.valueOf(jedis.get(key));
        } else {
            System.out.println("(nil)");
            return Integer.MIN_VALUE;
        }
    }

    public HashMap<String, Integer> getAll() {
        HashMap<String, Integer> all = new HashMap<>();
        Set<String> keys = jedis.keys("*");
        for (String key : keys) {
            Integer val = Integer.valueOf(jedis.get(key));
            all.put(key, val);
        }
        return all;
    }

    public void save(String key, Integer val) {
        jedis.setnx(key, String.valueOf(val));
    }


    public void delete(String key) {
        jedis.del(key);
    }


}
