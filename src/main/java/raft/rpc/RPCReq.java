package raft.rpc;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import raft.common.ReqType;

import java.io.Serializable;

@Getter
@Setter
@ToString
//@Builder
public class RPCReq<T> implements Serializable {

    /**
     * Options: REQ_VOTE(0), APP_ENTRY(1), KV(2)
     */
    ReqType requestType;

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

    public RPCReq(ReqType requestType, String addr, T obj) {
        this.requestType = requestType;
        this.addr = addr;
        this.param = obj;
    }


}
