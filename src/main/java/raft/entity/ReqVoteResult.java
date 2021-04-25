package raft.entity;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Return of RPC for requesting votes
 */
@Getter
@Setter
@Builder
public class ReqVoteResult implements Serializable {
    /**
     * currentTerm, for candidate to update itself
     */
    long term;

    /**
     * true means candidate received vote
     */
    boolean voteGranted;

}
