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
     * Options: REQ_VOTE, APP_ENTRY, KV
     */
    String Request;

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
