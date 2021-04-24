package raft.impl;

import raft.Consensus;
import raft.entity.AppEntryParam;
import raft.entity.AppEntryResult;
import raft.entity.ReqVoteParam;
import raft.entity.ReqVoteResult;

public class ConsensusIMPL implements Consensus {
    @Override
    public ReqVoteResult requestVote(ReqVoteParam param) {
        return null;
    }

    @Override
    public AppEntryResult appendEntry(AppEntryParam param) {
        return null;
    }
}
