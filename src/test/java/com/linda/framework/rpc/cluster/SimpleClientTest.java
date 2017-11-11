package com.linda.framework.rpc.cluster;

import com.linda.framework.rpc.HelloRpcService;
import com.linda.framework.rpc.generic.GenericService;
import com.linda.framework.rpc.utils.RpcUtils;

import java.util.HashMap;

public class SimpleClientTest {

    public static void main(String[] args) {
        RpcClusterClient client = new RpcClusterClient();
        client.setRemoteExecutor(new SimpleClusterExecutor());
        client.startService();
        HelloRpcService service = client.register(HelloRpcService.class);
        service.sayHello("this is linda", 32);
        GenericService genService = client.register(GenericService.class);

        String[]                getBeanTypes = new String[]{"com.linda.framework.rpc.TestBean", "int"};
        HashMap<String, Object> map          = new HashMap<String, Object>();
        map.put("limit", 111);
        map.put("offset", 322);
        map.put("order", "trtr");
        map.put("message", "this is a test");
        Object[] getBeanArgs = new Object[]{map, 543543};
        Object   hh          = genService.invoke(null, "com.linda.framework.rpc.HelloRpcService",
                                                 RpcUtils.DEFAULT_VERSION, "getBean", getBeanTypes, getBeanArgs);
        System.out.println(hh);
        client.stopService();
    }
}
