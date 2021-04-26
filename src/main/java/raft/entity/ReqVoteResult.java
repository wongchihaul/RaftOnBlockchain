package raft.entity;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import raft.impl.NodeIMPL;

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

    /**
     * Node returns false with its term
     *
     * @param node
     * @return
     */
    public static ReqVoteResult fail(NodeIMPL node) {
        return ReqVoteResult.builder().term(node.getCurrentTerm()).voteGranted(false).build();
    }

    /**
     * Node return true with its term
     *
     * @param node
     * @return
     */
    public static ReqVoteResult success(NodeIMPL node) {
        return ReqVoteResult.builder().term(node.getCurrentTerm()).voteGranted(true).build();
    }
}
