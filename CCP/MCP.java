package CCP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.json.simple.JSONObject;

public class MCP extends Connection{
    protected InetAddress address;
    protected byte IPAddress[] = {(byte)127,0,0,1};
    MCP() {
        super("MCP", false);

        try {
            address = InetAddress.getByAddress(IPAddress);
        } catch (Exception e) {
            System.out.println("Sending Error"+e);
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
        msgJ.put("sequence_number", internalSeq);

        byte[] buffer = msgJ.toJSONString().getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 2000);
        try {
            socket.send(packet);
        }catch(Exception e) {
            System.out.println(""+e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendPacket(String msg, DatagramSocket socket) {
        JSONObject msgJ = new JSONObject();
        msgJ.put("client_type", "CCP");
        msgJ.put("client_id", "BR17");
        msgJ.put("sequence_number", internalSeq);

        if(!msg.isEmpty()) {
            msgJ.put("message", "STAT");
            msgJ.put("status", msg);
        }else {
            msgJ.put("message", "AKEX");
        }

        byte[] buffer = msgJ.toJSONString().getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 2000);
        try {
            socket.send(packet);
        }catch(Exception e) {
            System.out.println(""+e);
        }
    }

    public boolean gotAckSt() {
        for(int i=0; i<messages.size(); i++) {
            if(messages.get(i).get("message").equals("AKST")) {
                messages.remove(i);
                internalSeq++;
                return true;
            }
        }
        return false;
    }
}
