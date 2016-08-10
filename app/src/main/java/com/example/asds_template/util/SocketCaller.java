package com.example.asds_template.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

/**
 * Created by TingYao on 7/22/2016.
 */
public class SocketCaller {
    String dstAddress;
    int dstPort;

    public SocketCaller(String addr, int port){
        dstAddress = addr;
        dstPort = port;
    }

    public void sendRecord(String rstr){
        Socket socket = null;
        InetAddress serverAddr = null;
        SocketAddress sc_add = null;
        System.out.println("Trying to send hello world");

        try {
            serverAddr = InetAddress.getByName(dstAddress);
            sc_add= new InetSocketAddress(serverAddr,dstPort);
            socket = new Socket();
            socket.connect(sc_add,1500);
            DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
            DOS.writeUTF(rstr);
            //DOS.writeChars(rstr);
            socket.close();

        }catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            //response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            //response = "IOException: " + e.toString();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public String checkCommand(){
        Socket socket = null;
        InetAddress serverAddr = null;
        SocketAddress sc_add = null;
        String response = "none";
        try {
            serverAddr = InetAddress.getByName(dstAddress);
            sc_add= new InetSocketAddress(serverAddr,dstPort);
            socket = new Socket();
            socket.connect(sc_add,2000);
            //DataInputStream in = new DataInputStream(socket.getInputStream());
            //response = in.readUTF();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            response = br.readLine();
            //System.out.println("here here: "+response);

        }catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            //response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            //response = "IOException: " + e.toString();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return response;
    }
}
