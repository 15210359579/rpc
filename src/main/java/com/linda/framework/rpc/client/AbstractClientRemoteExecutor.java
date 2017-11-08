package com.linda.framework.rpc.client;

import com.linda.framework.rpc.*;
import com.linda.framework.rpc.exception.RpcException;
import com.linda.framework.rpc.net.AbstractRpcConnector;
import com.linda.framework.rpc.net.RpcCallListener;
import com.linda.framework.rpc.net.RpcSender;
import com.linda.framework.rpc.serializer.JdkSerializer;
import com.linda.framework.rpc.serializer.RpcSerializer;
import com.linda.framework.rpc.sync.RpcSync;
import com.linda.framework.rpc.sync.SimpleFutureRpcSync;
import com.linda.framework.rpc.utils.RpcUtils.RpcType;
import org.apache.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractClientRemoteExecutor implements RemoteExecutor, RpcCallListener, Service {
    protected int           timeout = 10000;
    private   AtomicInteger index   = new AtomicInteger(10000);
    private RpcSync       clientRpcSync;
    private RpcSerializer serializer;

    private Logger logger = Logger.getLogger(AbstractClientRemoteExecutor.class);

    public AbstractClientRemoteExecutor() {
        clientRpcSync = new SimpleFutureRpcSync();
        serializer = new JdkSerializer();
    }

    private ConcurrentHashMap<String, RpcCallSync> rpcCache = new ConcurrentHashMap<String, RpcCallSync>();

    @Override
    public void oneway(RemoteCall call) {
        AbstractRpcConnector connector = getRpcConnector(call);
        byte[]               buffer    = serializer.serialize(call);
        int                  length    = buffer.length;
        RpcObject            rpc       = new RpcObject(ONEWAY, this.genIndex(), length, buffer);
        connector.sendRpcObject(rpc, timeout);
    }

    private String genRpcCallCacheKey(long threadId, int index) {
        return "rpc_" + threadId + "_" + index;
    }

    @Override
    public Object invoke(RemoteCall call) {
        AbstractRpcConnector connector = getRpcConnector(call);
        byte[]               buffer    = serializer.serialize(call);
        int                  length    = buffer.length;
        RpcObject            request   = new RpcObject(INVOKE, this.genIndex(), length, buffer);
        RpcCallSync          sync      = new RpcCallSync(request.getIndex(), request);
        rpcCache.put(this.genRpcCallCacheKey(request.getThreadId(), request.getIndex()), sync);
        connector.sendRpcObject(request, timeout);
        clientRpcSync.waitForResult(timeout, sync);
        rpcCache.remove(sync.getIndex());
        RpcObject response = sync.getResponse();
        if (response == null) {
            throw new RpcException("null rpc response");
        }
        if (response.getType() == RpcType.FAIL) {
            String message = "remote rpc call failed";
            if (response.getLength() > 0) {
                message = new String(response.getData());
            }
            throw new RpcException(message);
        }
        if (response.getLength() > 0) {
            return serializer.deserialize(sync.getResponse().getData());
        }
        return null;
    }

    @Override
    public void onRpcMessage(RpcObject rpc, RpcSender sender) {
        RpcCallSync sync = rpcCache.get(this.genRpcCallCacheKey(rpc.getThreadId(), rpc.getIndex()));
        if (sync != null && sync.getRequest().getThreadId() == rpc.getThreadId()) {
            clientRpcSync.notifyResult(sync, rpc);
        }
    }

    private int genIndex() {
        return index.getAndIncrement();
    }

    public abstract AbstractRpcConnector getRpcConnector(RemoteCall call);

    public RpcSerializer getSerializer() {
        return serializer;
    }

    public void setSerializer(RpcSerializer serializer) {
        this.serializer = serializer;
    }
}
