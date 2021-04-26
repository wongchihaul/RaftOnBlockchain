package raft;

import raft.entity.LogEntry;

/**
 * CRUD of persistent data in Redis
 */

public interface StateMachine {

    void apply(LogEntry logEntry);

    String getVal(String key);

    void setVal(String key, String value);

    void delVal(String... key);
}
