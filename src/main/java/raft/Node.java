package raft;

import client.KVAck;
import client.KVReq;
import raft.entity.AppEntryParam;
import raft.entity.AppEntryResult;
import raft.entity.ReqVoteParam;
import raft.entity.ReqVoteResult;

/**
 * @author wongchihaul
 */

public interface Node extends LifeCycle {

    ReqVoteResult handleReqVote(ReqVoteParam param);

    AppEntryResult handleAppEntry(AppEntryParam param);

    KVAck handleClientReq(KVReq req);

    /**
     * redirect to Leader
     * @param req
     * @return
     */
    KVAck redirect(KVReq req);

}
