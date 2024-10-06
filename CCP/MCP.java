package CCP;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class MCP extends Connection{
    protected InetAddress address;
    byte IPAddress[] = {10,20,30,1};
    MCP() {
        super("MCP", false);

        try {
            address = InetAddress.getByAddress(IPAddress);
        } catch (Exception e) {
            System.out.println(""+e);
        }
    }
    
    public void sendInit() {
        if(status) return;
        timeSent = System.currentTimeMillis();
        msgAttempts++;

        byte[] buffer = "Hello CCP17 Exists".getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,address, 0000);
        try {
            socket.send(packet);
        }catch(Exception e) {
            System.out.println(""+e);
        }
    }

    public void sendPacket(String msg) {
        byte[] buffer = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 0000);
        try {
            socket.send(packet);
        }catch(Exception e) {
            System.out.println(""+e);
        }
    }
}
