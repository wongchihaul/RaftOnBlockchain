package raft.impl;

import raft.StateMachine;
import raft.entity.LogEntry;

public class DAOIMPL implements StateMachine {
    @Override
    public void apply(LogEntry logEntry) {

    }

    @Override
    public LogEntry get(String key) {
        return null;
    }

    @Override
    public String getVal(String key) {
        return null;
    }

    @Override
    public void setVal(String key, String value) {

    }

    @Override
    public void delVal(String... key) {

    }
}
