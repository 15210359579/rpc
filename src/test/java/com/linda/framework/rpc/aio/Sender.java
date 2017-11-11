package com.linda.framework.rpc.aio;

import com.linda.framework.rpc.utils.SslUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Sender {

    public static void main(String[] args) throws IOException {
        Socket socket = SslUtils.getSocketInstance(null, 1);
        socket.connect(new InetSocketAddress("127.0.0.1", 4321));

        DataInputStream  dis = new DataInputStream(socket.getInputStream());
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String         line   = null;
        System.out.print("input->");
        while ((line = reader.readLine()) != null) {
            dos.writeUTF(line);
            System.out.println("send:" + line);
            String utf = dis.readUTF();
            System.out.println("read:" + utf);
            System.out.print("input->");
        }
        reader.close();
        dis.close();
        dos.close();
    }

}
