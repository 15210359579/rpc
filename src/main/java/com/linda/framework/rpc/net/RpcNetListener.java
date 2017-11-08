package com.linda.framework.rpc.net;

/**
 * 网络异常监听器，用于通知集群更新状态
 *
 * @author Administrator
 */
public interface RpcNetListener {

    public void onClose(AbstractRpcNet network, Exception e);

    public void onStart(AbstractRpcNet network);

}
