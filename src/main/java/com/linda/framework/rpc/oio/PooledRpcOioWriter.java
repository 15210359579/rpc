package com.linda.framework.rpc.oio;

import com.linda.framework.rpc.net.AbstractRpcConnector;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PooledRpcOioWriter extends AbstractRpcOioWriter {

    private int threadCount = 2;
    private ExecutorService executorService;

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    @Override
    public void startService() {
        executorService = Executors.newFixedThreadPool(threadCount);
        super.startService();
    }

    @Override
    public void stopService() {
        super.stopService();
        executorService.shutdown();
    }

    @Override
    public boolean doSend(final AbstractRpcConnector connector) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                PooledRpcOioWriter.this.exeSend(connector);
            }
        });
        return true;
    }

}
