package com.linda.framework.rpc.client;

import com.linda.framework.rpc.RemoteCall;
import com.linda.framework.rpc.RemoteExecutor;
import com.linda.framework.rpc.Service;
import com.linda.framework.rpc.exception.RpcException;
import com.linda.framework.rpc.net.AbstractRpcConnector;
import com.linda.framework.rpc.net.AbstractRpcMultiConnector;
import com.linda.framework.rpc.net.RpcCallListener;

public class MultiClientRemoteExecutor extends AbstractClientRemoteExecutor implements RemoteExecutor, RpcCallListener, Service {

    private AbstractRpcMultiConnector connector;

    public MultiClientRemoteExecutor(AbstractRpcMultiConnector connector) {
        super();
        connector.addRpcCallListener(this);
        this.connector = connector;
    }

    @Override
    public void startService() {
        connector.startService();
    }

    @Override
    public void stopService() {
        connector.stopService();
    }

    @Override
    public AbstractRpcConnector getRpcConnector(RemoteCall call) {
        AbstractRpcConnector resource = connector.getResource();
        if (resource == null) {
            throw new RpcException("connection lost");
        }
        return resource;
    }


}
