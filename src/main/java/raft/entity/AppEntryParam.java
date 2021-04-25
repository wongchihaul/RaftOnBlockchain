package raft.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class AppEntryParam {
    /**
     * currentTerm, for leader to update itself
     */
    long term;

    /**
     * so follower can redirect clients
     */
    String leaderId;

    /**
     * index of log entry immediately preceding
     * new ones
     */
    long prevLogIndex;

    /**
     * term of prevLogIndex entry
     */
    long preLogTerm;

    /**
     * log entries to store (empty for heartbeat;
     * may send more than one for efficiency)
     */
    LogEntry[] logEntries;

    /**
     * leader’s commitIndex
     */
    long leaderCommit;

}
