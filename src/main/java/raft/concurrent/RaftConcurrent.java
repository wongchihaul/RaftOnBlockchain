package raft.concurrent;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class RaftConcurrent {

    public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    public static final ExecutorService RaftThreadPool = Executors.newCachedThreadPool();


}
