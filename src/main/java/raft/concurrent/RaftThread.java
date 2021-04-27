package raft.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author 莫那·鲁道
 * https://github.com/stateIs0/lu-raft-kv/blob/master/lu-raft-kv/src/main/java/cn/think/in/java/current/RaftThread.java
 */
public class RaftThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftThread.class);
    private static final UncaughtExceptionHandler uncaughtExceptionHandler = (t, e)
            -> LOGGER.warn("Exception occurred from thread {}", t.getName(), e);

    public RaftThread(String threadName,  Runnable r) {
        super(r, threadName);
        setUncaughtExceptionHandler(uncaughtExceptionHandler);
    }

}