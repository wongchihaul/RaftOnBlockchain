package raft;

import raft.entity.LogEntry;

public interface LogModule {
    void write(LogEntry logEntry);

    LogEntry read(long index);

    void removeLogs(Long startIndex);

    LogEntry getLast();

    Long getLastIndex();
}
