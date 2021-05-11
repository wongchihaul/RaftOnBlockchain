package raft.impl;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import raft.common.NodeStatus;
import raft.common.Peer;
import raft.entity.*;

import java.util.concurrent.locks.ReentrantLock;


@Setter
@Getter
public class ConsensusIMPL {

    public static final Logger logger = LogManager.getLogger(ConsensusIMPL.class.getName());

    public NodeIMPL node;

    public ReentrantLock reqVoteLock = new ReentrantLock(), appEntryLock = new ReentrantLock();

    public ConsensusIMPL(NodeIMPL node) {
        this.node = node;
    }

    /**
     * RPC requesting for vote
     * Receiver :
     * 1. Reply false if term < currentTerm (§5.1)
     * 2. If votedFor is null or candidateId, and candidate’s log is at
     * least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
     *
     * @param param The parameter of Vote request
     * @return Vote result
     */
    public ReqVoteResult requestVote(ReqVoteParam param) {
        try {
            if (!reqVoteLock.tryLock()) {
                return ReqVoteResult.fail(node);
            }

            // Reply false if term < currentTerm
            if (param.getTerm() < node.getCurrentTerm()) {
                return ReqVoteResult.fail(node);
            }

            // If votedFor is null or candidateId, and candidate’s log is at
            // least as up-to-date as receiver’s log, grant vote
            if (node.getVotedFor() == null || node.getVotedFor().equals(param.getCandidateId())) {

                if (node.getLogModule().getLast() != null) {
                    if (param.getTerm() < node.getLogModule().getLast().getTerm()) {
                        return ReqVoteResult.fail(node);
                    }
                    if (param.getLastLogIndex() < node.getLogModule().getLastIndex()) {
                        return ReqVoteResult.fail(node);
                    }
                }
                // update status
                String id = param.getCandidateId();
                node.setStatus(NodeStatus.FOLLOWER);
                node.setCurrentTerm(param.getTerm());
                node.setVotedFor(id);
                node.setLeader(new Peer(id, Peer.getIP(id) + (Peer.getPort(id) - 100)));

                return ReqVoteResult.success(node);
            } else {
                return ReqVoteResult.fail(node);
            }
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
    public AppEntryResult appendEntry(AppEntryParam param) {
        try {
            if (!appEntryLock.tryLock()) {
                logger.error("Get Lock fail");
                return AppEntryResult.fail(node);
            }
            if (param.getLogEntries().size() != 0) {
                logger.info("trying append entry.... " + param.getLogEntries().toString());
            }


            // Requirement 1
            if (param.getTerm() < node.getCurrentTerm()) {
                return AppEntryResult.fail(node);

//            } else {
//                node.status = NodeStatus.FOLLOWER;
            }
            String id = param.getLeaderId();
            node.setCurrentTerm(param.getTerm());
            node.setLeader(new Peer(id, Peer.getIP(id) + (Peer.getPort(id) - 100)));

            node.prevElectionTime = System.currentTimeMillis();

            // heartbeat
            if (param.getLogEntries().size() == 0) {
//                logger.info(String.format(
//                        "node{%s} successfully appends heartbeat from leader{%s}, its term: %s, self term: %s",
//                        node.getAddr(), param.getLeaderId(), param.getTerm(), node.getCurrentTerm()));
                return AppEntryResult.success(node);
            }

            // entries to be applied in state machine
            // Requirement 2

            LogEntry lastEntry = node.getLogModule().getLast();

            if (lastEntry != null) {
                if (lastEntry.getTerm() == param.getPrevLogTerm()
                        && lastEntry.getIndex() != param.getPrevLogIndex()) {
//                System.out.println("term same index diff");
                    return AppEntryResult.fail(node);
                }
            }

            //Requirement 3 & 4
            long currIndex = param.getPrevLogIndex() + 100;
            LogEntry existingEntry = node.getLogModule().read(currIndex);
//            System.out.println("existing entry");
            if (existingEntry != null) {
                System.out.println("exsitingEntry: " + node.getLogModule().read(currIndex));
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
//                System.out.println("goodd");
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
