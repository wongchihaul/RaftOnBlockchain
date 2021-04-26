package raft.entity;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import raft.impl.NodeIMPL;

import java.io.Serializable;

@Getter
@Setter
@ToString
@Builder
public class AppEntryResult implements Serializable {
    /**
     * currentTerm, for leader to update itself
     */
    long term;

    /**
     * true if follower contained entry matching
     * prevLogIndex and prevLogTerm
     */
    boolean success;

    /**
     * Node returns false with its term
     *
     * @param node
     * @return
     */
    public static AppEntryResult fail(NodeIMPL node) {
        return AppEntryResult.builder().term(node.getCurrentTerm()).success(false).build();
    }

    /**
     * Node return true with its term
     *
     * @param node
     * @return
     */
    public static AppEntryResult success(NodeIMPL node) {
        return AppEntryResult.builder().term(node.getCurrentTerm()).success(true).build();
    }
}
