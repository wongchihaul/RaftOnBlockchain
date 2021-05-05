package raft.rpc;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * return (RPCResp)RPCReq.Request + Result
 * e.g. REQ_VOTE SUCCESS
 *
 * @param <T>
 */

@Getter
@Setter
@ToString
@Builder
public class RPCResp<T> implements Serializable, Supplier<T> {
    T result;

    RPCReq req;


    @Override
    public T get() {
        return result;
    }
}
