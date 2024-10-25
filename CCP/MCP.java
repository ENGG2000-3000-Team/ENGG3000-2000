package CCP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.json.simple.JSONObject;

public class MCP extends Connection{
    protected InetAddress address;
    protected byte IPAddress[] = {(byte)127,0,0,1};
    private int mcpPort = 2000;
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
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, mcpPort);
        try {
            socket.send(packet);
        }catch(Exception e) {}
        System.out.println("SENT INIT");
        internalSeq++;
    }

    @SuppressWarnings("unchecked")
    public void sendPacket(String msg, DatagramSocket socket) {
        timeSent = System.currentTimeMillis();
        msgAttempts++;
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
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, mcpPort);
        try {
            socket.send(packet);
        }catch(Exception e) {}
        internalSeq++;
        System.out.println("SENT: "+msg);
    }

    public boolean gotAckSt() {
        if(messages == null) return false;
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
