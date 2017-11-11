package com.linda.framework.rpc.aiorpc;

import com.linda.framework.rpc.HelloRpcService;
import com.linda.framework.rpc.HelloRpcServiceImpl;
import com.linda.framework.rpc.HelloRpcTestService;
import com.linda.framework.rpc.HelloRpcTestServiceImpl;
import com.linda.framework.rpc.aio.AbstractRpcAioAcceptor;
import com.linda.framework.rpc.server.SimpleRpcServer;

public class AioServerTest {

    public static void main(String[] args) {
        SimpleRpcServer server = new SimpleRpcServer();
        server.setHost("127.0.0.1");
        server.setPort(4321);
        server.setAcceptor(new AbstractRpcAioAcceptor());

        server.register(HelloRpcService.class, new HelloRpcServiceImpl());
        server.register(HelloRpcTestService.class, new HelloRpcTestServiceImpl());

        server.startService();

        System.out.println("server started");
    }


}
