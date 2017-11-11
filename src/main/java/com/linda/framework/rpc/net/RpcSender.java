package com.linda.framework.rpc.net;

import com.linda.framework.rpc.RpcObject;

public interface RpcSender {
    /**
     * @param rpc
     * @param timeout
     * @return
     */
    public boolean sendRpcObject(RpcObject rpc, int timeout);

}
