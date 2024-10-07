package CCP;
import java.net.DatagramPacket;
import java.net.InetAddress;

import org.json.simple.JSONObject;

public class BR17 extends Connection{
    protected InetAddress address;
    byte IPAddress[] = {10,20,30,117};
    BR17() {
        super("BR17", false);

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

        JSONObject msg = new JSONObject();
        msg.put("client_type", "CCP");
        msg.put("message", "CCIN");
        msg.put("client_id", "BR17");
        msg.put("sequence_number", generateRandom());

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 0000);
        try {
            socket.send(packet);
        }catch(Exception e) {
            System.out.println(""+e);
        }
    }

    public void sendPacket(String msg) {
        byte[] buffer = msg.getBytes(); //Change for JSON msg
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 0000);
        try {
            socket.send(packet);
        }catch(Exception e) {
            System.out.println(""+e);
        }
    }
    

    private Integer generateRandom() {
        return (int) (Math.random() * (30000 - 1000 + 1) + 1000);
    }
}
