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
    public void sendPacket(String msg, DatagramSocket socket) {
        JSONObject msgJ = new JSONObject();
        msgJ.put("client_type", "FCCP");
        msgJ.put("client_id", "BR17");
        msgJ.put("sequence_number", generateRandom());

        if(msg.equals("AKIN")) {
            msgJ.put("message", msg);
        }else if(!msg.isEmpty()) {
            msgJ.put("message", "EXEC");
            msgJ.put("cmd", msg);
        }else {
            msgJ.put("message", "STATRQ");
        }

        byte[] buffer = msgJ.toJSONString().getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 1234);
        try {
            socket.send(packet);
        }catch(Exception e) {
            System.out.println(""+e);
        }
    }

    public boolean gotAckEx() {
        for(int i=0; i<messages.size(); i++) {
            if(messages.get(i).get("message").equals("ACKEX")) {
                messages.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean gotINIT() {
        for(int i=0; i<messages.size(); i++) {
            if(messages.get(i).get("message").equals("BRIN")) {
                messages.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean gotStateUpdate() {
        for(int i=0; i<messages.size(); i++) {
            if(messages.get(i).get("message").equals("STAT")) {
                consideringMsg = messages.get(i);
                return true;
            }
        }
        return false;
    }
}
