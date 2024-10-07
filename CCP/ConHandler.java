package CCP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ConHandler {
    //Connection Handeling
    protected DatagramSocket socket;
    protected byte[] buf;
    JSONParser parser;
    MCP mcp;
    BR17 br17Con;

    ConHandler() {
        try {
            socket = new DatagramSocket(3017);
        }catch(Exception e) {
            System.out.println(e);
        }

        parser = new JSONParser();
        br17Con = new BR17();
        mcp = new MCP();
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
            mcp.addMessage(msg);
        }else {
            br17Con.addMessage(msg);
        }
    }

    private void close() {
        socket.close();
    }
}
