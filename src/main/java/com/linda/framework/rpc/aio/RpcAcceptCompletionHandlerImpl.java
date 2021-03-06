package com.linda.framework.rpc.aio;

import com.linda.framework.rpc.Service;

import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * TCP连接AIO接收
 *
 * @author lindezhi
 * 2016年6月13日 下午3:00:39
 */
public class RpcAcceptCompletionHandlerImpl implements CompletionHandler<AsynchronousSocketChannel, AbstractRpcAioAcceptor>, Service {

    /**
     * 公共的readerhandler
     */
    private RpcReadCompletionHandler readHandler;

    /**
     * 公共的writehandler
     */
    private RpcWriteCompletionHandler writeHandler;

    /**
     * 新建连接回调
     */
    @Override
    public void completed(AsynchronousSocketChannel channel, AbstractRpcAioAcceptor acceptor) {
        //新建tcp消息通道
        AbstractRpcAioConnector connector = new AbstractRpcAioConnector(acceptor.getAioWriter(), channel);
        try {
            connector.setReadHandler(readHandler);
            connector.setWriteHandler(writeHandler);
            //将上下文的listener添加到connector中
            acceptor.addConnectorListeners(connector);
            connector.startService();
        } catch (Exception e) {
            connector.handleConnectorException(e);
        } finally {
            //异步接受新的TCP连接
            acceptor.getServerChannel().accept(acceptor, this);
        }
    }

    /**
     * 新建连接失败
     */
    @Override
    public void failed(Throwable th, AbstractRpcAioAcceptor acceptor) {
        acceptor.handleFail(th, acceptor);
    }

    @Override
    public void startService() {
        readHandler = new RpcReadCompletionHandler();
        writeHandler = new RpcWriteCompletionHandler();
    }

    @Override
    public void stopService() {
        readHandler = null;
        writeHandler = null;
    }

}
