package raft.common;

/**
 * code for status
 */
public class Code {
    // code for node status
    public static class NodeStatus {
        public final static int FOLLOWER = 0;
        public final static int CANDIDATE = 1;
        public final static int LEADER = 2;
    }

    // code for rpc request
    public static class ReqType {
        public final static int REQ_VOTE = 0;
        public final static int APP_ENTRY = 1;
        public final static int KV = 2;
    }

}
