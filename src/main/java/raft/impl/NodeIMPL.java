package raft.impl;

import chainUtils.NoobChain;
import client.KVAck;
import client.KVReq;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import raft.Consensus;
import raft.LogModule;
import raft.Node;
import raft.common.NodeStatus;
import raft.common.Peer;
import raft.common.ReqType;
import raft.entity.*;
import raft.rpc.RPCClient;
import raft.rpc.RPCReq;
import raft.rpc.RPCResp;
import raft.rpc.RPCServer;
import raft.tasks.HeartBeatTask;
import raft.tasks.LeaderElection;
import raft.tasks.Replication;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static raft.concurrent.RaftConcurrent.RaftThreadPool;


@Getter
@Setter
@ToString
//@Builder
public class NodeIMPL implements Node {


    public static final int HEARTBEAT_TICK = 125;

    public static final int ELECTION_TIMEOUT = 150;

    public static final int REPLICATION_TIMEOUT = 4000;

    public static final Logger LOGGER = Logger.getLogger(NodeIMPL.class.getName());

    public volatile boolean started;

    // START of Raft properties configuration
    /**
     * initialized to 0 on first boot, increases monotonically
     */
    long currentTerm = 0;

    /**
     * candidateId that received vote in current term (or null if none)
     */
    String votedFor = null;

    /**
     * log entries; each entry contains command
     * for state machine, and term when entry
     * was received by leader (first index is 1)
     * And we will have CRUD of log entries in Redis
     */
    LogModule logModule;

    /**
     * Options: FOLLOWER(0), CANDIDATE(1), LEADER(2)
     * Initiate as a FOLLOWER;
     */
    volatile NodeStatus status = NodeStatus.FOLLOWER;

    volatile Peer leader = null;

    /* ============ 所有服务器上经常变的 ============= */

    /**
     * 已知的最大的已经被提交的日志条目的索引值
     */
    volatile long commitIndex;

    /**
     * 最后被应用到状态机的日志条目索引值（初始化为 0，持续递增)
     */
    volatile long lastApplied = 0;



    /* ========== 在领导人里经常改变的(选举后重新初始化) ================== */

    /**
     * 对于每一个服务器，需要发送给他的下一个日志条目的索引值（初始化为领导人最后索引值加一）
     */
    Map<Peer, Long> nextIndexes;

    /**
     * 对于每一个服务器，已经复制给他的日志的最高索引值
     */
    Map<Peer, Long> latestIndexes;

    /**
     * Set of peers, excluding self
     */
    private volatile Set<Peer> peerSet;


    public volatile long prevElectionTime = 0;
    //the election timeouts are chosen randomly from a ﬁxed interval(150-300ms) as suggested
    public volatile int electionTimeOut = 150;


    public volatile long preHeartBeat = 0;
    // END of Raft properties configuration


    // START of Network and Redis configuration
    private String addr;
    private Peer peer;

    Jedis jedis;

    private StateMachineIMPL stateMachine;
    // END of Network and Redis configuration

    private Consensus consensus;

    private long lastHeartBeatTime = 0;

    private RPCClient rpcClient = new RPCClient();
    private RPCServer rpcServer = new RPCServer(9000, this); //这里先随便写了个

    private HeartBeatTask heartBeatTask = new HeartBeatTask(this);

    //这里用来取消scheduled tasks
    private ScheduledFuture<?> scheduledHeartBeatTask;


    public NodeIMPL(String addr) {
        this.addr = addr;
        this.peer = new Peer(addr);
        this.jedis = new Jedis(Peer.getIP(addr), Peer.getPort(addr));
        this.logModule = new LogModuleIMPL(this);
        this.peerSet = new HashSet<>();
        this.stateMachine = new StateMachineIMPL(this);
        this.consensus = new ConsensusIMPL(this);
    }

    @Override
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

            RaftThreadPool.submit(leaderElection);

            LogEntry logEntry = logModule.getLast();
            if (logEntry != null) {
                currentTerm = logEntry.getTerm();
            }

            started = true;

            LOGGER.info("start success, selfId : " + this.getAddr());
        }
    }

    @Override
    public void destroy() {
        rpcServer.stop();
    }


    @Override
    public ReqVoteResult handleReqVote(ReqVoteParam param) {
        LOGGER.warning(String.format("request vote param info: %s", param));
        return consensus.requestVote(param);
    }

    @Override
    public AppEntryResult handleAppEntry(AppEntryParam param) {
        LOGGER.warning(String.format("Append Entry param info: %s", param));
        return consensus.appendEntry(param);
    }
    public boolean verifyChain(NoobChain noobChain){
        return true;
    }

    public KVAck handleClientReq(KVReq req) {
        LOGGER.warning(String.format("handlerClientRequest handler %s operation", KVReq.Type.value(req.getType())));


        if (status != NodeStatus.LEADER) {
            LOGGER.warning("I not am leader , only invoke redirect method");
            return redirect(req);
        }


        if (verifyChain(req.getNoobChain())){
            LogEntry logEntry = LogEntry.builder()
                    .transaction(Transaction.builder().
                            key(req.getKey()).
                            value(req.getValue()).
                            noobChain(req.getNoobChain()).
                            build())
                    .term(currentTerm)
                    .build();
            logModule.write(logEntry);
            final AtomicInteger success = new AtomicInteger(0);

            List<Future<Boolean>> futureList = new CopyOnWriteArrayList<>();

            int count = 0;
            //  复制到其他机器
            for (Peer peer : peerSet) {
                // TODO check self and RaftThreadPool
                count++;
                // 并行发起 RPC 复制.
                futureList.add(replication(peer, logEntry));
            }
            CountDownLatch latch = new CountDownLatch(futureList.size());
            List<Boolean> resultList = new CopyOnWriteArrayList<>();

            getRPCAppendResult(futureList, latch, resultList);
            try {
                latch.await(4000, MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (Boolean aBoolean : resultList) {
                if (aBoolean) {
                    success.incrementAndGet();
                }
            }
            List<Long> matchIndexList = new ArrayList<>(latestIndexes.values());
            // 小于 2, 没有意义
            int median = 0;
            if (matchIndexList.size() >= 2) {
                Collections.sort(matchIndexList);
                median = matchIndexList.size() / 2;
            }
            Long N = matchIndexList.get(median);
            if (N > commitIndex) {
                LogEntry entry = logModule.read(N);
                if (entry != null && entry.getTerm() == currentTerm) {
                    commitIndex = N;
                }
            }
            //  响应客户端(成功一半)
            if (success.get() >= (count / 2)) {
                // 更新
                commitIndex = logEntry.getIndex();
                //  应用到状态机
                getStateMachine().apply(logEntry);
                lastApplied = commitIndex;

              //  LOGGER.info("success apply local state machine,  logEntry info : {}", logEntry);
                // 返回成功.
                return KVAck.builder().success(true).build();
            } else {
                // 回滚已经提交的日志.
                logModule.removeLogs(logEntry.getIndex());
                //LOGGER.warn("fail apply local state  machine,  logEntry info : {}", logEntry);
                // TODO 不应用到状态机,但已经记录到日志中.由定时任务从重试队列取出,然后重复尝试,当达到条件时,应用到状态机.
                // 这里应该返回错误, 因为没有成功复制过半机器.
                return KVAck.builder().success(false).build();
            }
        }
        return KVAck.builder().success(false).build();
    }
    private void getRPCAppendResult(List<Future<Boolean>> futureList, CountDownLatch latch, List<Boolean> resultList) {
        for (Future<Boolean> future : futureList) {
            RaftThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        resultList.add(future.get(3000, MILLISECONDS));
                    } catch (CancellationException | TimeoutException | ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        resultList.add(false);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
    }
    /** 复制到其他机器  */
    public Future<Boolean> replication(Peer peer, LogEntry entry) {

        return null;
    }
    @Override
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
