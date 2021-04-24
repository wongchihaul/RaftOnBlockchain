package raft;

import client.KVAck;
import client.KVReq;
import raft.config.NodeConfig;
import raft.entity.AppEntryParam;
import raft.entity.AppEntryResult;
import raft.entity.ReqVoteParam;
import raft.entity.ReqVoteResult;

/**
 * @author wongchihaul
 */

public interface Node {

    void setConfig(NodeConfig config);

    ReqVoteResult handleReqVote(ReqVoteParam param);

    AppEntryResult handlerAppEntry(AppEntryParam param);

    KVAck handleClientReq(KVReq req);

    /**
     * redirect to Leader
     *
     * @param req
     * @return
     */
    KVAck redirect(KVReq req);


}
