package com.linda.framework.rpc;

import com.linda.framework.rpc.utils.RpcUtils;

import java.util.List;

public class HostTest {

    public static void main(String[] args) {
        List<String> ips      = RpcUtils.getLocalV4IPs();
        String       chooseIP = RpcUtils.chooseIP(ips);
        for (String ip : ips) {
            System.out.print(ip);
            System.out.println(",");
        }
        System.out.println();
        System.out.println(chooseIP);

    }

}
