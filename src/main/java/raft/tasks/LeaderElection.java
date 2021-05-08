package raft.tasks;

import raft.LogModule;
import raft.common.NodeStatus;
import raft.common.Peer;
import raft.common.PeerSet;
import raft.common.ReqType;
import raft.entity.LogEntry;
import raft.entity.ReqVoteParam;
import raft.entity.ReqVoteResult;
import raft.impl.NodeIMPL;
import raft.rpc.RPCReq;
import raft.rpc.RPCResp;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static raft.concurrent.RaftConcurrent.scheduler;

/**
 * When servers start up, they begin as followers.
 * 1. begins election when election timeout
 * - increment its current term (currentTerm ++
 * - transitions to candidate state
 * - vote for itself
 * - issues RequestVote RPCs in parallel to each of the other servers in the cluster.
 * 2. win election if it receives votes from a majority of the servers in the full cluster
 * for the same term.
 * - send heartbeats to all
 * 3. candidate receive AppendEntries RPC
 * - if leader's term as large as currentTerm
 * - return to follower
 * 4. if timeout， start a new election
 */
public class LeaderElection implements Runnable {
    private final static Logger LOGGER = Logger.getLogger(LeaderElection.class.getName());

    NodeIMPL node;

    ExecutorService exs;

    AtomicInteger[] votesCount = {new AtomicInteger(0)};

    public LeaderElection(NodeIMPL node) {
        this.node = node;
        this.exs = Executors.newFixedThreadPool(4);     // 1 self +  4 peers = 5 nodes in total
    }

    @Override
    public void run() {
        if (node.getStatus() == NodeStatus.LEADER || node.getStatus() == NodeStatus.CANDIDATE) {
            return;
        }
//        if(node.getCurrentTerm() == 0) {
//            try{
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
        startElection();
    }


    void startElection() {
        if (PeerSet.leader != null) {
            node.setLeader(PeerSet.leader);
            return;
        } else {
            long curTime1 = System.currentTimeMillis();
            Random rand = new Random();

            //begin election
            node.setStatus(NodeStatus.CANDIDATE);

            //increment term and vote for itself
            node.setCurrentTerm(node.getCurrentTerm() + 1);
            node.setVotedFor(node.getAddr());

            LOGGER.info("node " + node.getPeer() + "becomes a candidate and begins the election," +
                    " " +
                    "current term: " + node.getCurrentTerm() + " LastEntry: " + node.getLogModule().getLast()
                    + "peerset: " + node.getPeerSet());


            Set<Peer> peerSet = node.getPeerSet();

            //election timeout is (150-300ms)
            int timeout = NodeIMPL.ELECTION_TIMEOUT + rand.nextInt(150);

            CompletableFuture[] cfs = peerSet.stream()
                    .map(peer -> CompletableFuture.supplyAsync(() -> sendVoteReq(peer), this.exs)
                            .thenAccept(this::handleVoteResp))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(cfs).join();

            //candidate receive AppendEntries RPC,  if leader's term as large as currentTerm, return
            // to follower
            boolean flag = true;
            if (node.getStatus() == NodeStatus.FOLLOWER) {
                LOGGER.info("candidate receive AppendEntries RPC from valid leader, return to " +
                        "follower");
                flag = false;
            }
            if (flag) {
                System.out.println(("votesCount: " + votesCount[0].get() + " peer number: " + (node.getPeerSet().size() + 1) / 2));
                //check votes from a majority of the servers, add vote from itself
                if (votesCount[0].get() + 1 > (node.getPeerSet().size() + 1) / 2) {
                    LOGGER.info("The Node " + node.getAddr() + " becomes leader");
                    node.setStatus(NodeStatus.LEADER);

                    // Start heartbeat task
                    HeartBeat heartBeat = new HeartBeat(node);
                    ScheduledFuture<?> scheduledHB = scheduler.scheduleAtFixedRate(heartBeat, 0, NodeIMPL.HEARTBEAT_TICK, TimeUnit.MILLISECONDS);
                    node.setScheduledHeartBeatTask(scheduledHB);

                    //set itself to leader
                    node.setLeader(node.getPeer());
                    PeerSet.leader = node.getPeer();

                } else {
                    waitForAWhile(curTime1, timeout);
                    votesCount[0].set(0);
                    System.out.println("no leader elected yet and start over");
                    node.setVotedFor(null);
                    startElection();
                }
            } else {
                waitForAWhile(curTime1, timeout);
                if (PeerSet.leader == null) {
                    votesCount[0].set(0);
                    node.setLeader(null);
                    startElection();
                }
            }
        }
    }

    public void waitForAWhile(long start, long timeout) {
        long now = System.currentTimeMillis();
        while (true) {
            if (now - start < timeout) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                now = System.currentTimeMillis();
            } else {
                break;
            }
        }
    }


    RPCResp sendVoteReq(Peer peer) {
        LogModule logModule = node.getLogModule();
        long lastTerm = 0;
        LogEntry currLast = logModule.getLast();
        if (currLast != null) {
            lastTerm = currLast.getTerm();
        }
        //update vote request param
        ReqVoteParam reqVoteParam = ReqVoteParam.builder()
                .term(node.getCurrentTerm())
                .candidateId(node.getAddr())
                .lastLogIndex(logModule.getLastIndex())
                .lastLogTerm(lastTerm)
                .build();
        //update RPC request object
        RPCReq rpcReq = RPCReq.builder()
                .requestType(ReqType.REQ_VOTE)
                .param(reqVoteParam)
                .addr(peer.getAddr())
                .build();

//        System.out.println(rpcReq.toString());

        //get the RPC response from client, and add the response to future list
        RPCResp voteResp = node.getRpcClient().sendReq(rpcReq);
        return voteResp;
    }

    void handleVoteResp(RPCResp voteResp) {
        if (voteResp == null) {
            return;
        }
        ReqVoteResult voteResult = (ReqVoteResult) voteResp.getResult();
//        System.out.println("voteResult:" + voteResult);
        if (voteResult == null) {
            return;
        }
        if (voteResult.isVoteGranted()) {
            votesCount[0].incrementAndGet();
        } else {
            long paramTerm = voteResult.getTerm();
            if (paramTerm > node.getCurrentTerm()) {
                node.setCurrentTerm(paramTerm);
            }
        }
    }


}
