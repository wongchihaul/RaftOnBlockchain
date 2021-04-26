package raft.impl;

import lombok.Getter;
import lombok.Setter;
import raft.Consensus;
import raft.common.Code;
import raft.common.Peer;
import raft.entity.*;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;


@Setter
@Getter
public class ConsensusIMPL implements Consensus {

    public static final Logger logger = Logger.getLogger(ConsensusIMPL.class.getName());

    public NodeIMPL node;

    public ReentrantLock reqVoteLock = new ReentrantLock(), appEntryLock = new ReentrantLock();

    /**
     * RPC requesting for vote
     * Receiver :
     * 1. Reply false if term < currentTerm (§5.1)
     * 2. If votedFor is null or candidateId, and candidate’s log is at
     * least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
     *
     * @param param
     * @return
     */
    @Override
    public ReqVoteResult requestVote(ReqVoteParam param) {
        try {
            if (!reqVoteLock.tryLock()) {
                return ReqVoteResult.fail(node);
            }

            // Reply false if term < currentTerm
            if (param.getTerm() < node.getCurrentTerm()) {
                return ReqVoteResult.success(node);
            }

            // If votedFor is null or candidateId, and candidate’s log is at
            // least as up-to-date as receiver’s log, grant vote
            if (node.getVotedFor() == null || node.getVotedFor().equals(param.getCandidateId())) {

                if (node.getLogModule().getLast() != null) {
                    if (param.getTerm() < node.getLogModule().getLast().getTerm()
                            || param.getLastLogIndex() < node.getLogModule().getLastIndex()) {
                        return ReqVoteResult.fail(node);
                    }
                }
            }

            // update status
            node.status = Code.NodeStatus.FOLLOWER;
            node.setCurrentTerm(param.getTerm());
            node.setVotedFor(param.getCandidateId());
            node.setLeader(new Peer(param.getCandidateId()));

            return ReqVoteResult.success(node);
        } finally {
            reqVoteLock.unlock();
        }
    }

    /**
     * Append entries RPC
     * Receiver:
     * 1. Reply false if term < currentTerm (§5.1)
     * 2. Reply false if log doesn't contain an entry at prevLogIndex
     * whose term matches prevLogTerm (§5.3)
     * 3. If an existing entry conflicts with a new one (same index
     * but different terms), delete the existing entry and all that
     * follow it (§5.3)
     * 4. Append any new entries not already in the log
     * 5. If leaderCommit > commitIndex, set commitIndex =
     * min(leaderCommit, index of last new entry)
     *
     * @param param
     * @return
     */
    @Override
    public AppEntryResult appendEntry(AppEntryParam param) {
        try {
            if (!appEntryLock.tryLock()) {
                return AppEntryResult.fail(node);
            }

            if (param.getTerm() < node.getCurrentTerm()) {
                return AppEntryResult.fail(node);
            } else {
                node.status = Code.NodeStatus.FOLLOWER;
            }

            node.setCurrentTerm(param.getTerm());
            node.setLeader(new Peer(param.getLeaderId()));

            // heartbeat
            if (param.getLogEntries().size() == 0) {
                logger.info(String.format("node %s successfully appends heartbeat, its term: %s, self term: %s",
                        param.getLeaderId(), param.getTerm(), node.getCurrentTerm()));
                return AppEntryResult.success(node);
            }

            // entries to be applied in state machine
            // Requirement 2
            LogEntry lastEntry = node.getLogModule().getLast();
            if (lastEntry.getTerm() == param.getPrevLogTerm()
                    && lastEntry.getIndex() != param.getPrevLogIndex()) {
                return AppEntryResult.fail(node);
            }

            //Requirement 3 & 4
            long currIndex = param.getPrevLogIndex() + 1;
            LogEntry existingEntry = node.getLogModule().read(currIndex);
            if (existingEntry != null) {
                //conflict
                if (existingEntry.getTerm() != param.getLogEntries().get(0).getTerm()) {
                    node.getLogModule().removeLogs(currIndex);
                } else {
                    //redundant
                    return AppEntryResult.success(node);
                }
            } else {
                // append to log[] and apply to state machine
                for (LogEntry entry : param.getLogEntries()) {
                    node.getLogModule().write(entry);
                    node.getStateMachine().apply(entry);
                }
            }

            //Requirement 5
            if (node.getCommitIndex() < param.getLeaderCommit()) {
                long commitIndex = Math.min(node.getLogModule().getLastIndex(), param.getLeaderCommit());
                node.setCommitIndex(commitIndex);
                node.setLastApplied(commitIndex);
            }

            return AppEntryResult.success(node);


        } finally {
            appEntryLock.unlock();
        }

    }
}
