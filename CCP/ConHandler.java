package CCP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ConHandler {
    //Connection Handeling
    private DatagramSocket socket;
    private byte[] buf;
    private JSONParser parser;
    private MCP mcP;
    private BR17 br17Con;

    ConHandler() {
        try {
            socket = new DatagramSocket(3017);
        }catch(Exception e) {
            System.out.println(e);
        }

        parser = new JSONParser();
        br17Con = new BR17();
        mcP = new MCP();
    }

    public void recievePacket() {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);
        }catch(Exception e) {
            System.out.println(e);
        }
        String received = new String(packet.getData(), 0, packet.getLength());
        JSONObject msg = new JSONObject();
        try {
            msg = (JSONObject) parser.parse(received);
        } catch (ParseException e) {
            System.out.println(e);
        }

        if(msg.get("client_type").equals("mcp")) {
            mcP.addMessage(msg);
        }else {
            br17Con.addMessage(msg);
        }
    }

    public void sendInits() {
        mcP.sendInit(socket);
        br17Con.sendInit(socket);
    }

    public void sendEXEC(String cmd) {
        br17Con.sendPacket(cmd,socket);
    }

    public void sendSTATRQ() {
        br17Con.sendPacket("",socket);
    }

    public void sendSTAT(String state) {
        mcP.sendPacket(state,socket);
    }

    public void sendAKEXC() {
        mcP.sendPacket("",socket);
    }

    public boolean gotMCPAck() {
        return !mcP.getStatus() && mcP.gotAck();
    }

    public boolean gotBRAck() {
        return !br17Con.getStatus() && br17Con.gotAck();
    }

    public MCP getMCP() {
        return mcP;
    }

    public BR17 getBR() {
        return br17Con;
    }

    public void close() {
        socket.close();
    }
}
