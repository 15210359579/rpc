package com.linda.framework.rpc.oio;

import com.linda.framework.rpc.RpcObject;
import com.linda.framework.rpc.net.AbstractRpcConnector;
import com.linda.framework.rpc.net.AbstractRpcWriter;
import com.linda.framework.rpc.utils.RpcUtils;

import java.io.DataOutputStream;

public abstract class AbstractRpcOioWriter extends AbstractRpcWriter {

    public AbstractRpcOioWriter() {
        super();
    }

    public boolean exeSend(AbstractRpcConnector con) {
        boolean                 hasSend   = false;
        AbstractRpcOioConnector connector = (AbstractRpcOioConnector) con;
        DataOutputStream        dos       = connector.getOutputStream();
        while (connector.isNeedToSend()) {
            RpcObject rpc = connector.getToSend();
            RpcUtils.writeDataRpc(rpc, dos, connector);
            hasSend = true;
        }
        return hasSend;
    }

}
