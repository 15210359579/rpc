package com.linda.framework.rpc.nio;

import com.linda.framework.rpc.Service;
import com.linda.framework.rpc.exception.RpcNetExceptionHandler;
import com.linda.framework.rpc.net.AbstractRpcNet;
import com.linda.framework.rpc.net.RpcNetListener;
import com.linda.framework.rpc.net.RpcOutputNofity;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractRpcNioSelector implements Service, RpcOutputNofity, RpcNetExceptionHandler {

    public abstract void register(AbstractRpcNioAcceptor acceptor);

    public abstract void unRegister(AbstractRpcNioAcceptor acceptor);

    public abstract void register(AbstractRpcNioConnector connector);

    public abstract void unRegister(AbstractRpcNioConnector connector);

    public AbstractRpcNioSelector() {
        netListeners = new LinkedList<RpcNetListener>();
    }

    protected List<RpcNetListener> netListeners;

    public void addRpcNetListener(RpcNetListener listener) {
        netListeners.add(listener);
    }

    public void fireNetListeners(AbstractRpcNet network, Exception e) {
        for (RpcNetListener listener : netListeners) {
            listener.onClose(network, e);
        }
    }


}
