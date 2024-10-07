package CCP;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.json.simple.JSONObject;

public class BR17 extends Connection{
    protected InetAddress address;
    protected byte IPAddress[] = {10,20,30,117};
    BR17() {
        super("BR17", false);

        try {
            address = InetAddress.getByAddress(IPAddress);
        } catch (Exception e) {
            System.out.println(""+e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendInit(DatagramSocket socket) {
        if(status) return;
        timeSent = System.currentTimeMillis();
        msgAttempts++;

        JSONObject msgJ = new JSONObject();
        msgJ.put("client_type", "CCP");
        msgJ.put("message", "CCIN");
        msgJ.put("client_id", "BR17");
        msgJ.put("sequence_number", generateRandom());

        byte[] buffer = msgJ.toJSONString().getBytes();

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 3017);
        try {
            socket.send(packet);
        }catch(Exception e) {
            System.out.println("Sending Error"+e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendPacket(String msg, DatagramSocket socket) {
        JSONObject msgJ = new JSONObject();
        msgJ.put("client_type", "CCP");
        msgJ.put("client_id", "BR17");
        msgJ.put("sequence_number", generateRandom());

        if(!msg.isEmpty()) {
            msgJ.put("message", "EXEC");
            msgJ.put("cmd", msg);
        }else {
            msgJ.put("message", "STATRQ");
        }

        byte[] buffer = msgJ.toJSONString().getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 3017);
        try {
            socket.send(packet);
        }catch(Exception e) {
            System.out.println(""+e);
        }
    }
}
