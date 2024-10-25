package CCP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ConnectionHandler {
    //Connection Handeling
    private DatagramSocket socket;
    private byte[] buf;
    private JSONParser parser;
    private MCP mcp;
    private BR17 br17Con;
    private ExecutorService threadExecutor;

    ConnectionHandler() {
        try {
            socket = new DatagramSocket(3017);
        }catch(Exception e) {
            System.out.println(e);
        }
        buf = new byte[1000];

        parser = new JSONParser();
        br17Con = new BR17();
        mcp = new MCP();
        threadExecutor = Executors.newSingleThreadExecutor();
    }

    public void recievePacket() {
        threadExecutor.submit(new RecieverThread(this));
    }

    public void recievePacketAsync() {
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
            System.out.println("Failed to parse:"+e);
            return;
        }

        if(msg.get("client_type").equals("ccp")) {
            mcp.addMessage(msg);
        }else {
            br17Con.addMessage(msg);
        }
    }

    public void sendInit() {
        mcp.sendInit(socket);
    }

    public void sendEXEC(String cmd) {
        br17Con.sendPacket(cmd,socket);
    }

    public void sendSTATRQ() {
        br17Con.sendPacket("",socket);
    }

    public void sendAKINIT() {
        br17Con.sendPacket("AKIN",socket);
    }

    public void sendSTAT(String state) {
        mcp.sendPacket(state,socket);
    }

    public void sendAKEXC() {
        mcp.sendPacket("",socket);
    }

    public MCP getMCP() {
        return mcp;
    }

    public BR17 getBR() {
        return br17Con;
    }

    public void close() {
        socket.close();
    }

    public boolean gotMCPAckIN() {
        return mcp.gotAckIN();
    }
}
