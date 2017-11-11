package com.linda.framework.rpc;

import com.linda.framework.rpc.exception.RpcException;
import com.linda.framework.rpc.filter.RpcFilter;
import com.linda.framework.rpc.filter.RpcFilterChain;
import com.linda.framework.rpc.net.RpcSender;
import org.apache.log4j.Logger;

import java.util.Map;

public class RpcLoginCheckFilter implements RpcFilter {

    private Logger logger = Logger.getLogger(RpcLoginCheckFilter.class);

    @Override
    public void doFilter(RpcObject rpc, RemoteCall call, RpcSender sender, RpcFilterChain chain) {
        String service = call.getService();
        if (service.equals(LoginRpcService.class.getName())) {
            logger.info("----------user login---------------");
            try {
                chain.nextFilter(rpc, call, sender);
                rpc.getRpcContext().put("logined", true);
                rpc.getRpcContext().put("user", call.getArgs());
            } catch (RpcException e) {
                throw e;
            }
            return;
        } else {
            Map<String, Object> context = rpc.getRpcContext();
            if (context.get("logined") == null) {
                throw new RpcException("user not login");
            } else {
                chain.nextFilter(rpc, call, sender);
            }
        }
    }

}
