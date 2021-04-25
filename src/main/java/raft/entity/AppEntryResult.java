package raft.entity;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
}
