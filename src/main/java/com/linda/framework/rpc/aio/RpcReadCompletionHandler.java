package com.linda.framework.rpc.aio;

import java.nio.channels.CompletionHandler;

/**
 * @author lindezhi
 * 2015年6月13日 下午4:15:31
 */
public class RpcReadCompletionHandler implements CompletionHandler<Integer, AbstractRpcAioConnector> {

    /**
     * 数据接收回调
     */
    @Override
    public void completed(Integer num, AbstractRpcAioConnector connector) {
        if (num != null) {
            connector.readCallback(num);
        }
    }

    /**
     * 连接异常回调
     */
    @Override
    public void failed(Throwable e, AbstractRpcAioConnector connector) {
        connector.handleFail(e, connector);
    }

}
