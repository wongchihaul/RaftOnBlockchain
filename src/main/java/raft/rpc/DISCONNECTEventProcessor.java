package raft.rpc;

import com.alipay.remoting.Connection;
import com.alipay.remoting.ConnectionEventProcessor;
import org.junit.Assert;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ConnectionEventProcessor for ConnectionEventType.CLOSE
 *
 * @author xiaomin.cxm
 * @version $Id: DISCONNECTEventProcessor.java, v 0.1 Apr 8, 2016 10:58:48 AM xiaomin.cxm Exp $
 */
public class DISCONNECTEventProcessor implements ConnectionEventProcessor {

    private final AtomicBoolean dicConnected = new AtomicBoolean();
    private final AtomicInteger disConnectTimes = new AtomicInteger();

    @Override
    public void onEvent(String remoteAddr, Connection conn) {
        Assert.assertNotNull(conn);
        dicConnected.set(true);
        disConnectTimes.incrementAndGet();
    }

    public boolean isDisConnected() {
        return this.dicConnected.get();
    }

    public int getDisConnectTimes() {
        return this.disConnectTimes.get();
    }

    public void reset() {
        this.disConnectTimes.set(0);
        this.dicConnected.set(false);
    }
}

