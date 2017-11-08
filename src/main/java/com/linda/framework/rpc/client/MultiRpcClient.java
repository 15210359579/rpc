package com.linda.framework.rpc.client;

import com.linda.framework.rpc.net.AbstractRpcMultiConnector;
import com.linda.framework.rpc.nio.AbstractRpcNioConnector;

public class MultiRpcClient extends AbstractRpcClient {

    private MultiClientRemoteExecutor executor;
    private AbstractRpcMultiConnector connector;
    private int connections = 2;

    @Override
    public AbstractClientRemoteExecutor getRemoteExecutor() {
        return executor;
    }

    public int getConnections() {
        return connections;
    }

    public void setConnections(int connections) {
        this.connections = connections;
    }

    @Override
    public void initConnector(int threadCount) {
        checkConnector();
        connector.setHost(this.getHost());
        connector.setPort(this.getPort());
        connector.setConnectionCount(connections);
        connector.setExecutorThreadCount(threadCount);
        executor = new MultiClientRemoteExecutor(connector);
    }

    private void checkConnector() {
        if (connector == null) {
            connector = new AbstractRpcMultiConnector();
            if (connectorClass == null) {
                connectorClass = AbstractRpcNioConnector.class;
            }
            connector.setConnectorClass(connectorClass);
        }
    }
}
