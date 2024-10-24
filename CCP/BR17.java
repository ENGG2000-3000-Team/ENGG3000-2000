package CCP;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.json.simple.JSONObject;

public class BR17 extends Connection{
    protected InetAddress address;
    protected byte IPAddress[] = {10,20,30,117};
    private int brPort = 1234;
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
        msgJ.put("client_type", "BCCP");
        msgJ.put("client_id", "BR17");
        msgJ.put("sequence_number", internalSeq);

        if(msg.equals("AKIN")) {
            msgJ.put("message", msg);
        }else if(!msg.isEmpty()) {
            msgJ.put("message", "EXEC");
            msgJ.put("cmd", msg);
        }else {
            msgJ.put("message", "STATRQ");
            msgAttempts++;
        }

        byte[] buffer = msgJ.toJSONString().getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, brPort);
        try {
            socket.send(packet);
        }catch(Exception e) {}
        timeSent = System.currentTimeMillis();
        msgAttempts++;
    }

    public boolean gotAckEx() {
        if(messages == null) return false;
        for(int i=0; i<messages.size(); i++) {
            if(messages.get(i).get("message").equals("ACKEX")) {
                messages.remove(i);
                internalSeq++;
                return true;
            }
        }
        return false;
    }

    public boolean gotINIT() {
        if(messages == null) return false;
        for(int i=0; i<messages.size(); i++) {
            if(messages.get(i).get("message").equals("BRIN")) {
                expectedSeq = Integer.valueOf(messages.get(i).get("sequence_number").toString());
                messages.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean gotStateUpdate() {
        if(messages == null) return false;
        for(int i=0; i<messages.size(); i++) {
            if(messages.get(i).get("message").equals("STAT")) {
                consideringMsg = messages.get(i);
                internalSeq++;
                return true;
            }
        }
        return false;
    }
}
