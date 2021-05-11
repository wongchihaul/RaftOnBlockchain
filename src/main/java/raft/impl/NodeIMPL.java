package raft.impl;

import client.KVAck;
import client.KVReq;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import raft.common.NodeStatus;
import raft.common.Peer;
import raft.common.PeerSet;
import raft.common.ReqType;
import raft.concurrent.RaftConcurrent;
import raft.entity.*;
import raft.rpc.RPCClient;
import raft.rpc.RPCReq;
import raft.rpc.RPCResp;
import raft.rpc.RPCServer;
import raft.tasks.LeaderElection;
import raft.tasks.Replication;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static client.KVReq.GET;
import static raft.common.RedisPool.setConfig;
import static raft.concurrent.RaftConcurrent.RaftThreadPool;


@Getter
@Setter
@ToString
public class NodeIMPL {

    public static final int HEARTBEAT_TICK = 125;
    public static final int ELECTION_TIMEOUT = 300;
    public static final int REPLICATION_TIMEOUT = 4000;
    public static final Logger logger = LogManager.getLogger(NodeIMPL.class.getName());

    /* =============== Node configure ==================== */
    private final String addr;
    private final String redisAddr;
    private final Peer peer;
    private JedisPool jedisPool;

    /* ============ Volatile variable of the node ============= */
    private volatile NodeStatus status = NodeStatus.FOLLOWER;
    /** CandidateId that received vote in current term (or null if none) */
    private volatile String votedFor = null;
    private volatile Peer leader = null;

    /** initialized to 0 on first boot, increases monotonically */
    private volatile long currentTerm = 0;
    /** The latest commit index */
    private volatile long commitIndex = 0;
    /** The last Applied index of logEntry (initial as 0, continuously increase) */
    private volatile long lastApplied = 0;
    private volatile boolean started;


    /** The next index of logEntry for each node. */
    private Map<Peer, Long> nextIndexes;
    /** The last applied index of logEntry for each node. */
    private Map<Peer, Long> latestIndexes;

    /** Set of peers, excluding self */
    private volatile Set<Peer> peerSet;


    /* ================= RPC related ===================== */
    private RPCClient rpcClient;
    private RPCServer rpcServer;


    /* =============== Raft Algorithm related ============== */
    /**
     * log entries; each entry contains command for state machine, and term when entry
     * was received by leader (first index is 1)
     * And we will have CRUD of log entries in Redis
     */
    private LogModuleIMPL logModule;
    private StateMachineIMPL stateMachine;
    private ConsensusIMPL consensus;


    /* =============== time variable ================ */
    public volatile long prevElectionTime = 0;
    public volatile int electionTimeOut = 250;

    // Use to cancel heartbeat task
    private ScheduledFuture<?> scheduledHeartBeatTask;


    public NodeIMPL(String addr, String redisAddr) {
        this.addr = addr;
        this.redisAddr = redisAddr;
        this.peer = new Peer(addr, redisAddr);
        this.jedisPool = new JedisPool(setConfig(), Peer.getIP(redisAddr), Peer.getPort(redisAddr));
        this.logModule = new LogModuleIMPL(this);
        this.peerSet = PeerSet.getOthers(this.peer);
        this.stateMachine = new StateMachineIMPL(this);
        this.consensus = new ConsensusIMPL(this);
        this.rpcClient = new RPCClient();
        this.rpcServer = new RPCServer(Peer.getPort(addr), this);
        this.nextIndexes = new HashMap<>();
        this.latestIndexes = new HashMap<>();
        this.started = false;
    }

    public void init() {
        if (started) {
            return;
        }
        synchronized (this) {
            if (started) {
                return;
            }
            rpcServer.start();

            consensus = new ConsensusIMPL(this);


            LeaderElection leaderElection = new LeaderElection(this);
            RaftConcurrent.scheduler.scheduleAtFixedRate(leaderElection, 3000, 500, TimeUnit.MILLISECONDS);
            LogEntry logEntry = logModule.getLast();
            if (logEntry != null) {
                currentTerm = logEntry.getTerm();
            }

            started = true;

            logger.info("start success, selfId : " + this.getAddr());
        }
    }

    public void destroy() {
        rpcServer.stop();
        scheduledHeartBeatTask.cancel(true);
        started = false;
    }


    public ReqVoteResult handleReqVote(ReqVoteParam param) {
//        LOGGER.debug(String.format("Node{%s} handle request vote param info: %s", this.getAddr(), param));
        return consensus.requestVote(param);
    }

    public AppEntryResult handleAppEntry(AppEntryParam param) {
//        LOGGER.debug(String.format("Append Entry param info: %s", param));
        return consensus.appendEntry(param);
    }

    public KVAck handleClientReq(KVReq req) {
        logger.debug(String.format("Node{%s} receive request: %s", addr, req));

        if (status != NodeStatus.LEADER) {
            logger.info(String.format("Node{%s} is not am leader, redirect to node{%s}", addr, leader.getAddr()));
            return redirect(req);
        }

        if (req.getType() == GET) {
            String key = req.getReqKey();
            String res = this.stateMachine.getVal(key);
            System.out.println(KVAck.builder().success(true).val(res).build());
            return KVAck.builder().success(true).val(res).build();
        }

//        LogEntry logEntry = LogEntry.builder()
//                .transaction(Transaction.builder().
//                        key(req.getKey()).
//                        value(req.getValue()).
//                        noobChain(req.getNoobChain()).
//                        build())
//                //.index(this.commitIndex + 1)
//                .term(currentTerm)
//                .build();
        Transaction trans = Transaction.builder().
                                        key(req.getKey()).
                        value(req.getValue()).
                        noobChain(req.getNoobChain()).
                        build();

        LogEntry logEntry = new LogEntry(currentTerm, trans);



        logModule.write(logEntry);

        var replicaResult = RaftThreadPool.submit(new Replication(this, logEntry));

        while (!replicaResult.isDone()) {
            // wait for replication to finish
        }

        // in case interrupted before applying to state machine
        if (this.commitIndex == logEntry.getIndex() && this.lastApplied == logEntry.getIndex()) {
            return KVAck.builder().success(true).val(null).build();
        } else {
            return KVAck.builder().success(false).val(null).build();
        }
    }

    public KVAck redirect(KVReq req) {
        RPCReq redirectReq = RPCReq.builder()
                .addr(this.getLeader().getAddr())
                .param(req)
                .requestType(ReqType.KV)
                .build();
        RPCResp resp = rpcClient.sendReq(redirectReq);
        return (KVAck) resp.getResult();
    }
}
