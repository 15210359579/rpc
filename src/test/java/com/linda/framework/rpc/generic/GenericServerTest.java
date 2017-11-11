package com.linda.framework.rpc.generic;

import com.linda.framework.rpc.HelloRpcService;
import com.linda.framework.rpc.HelloRpcServiceImpl;
import com.linda.framework.rpc.nio.AbstractRpcNioAcceptor;
import com.linda.framework.rpc.server.SimpleRpcServer;
import com.linda.framework.rpc.utils.RpcUtils;

public class GenericServerTest {

    public static void main(String[] args) {
        SimpleRpcServer server = new SimpleRpcServer();
        server.addRpcFilter(new RpcContextClearFilter());
        server.setAcceptor(new AbstractRpcNioAcceptor());
        server.setHost("127.0.0.1");
        server.setPort(4445);

        HelloRpcService helloService = new HelloRpcServiceImpl();

        server.register(HelloRpcService.class, new HelloRpcServiceImpl(), RpcUtils.DEFAULT_VERSION, "aapp");
        server.startService();
        System.out.println("server startup");
    }

}
