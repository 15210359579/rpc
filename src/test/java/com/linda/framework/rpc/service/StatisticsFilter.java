package com.linda.framework.rpc.service;

import com.linda.framework.rpc.RemoteCall;
import com.linda.framework.rpc.RpcObject;
import com.linda.framework.rpc.Service;
import com.linda.framework.rpc.filter.RpcFilter;
import com.linda.framework.rpc.filter.RpcFilterChain;
import com.linda.framework.rpc.net.RpcSender;

import java.util.concurrent.atomic.AtomicLong;

public class StatisticsFilter implements RpcFilter, Service {

    private long       start = 0;
    private long       end   = 0;
    private AtomicLong call  = new AtomicLong(0);

    public void reset() {
        this.call.set(0);
        this.start = 0;
        this.end = 0;
    }

    @Override
    public void doFilter(RpcObject rpc, RemoteCall call, RpcSender sender,
                         RpcFilterChain chain) {
        this.call.incrementAndGet();
        chain.nextFilter(rpc, call, sender);
    }

    @Override
    public void startService() {
        this.start = System.currentTimeMillis();
    }

    @Override
    public void stopService() {
        this.end = System.currentTimeMillis();
    }

    public long getTime() {
        return this.end - this.start;
    }

    public long getCall() {
        return this.call.get();
    }

    public long getTps() {
        long time = this.getTime();
        long cc   = this.getCall();
        if (time > 0) {
            return cc * 1000 / time;
        } else {
            return 0;
        }
    }
}
