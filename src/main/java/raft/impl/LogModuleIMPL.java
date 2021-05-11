package raft.impl;

import com.alibaba.fastjson.JSON;
import org.json.simple.parser.ParseException;
import raft.entity.LogEntry;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.util.UUID;

import static client.BlockChainAutoClient.StringToObject;

public class LogModuleIMPL {
    NodeIMPL node;
    String uuid;
    JedisPool jedisPool;

    public LogModuleIMPL(NodeIMPL node) {
        this.node = node;
        this.jedisPool = node.getJedisPool();
        this.uuid = UUID.randomUUID().toString().replace("-", "");
    }


    public void write(LogEntry logEntry) {
        Jedis jedis = null;
        try {
            String entry = JSON.toJSONString(logEntry);
            jedis = jedisPool.getResource();
            jedis.lpush(uuid, entry);
        } catch (JedisException e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
    }

    public LogEntry read(long index) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String entry = jedis.lindex(uuid, index);
            if (entry == null) {
                return null;
            }
            return StringToObject(jedis.lindex(uuid, index));


            //return JSON.parseObject(entry, LogEntry.class);
        } catch (JedisException | ParseException e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
        return null;
    }

    /**
     * Remove log entries at and after startIndex
     *
     * @param startIndex
     */
    public void removeLogs(Long startIndex) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.ltrim(uuid, 0, startIndex - 1);
        } catch (JedisException e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
    }

    public LogEntry getLast() {
        return read(getLastIndex());
    }

    // start from 1
    public Long getLastIndex() {
        Jedis jedis = null;
        Long lastIndex = null;
        try {
            jedis = jedisPool.getResource();
//            System.out.println("$$$the jedis uuid length is" + jedis.llen(uuid));
            //lastIndex = jedis.llen(uuid) == 0 ? 1 : jedis.llen(uuid);
            lastIndex = jedis.llen(uuid)-1;
        } catch (JedisException e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
        return lastIndex;
    }
}
