package com.linda.framework.rpc.nio;

import com.google.common.base.Objects;
import com.linda.framework.rpc.exception.RpcException;
import com.linda.framework.rpc.net.AbstractRpcConnector;
import com.linda.framework.rpc.net.RpcNetListener;
import com.linda.framework.rpc.utils.RpcUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class AbstractRpcNioConnector extends AbstractRpcConnector {

    private SocketChannel          channel;
    private AbstractRpcNioSelector selector;
    private Logger logger = Logger.getLogger(AbstractRpcNioConnector.class);
    private ByteBuffer   channelWriteBuffer;
    private ByteBuffer   channelReadBuffer;
    private SelectionKey selectionKey;

    private RpcNioBuffer rpcNioReadBuffer;
    private RpcNioBuffer rpcNioWriteBuffer;

    private AbstractRpcNioAcceptor acceptor;

    public AbstractRpcNioConnector(SocketChannel socketChanel, AbstractRpcNioSelector selection) {
        this(selection);
        this.channel = socketChanel;
    }

    public AbstractRpcNioConnector(AbstractRpcNioSelector selector) {
        super(null);
        if (selector == null) {
            this.selector = new SimpleRpcNioSelector();
        } else {
            this.selector = selector;
        }
        this.initBuf();
    }

    @Override
    public void addRpcNetListener(RpcNetListener listener) {
        super.addRpcNetListener(listener);
        this.selector.addRpcNetListener(listener);
    }

    public AbstractRpcNioConnector() {
        this(null);
    }

    private void initBuf() {
        channelWriteBuffer = ByteBuffer.allocate(RpcUtils.MEM_512KB);
        channelReadBuffer = ByteBuffer.allocate(RpcUtils.MEM_512KB);
        rpcNioReadBuffer = new RpcNioBuffer(RpcUtils.MEM_512KB);
        rpcNioWriteBuffer = new RpcNioBuffer(RpcUtils.MEM_512KB);
    }

    @Override
    public void startService() {
        super.startService();
        try {
            if (channel == null) {
                channel = SocketChannel.open();
                channel.connect(new InetSocketAddress(this.getHost(), this.getPort()));
                channel.configureBlocking(false);
                while (!channel.isConnected()) { ; }
                logger.info("connect to " + this.getHost() + ":" + this.getPort() + " success");
                selector.startService();
                selector.register(this);
            }
            InetSocketAddress remoteAddress = (InetSocketAddress) channel.socket().getRemoteSocketAddress();
            InetSocketAddress localAddress  = (InetSocketAddress) channel.socket().getLocalSocketAddress();
            String            remote        = RpcUtils.genAddressString("remoteAddress-> ", remoteAddress);
            String            local         = RpcUtils.genAddressString("localAddress-> ", localAddress);
            logger.info(local + "  " + remote);
            remotePort = remoteAddress.getPort();
            remoteHost = remoteAddress.getAddress().getHostAddress();
            this.fireStartNetListeners();
        } catch (IOException e) {
            logger.error("connect to host " + this.getHost() + " port " + this.getPort() + " failed", e);
            throw new RpcException("connect to host error");
        }
    }

    public ByteBuffer getChannelWriteBuffer() {
        return channelWriteBuffer;
    }

    public ByteBuffer getChannelReadBuffer() {
        return channelReadBuffer;
    }

    @Override
    public void stopService() {
        super.stopService();
        this.selector.unRegister(this);
        this.sendQueueCache.clear();
        this.rpcContext.clear();
        try {
            channel.close();
            channelWriteBuffer.clear();
            channelReadBuffer.clear();
            rpcNioReadBuffer.clear();
            rpcNioWriteBuffer.clear();
        } catch (IOException e) {

        }
        this.stop = true;
    }

    public boolean isValid() {
        return !stop;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    public void setSelectionKey(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    @Override
    public void notifySend() {
        selector.notifySend(this);
    }

    public RpcNioBuffer getRpcNioReadBuffer() {
        return rpcNioReadBuffer;
    }

    public RpcNioBuffer getRpcNioWriteBuffer() {
        return rpcNioWriteBuffer;
    }

    public AbstractRpcNioAcceptor getAcceptor() {
        return acceptor;
    }

    public void setAcceptor(AbstractRpcNioAcceptor acceptor) {
        this.acceptor = acceptor;
    }

    @Override
    public void handleConnectorException(Exception e) {
        logger.error("connector " + this.getHost() + ":" + this.getPort() + " io exception start to shutdown");
        this.stopService();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof AbstractRpcNioConnector)) { return false; }
        AbstractRpcNioConnector that = (AbstractRpcNioConnector) o;
        return Objects.equal(getChannel(), that.getChannel()) &&
               Objects.equal(selector, that.selector) &&
               Objects.equal(logger, that.logger) &&
               Objects.equal(getChannelWriteBuffer(), that.getChannelWriteBuffer()) &&
               Objects.equal(getChannelReadBuffer(), that.getChannelReadBuffer()) &&
               Objects.equal(getSelectionKey(), that.getSelectionKey()) &&
               Objects.equal(getRpcNioReadBuffer(), that.getRpcNioReadBuffer()) &&
               Objects.equal(getRpcNioWriteBuffer(), that.getRpcNioWriteBuffer()) &&
               Objects.equal(getAcceptor(), that.getAcceptor());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getChannel(), selector, logger, getChannelWriteBuffer(), getChannelReadBuffer(),
                                getSelectionKey(), getRpcNioReadBuffer(), getRpcNioWriteBuffer(), getAcceptor());
    }
}
