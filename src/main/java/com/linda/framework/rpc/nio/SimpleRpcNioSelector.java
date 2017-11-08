package com.linda.framework.rpc.nio;

import com.linda.framework.rpc.RpcObject;
import com.linda.framework.rpc.exception.RpcException;
import com.linda.framework.rpc.net.AbstractRpcConnector;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleRpcNioSelector extends AbstractRpcNioSelector {

    private Selector selector;
    private boolean stop    = false;
    private boolean started = false;
    private ConcurrentHashMap<SocketChannel, AbstractRpcNioConnector>      connectorCache;
    private List<AbstractRpcNioConnector>                                  connectors;
    private ConcurrentHashMap<ServerSocketChannel, AbstractRpcNioAcceptor> acceptorCache;
    private List<AbstractRpcNioAcceptor>                                   acceptors;
    private static final int                  READ_OP       = SelectionKey.OP_READ;
    private static final int                  READ_WRITE_OP = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    private              LinkedList<Runnable> selectTasks   = new LinkedList<Runnable>();

    private AbstractRpcNioSelector delegageSelector;

    private Logger logger = Logger.getLogger(SimpleRpcNioSelector.class);

    public SimpleRpcNioSelector() {
        super();
        try {
            selector = Selector.open();
            connectorCache = new ConcurrentHashMap<SocketChannel, AbstractRpcNioConnector>();
            connectors = new CopyOnWriteArrayList<AbstractRpcNioConnector>();
            acceptorCache = new ConcurrentHashMap<ServerSocketChannel, AbstractRpcNioAcceptor>();
            acceptors = new CopyOnWriteArrayList<AbstractRpcNioAcceptor>();
        } catch (IOException e) {
            throw new RpcException(e);
        }
    }

    @Override
    public void register(final AbstractRpcNioAcceptor acceptor) {
        final ServerSocketChannel channel = acceptor.getServerSocketChannel();
        this.addSelectTask(new Runnable() {
            @Override
            public void run() {
                try {
                    channel.register(selector, SelectionKey.OP_ACCEPT);
                } catch (Exception e) {
                    acceptor.handleNetException(e);
                }
            }
        });
        this.notifySend(null);
        acceptorCache.put(acceptor.getServerSocketChannel(), acceptor);
        acceptors.add(acceptor);
    }

    @Override
    public void unRegister(AbstractRpcNioAcceptor acceptor) {
        ServerSocketChannel channel = acceptor.getServerSocketChannel();
        acceptorCache.remove(channel);
        acceptors.remove(acceptor);
    }

    @Override
    public void register(final AbstractRpcNioConnector connector) {
        this.addSelectTask(new Runnable() {
            @Override
            public void run() {
                try {
                    SelectionKey selectionKey = connector.getChannel().register(selector, READ_OP);
                    SimpleRpcNioSelector.this.initNewSocketChannel(connector.getChannel(), connector, selectionKey);
                } catch (Exception e) {
                    connector.handleNetException(e);
                }
            }
        });
        this.notifySend(null);
    }

    @Override
    public void unRegister(AbstractRpcNioConnector connector) {
        connectorCache.remove(connector.getChannel());
        connectors.remove(connector);
    }

    private void initNewSocketChannel(SocketChannel channel, AbstractRpcNioConnector connector,
                                      SelectionKey selectionKey) {
        if (connector.getAcceptor() != null) {
            connector.getAcceptor().addConnectorListeners(connector);
        }
        connector.setSelectionKey(selectionKey);
        connectorCache.put(channel, connector);
        connectors.add(connector);
    }

    @Override
    public synchronized void startService() {
        if (!started) {
            new SelectionThread().start();
            started = true;
        }
    }

    @Override
    public void stopService() {
        this.stop = true;
    }

    private boolean doAccept(SelectionKey selectionKey) {
        ServerSocketChannel    server   = (ServerSocketChannel) selectionKey.channel();
        AbstractRpcNioAcceptor acceptor = acceptorCache.get(server);
        try {
            SocketChannel client = server.accept();
            if (client != null) {
                client.configureBlocking(false);
                if (delegageSelector != null) {
                    AbstractRpcNioConnector connector = new AbstractRpcNioConnector(client, delegageSelector);
                    connector.setAcceptor(acceptor);
                    connector.setExecutorService(acceptor.getExecutorService());
                    connector.setExecutorSharable(true);
                    delegageSelector.register(connector);
                    connector.startService();
                } else {
                    AbstractRpcNioConnector connector = new AbstractRpcNioConnector(client, this);
                    connector.setAcceptor(acceptor);
                    connector.setExecutorService(acceptor.getExecutorService());
                    connector.setExecutorSharable(true);
                    this.register(connector);
                    connector.startService();
                }
                return true;
            }
        } catch (Exception e) {
            this.handSelectionKeyException(selectionKey, e);
        }
        return false;
    }

    private void fireRpc(AbstractRpcNioConnector connector, RpcObject rpc) {
        rpc.setHost(connector.getRemoteHost());
        rpc.setPort(connector.getRemotePort());
        rpc.setRpcContext(connector.getRpcContext());
        connector.fireCall(rpc);
    }

    private boolean doRead(SelectionKey selectionKey) {
        boolean                 result    = false;
        SocketChannel           client    = (SocketChannel) selectionKey.channel();
        AbstractRpcNioConnector connector = connectorCache.get(client);
        if (connector != null) {
            try {
                RpcNioBuffer connectorReadBuf = connector.getRpcNioReadBuffer();
                ByteBuffer   channelReadBuf   = connector.getChannelReadBuffer();
                while (!stop) {
                    int read = 0;
                    while ((read = client.read(channelReadBuf)) > 0) {
                        channelReadBuf.flip();
                        byte[] readBytes = new byte[read];
                        channelReadBuf.get(readBytes);
                        connectorReadBuf.write(readBytes);
                        channelReadBuf.clear();
                        while (connectorReadBuf.hasRpcObject()) {
                            RpcObject rpc = connectorReadBuf.readRpcObject();
                            this.fireRpc(connector, rpc);
                        }
                    }
                    if (read < 1) {
                        if (read < 0) {
                            this.handSelectionKeyException(selectionKey, new RpcException());
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                this.handSelectionKeyException(selectionKey, e);
            }
        }
        return result;
    }

    private boolean doWrite(SelectionKey selectionKey) {
        boolean                 result    = false;
        SocketChannel           channel   = (SocketChannel) selectionKey.channel();
        AbstractRpcNioConnector connector = connectorCache.get(channel);
        if (connector.isNeedToSend()) {
            try {
                RpcNioBuffer connectorWriteBuf = connector.getRpcNioWriteBuffer();
                ByteBuffer   channelWriteBuf   = connector.getChannelWriteBuffer();
                while (connector.isNeedToSend()) {
                    RpcObject rpc = connector.getToSend();
                    connectorWriteBuf.writeRpcObject(rpc);
                    channelWriteBuf.put(connectorWriteBuf.readBytes());
                    channelWriteBuf.flip();
                    int wantWrite = channelWriteBuf.limit() - channelWriteBuf.position();
                    int write     = 0;
                    while (write < wantWrite) {
                        write += channel.write(channelWriteBuf);
                    }
                    channelWriteBuf.clear();
                    result = true;
                }
                if (!connector.isNeedToSend()) {
                    selectionKey.interestOps(READ_OP);
                }
            } catch (Exception e) {
                this.handSelectionKeyException(selectionKey, e);
            }
        }
        return result;
    }

    private void logState() {
        int acceptorLen  = acceptors.size();
        int connectorLen = connectors.size();
        logger.info("acceptors:" + acceptorLen + " connectors:" + connectorLen);
    }

    private void handSelectionKeyException(final SelectionKey selectionKey, Exception e) {
        SelectableChannel channel = selectionKey.channel();
        if (channel instanceof ServerSocketChannel) {
            AbstractRpcNioAcceptor acceptor = acceptorCache.get(channel);
            if (acceptor != null) {
                logger.error("acceptor " + acceptor.getHost() + ":" + acceptor.getPort() + " selection error " +
                             e.getClass() + " " + e.getMessage() + " start to shutdown");
                this.fireNetListeners(acceptor, e);
                acceptor.stopService();
            }
        } else {
            AbstractRpcNioConnector connector = connectorCache.get(channel);
            if (connector != null) {
                logger.error("connector " + connector.getHost() + ":" + connector.getPort() + " selection error " +
                             e.getClass() + " " + e.getMessage() + " start to shutdown");
                this.fireNetListeners(connector, e);
                connector.stopService();
            }
        }
        this.logState();
    }

    private boolean doDispatchSelectionKey(SelectionKey selectionKey) {
        boolean result = false;
        try {
            if (selectionKey.isAcceptable()) {
                result = doAccept(selectionKey);
            }
            if (selectionKey.isReadable()) {
                result = doRead(selectionKey);
            }
            if (selectionKey.isWritable()) {
                result = doWrite(selectionKey);
            }
        } catch (Exception e) {
            this.handSelectionKeyException(selectionKey, e);
        }
        return result;
    }

    private class SelectionThread extends Thread {
        @Override
        public void run() {
            logger.info("select thread has started :" + Thread.currentThread().getId());
            while (!stop) {
                if (SimpleRpcNioSelector.this.hasTask()) {
                    SimpleRpcNioSelector.this.runSelectTasks();
                }
                boolean needSend = checkSend();
                try {
                    if (needSend) {
                        selector.selectNow();
                    } else {
                        selector.select();
                    }
                } catch (IOException e) {
                    SimpleRpcNioSelector.this.handleNetException(e);
                }
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    doDispatchSelectionKey(selectionKey);
                }
            }
        }
    }

    private boolean checkSend() {
        boolean needSend = false;
        for (AbstractRpcNioConnector connector : connectors) {
            if (connector.isNeedToSend()) {
                SelectionKey selectionKey = connector.getChannel().keyFor(selector);
                selectionKey.interestOps(READ_WRITE_OP);
                needSend = true;
            }
        }
        return needSend;
    }

    @Override
    public void notifySend(AbstractRpcConnector connector) {
        selector.wakeup();
    }

    private void addSelectTask(Runnable task) {
        selectTasks.offer(task);
    }

    private boolean hasTask() {
        Runnable peek = selectTasks.peek();
        return peek != null;
    }

    private void runSelectTasks() {
        Runnable peek = selectTasks.peek();
        while (peek != null) {
            peek = selectTasks.pop();
            peek.run();
            peek = selectTasks.peek();
        }
    }

    public void setDelegageSelector(AbstractRpcNioSelector delegageSelector) {
        this.delegageSelector = delegageSelector;
    }

    @Override
    public void handleNetException(Exception e) {
        logger.error("selector exception:" + e.getMessage());
    }

}
