package raft.entity;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Params of RPC for requesting votes
 */

@Getter
@Setter
@Builder
@ToString
public class ReqVoteParam {

    /**
     * candidate’s term
     */
    long term;

    /**
     * candidate requesting vote(ip:port)
     */
    String candidateId;

    /**
     * index of candidate’s last log entry
     */
    long lastLogIndex;

    /**
     * term of candidate’s last log entry
     */
    long lastLogTerm;
}
