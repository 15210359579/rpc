package com.linda.framework.rpc.utils;

import com.linda.framework.rpc.exception.RpcException;
import org.apache.log4j.Logger;

import javax.net.ssl.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * ssl的方式，目前不支持
 *
 * @author lindezhi
 * 2016年6月13日 下午4:29:59
 */
public class SslUtils {

    private static Logger logger = Logger.getLogger(SslUtils.class);

    public static Socket getSocketInstance(SSLContext sslContext, int sslmode) {
        Socket socket = null;
        try {
            if (sslContext != null) {
                SSLSocketFactory factory = sslContext.getSocketFactory();
                socket = factory.createSocket();
                postSSLSocket((SSLSocket) socket, sslmode);
            }
        } catch (Exception e) {
            logger.error("create ssl socket error,start to use socket");
        }
        if (socket == null) {
            socket = new Socket();
        }
        return socket;
    }

    public static void postSSLSocket(SSLSocket socket, int sslmode) {
        String[] pwdsuits = socket.getSupportedCipherSuites();
        socket.setEnabledCipherSuites(pwdsuits);
        //双向认证
        if (sslmode == 2) {
            socket.setUseClientMode(false);
            socket.setNeedClientAuth(true);
        } else {
            socket.setUseClientMode(true);
            socket.setWantClientAuth(true);
        }
    }

    private static void postSSLServerSocket(SSLServerSocket sslServerSocket, int sslmode) {
        String[] pwdsuits = sslServerSocket.getSupportedCipherSuites();
        sslServerSocket.setEnabledCipherSuites(pwdsuits);
        sslServerSocket.setUseClientMode(false);
        if (sslmode == 2) {
            sslServerSocket.setNeedClientAuth(true);
        } else {
            sslServerSocket.setWantClientAuth(true);
        }
    }

    public static ServerSocket getServerSocketInstance(SSLContext sslContext, int sslmode) {
        ServerSocket serverSocket = null;
        try {
            if (sslContext != null) {
                SSLServerSocketFactory factory         = sslContext.getServerSocketFactory();
                SSLServerSocket        sslServerSocket = (SSLServerSocket) factory.createServerSocket();
                postSSLServerSocket(sslServerSocket, sslmode);
                sslServerSocket.setReuseAddress(true);
            }
        } catch (Exception e) {
            logger.error("create ssl server error,start to use socket");
        }
        try {
            if (serverSocket == null) {
                serverSocket = new ServerSocket();
            }
        } catch (Exception e) {
            throw new RpcException(e);
        }
        return serverSocket;
    }
}
