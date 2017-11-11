package com.linda.framework.rpc.oio;

import com.linda.framework.rpc.RpcObject;
import com.linda.framework.rpc.exception.RpcException;
import com.linda.framework.rpc.exception.RpcNetExceptionHandler;
import com.linda.framework.rpc.net.AbstractRpcConnector;
import com.linda.framework.rpc.utils.RpcUtils;
import com.linda.framework.rpc.utils.SslUtils;
import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class AbstractRpcOioConnector extends AbstractRpcConnector implements RpcNetExceptionHandler {

    private Socket           socket;
    private DataInputStream  dis;
    private DataOutputStream dos;
    private Logger logger = Logger.getLogger(AbstractRpcOioConnector.class);

    public AbstractRpcOioConnector(AbstractRpcOioWriter writer) {
        super(writer);
        this.init();
    }

    public AbstractRpcOioConnector() {
        this(null);
    }

    private void init() {
        if (this.getRpcWriter() == null) {
            this.setRpcWriter(new SimpleRpcOioWriter());
        }
    }

    public AbstractRpcOioConnector(Socket socket, AbstractRpcOioWriter writer) {
        this(writer);
        this.socket = socket;
    }

    @Override
    public void startService() {
        super.startService();
        try {
            if (socket == null) {
                socket = SslUtils.getSocketInstance(sslContext, sslMode);
                socket.connect(new InetSocketAddress(this.getHost(), this.getPort()));
            }
            InetSocketAddress remoteAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
            remotePort = remoteAddress.getPort();
            remoteHost = remoteAddress.getAddress().getHostAddress();
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            this.getRpcWriter().registerWrite(this);
            this.getRpcWriter().startService();
            new ClientThread().start();
            this.fireStartNetListeners();
        } catch (Exception e) {
            this.handleNetException(e);
        }
    }

    private class ClientThread extends Thread {
        @Override
        public void run() {
            while (!stop) {
                RpcObject rpc = RpcUtils.readDataRpc(dis, AbstractRpcOioConnector.this);
                if (rpc != null) {
                    rpc.setHost(remoteHost);
                    rpc.setPort(remotePort);
                    rpc.setRpcContext(rpcContext);
                    fireCall(rpc);
                }
            }
        }
    }

    @Override
    public void stopService() {
        super.stopService();
        stop = true;
        RpcUtils.close(dis, dos);
        try {
            socket.close();
        } catch (IOException e) {
            //do nothing
        }
        rpcContext.clear();
        sendQueueCache.clear();
    }

    public DataOutputStream getOutputStream() {
        return dos;
    }

    @Override
    public void handleConnectorException(Exception e) {
        this.getRpcWriter().unRegWrite(this);
        this.stopService();
        logger.error("connection caught io exception close");
        throw new RpcException(e);
    }
}
