package raft.rpc;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
@Builder
public class RPCReq<T> implements Serializable {

    /**
     * Options: REQ_VOTE(0), APP_ENTRY(1), KV(2)
     */
    int Request;

    /**
     * IP address of RPCServer
     */
    String addr;

    /**
     * param
     *
     * @see raft.entity.AppEntryParam
     * @see raft.entity.ReqVoteParam
     * @see client.KVReq
     */
    T param;


}
