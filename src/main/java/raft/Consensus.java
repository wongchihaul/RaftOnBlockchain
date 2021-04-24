package raft;

import raft.entity.AppEntryParam;
import raft.entity.AppEntryResult;
import raft.entity.ReqVoteParam;
import raft.entity.ReqVoteResult;

/**
 * Raft Consensus Module
 *
 * @author wongchihaul
 */

public interface Consensus {
    /**
     * Request for vote RPC
     * <p>
     * Receiver :
     * 1. Reply false if term < currentTerm (§5.1)
     * 2. If votedFor is null or candidateId, and candidate’s log is at
     * least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
     */
    ReqVoteResult requestVote(ReqVoteParam param);

    /**
     * Append entries RPC
     * <p>
     * Receiver:
     * 1. Reply false if term < currentTerm (§5.1)
     * 2. Reply false if log doesn’t contain an entry at prevLogIndex
     * whose term matches prevLogTerm (§5.3)
     * 3. If an existing entry conflicts with a new one (same index
     * but different terms), delete the existing entry and all that
     * follow it (§5.3)
     * 4. Append any new entries not already in the log
     * 5. If leaderCommit > commitIndex, set commitIndex =
     * min(leaderCommit, index of last new entry)
     */

    AppEntryResult appendEntry(AppEntryParam param);
}
