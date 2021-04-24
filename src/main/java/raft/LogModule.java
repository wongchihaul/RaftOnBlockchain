package raft;

import raft.entity.LogEntry;

public interface LogModule {
    void write(LogEntry logEntry);

    LogEntry read(Long index);

    void removeLogs(Long startIndex);

    LogEntry getLast();

    Long getLastIndex();
}
