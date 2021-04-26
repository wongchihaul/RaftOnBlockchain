package raft.impl;

import com.alibaba.fastjson.JSON;
import raft.LogModule;
import raft.entity.LogEntry;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.UUID;

public class LogModuleIMPL implements LogModule {
    NodeIMPL node;
    String uuid;
    Jedis jedis;

    public LogModuleIMPL(NodeIMPL node) {
        this.node = node;
        this.jedis = node.getJedis();
        this.uuid = UUID.randomUUID().toString().replace("-", "");
    }


    @Override
    public void write(LogEntry logEntry) {
        try {
            String entry = JSON.toJSONString(logEntry);
            jedis.lpush(uuid, entry);
        } catch (JedisException e) {
            e.printStackTrace();
        }
    }

    @Override
    public LogEntry read(Long index) {
        try {
            String entry = jedis.lindex(uuid, index);
            if (entry == null) {
                return null;
            }
            return JSON.parseObject(entry, LogEntry.class);
        } catch (JedisException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Remove log entries at and after startIndex
     *
     * @param startIndex
     */
    @Override
    public void removeLogs(Long startIndex) {
        try {
            jedis.ltrim(uuid, 0, startIndex - 1);
        } catch (JedisException e) {
            e.printStackTrace();
        }
    }

    @Override
    public LogEntry getLast() {
        return read(getLastIndex());
    }

    // start from 1
    @Override
    public Long getLastIndex() {
        return jedis.llen(uuid);
    }
}
