package raft.impl;

import raft.LogModule;
import raft.entity.LogEntry;

public class LogModuleIMPL implements LogModule {
    @Override
    public void write(LogEntry logEntry) {

    }

    @Override
    public LogEntry read(Long index) {
        return null;
    }

    @Override
    public void removeLogs(Long startIndex) {

    }

    @Override
    public LogEntry getLast() {
        return null;
    }

    @Override
    public Long getLastIndex() {
        return null;
    }
}
