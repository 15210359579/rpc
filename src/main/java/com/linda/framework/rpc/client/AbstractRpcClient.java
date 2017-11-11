package com.linda.framework.rpc.client;

import com.linda.framework.rpc.net.AbstractRpcConnector;
import com.linda.framework.rpc.net.AbstractRpcNetworkBase;
import com.linda.framework.rpc.serializer.RpcSerializer;
import com.linda.framework.rpc.utils.RpcUtils;

public abstract class AbstractRpcClient extends AbstractRpcNetworkBase {

    /**
     * 序列化可设置
     */
    private RpcSerializer serializer;

    private SimpleClientRemoteProxy proxy = new SimpleClientRemoteProxy();

    protected Class<? extends AbstractRpcConnector> connectorClass;
    /**
     * 默认2
     */
    private int executorThreadCount = 2;

    public abstract AbstractClientRemoteExecutor getRemoteExecutor();

    public Class<? extends AbstractRpcConnector> getConnectorClass() {
        return connectorClass;
    }

    public void setConnectorClass(Class<? extends AbstractRpcConnector> connectorClass) {
        this.connectorClass = connectorClass;
    }

    public abstract void initConnector(int threadCount);

    public <T> T register(Class<T> iface) {
        return this.register(iface, RpcUtils.DEFAULT_VERSION);
    }

    public <T> T register(Class<T> iface, String version) {
        return this.register(iface, version, RpcUtils.DEFAULT_GROUP);
    }

    public <T> T register(Class<T> iface, String version, String group) {
        return proxy.registerRemote(iface, version, group);
    }

    @Override
    public void startService() {
        initConnector(executorThreadCount);
        AbstractClientRemoteExecutor executor = getRemoteExecutor();
        if (serializer != null) {
            executor.setSerializer(serializer);
        }
        proxy.setRemoteExecutor(executor);
        proxy.startService();
    }

    @Override
    public void stopService() {
        proxy.stopService();
    }

    public RpcSerializer getSerializer() {
        return serializer;
    }

    public void setSerializer(RpcSerializer serializer) {
        this.serializer = serializer;
    }

    public String getApplication() {
        return proxy.getApplication();
    }

    public void setApplication(String application) {
        proxy.setApplication(application);
    }
}
