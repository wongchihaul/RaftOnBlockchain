package raft;

import raft.entity.LogEntry;

/**
 * CRUD of persistent data in database
 */

public interface StateMachine {

    void apply(LogEntry logEntry);

    LogEntry get(String key);

    String getVal(String key);

    void setVal(String key, String value);

    void delVal(String... key);
}
