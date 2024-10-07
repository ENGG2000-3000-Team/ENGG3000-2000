package CCP;
import java.util.Queue;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.LinkedList;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public abstract class Connection {
    protected String name;
    protected boolean status;

    //Message Handeling
    protected JSONParser parser;
    protected Queue<JSONObject> messages;
    protected JSONObject consideringMsg;

    protected long lastMsgTime;
    protected long timeSent;
    protected int msgAttempts;

    Connection(String n, boolean s) {
        name = n;
        status = s;
        msgAttempts = 0;
        messages = new LinkedList<JSONObject>();
        consideringMsg = null;
    }

    public void startListening() {
        lastMsgTime = System.currentTimeMillis();
        msgAttempts = 0;
        status = true;
    }

    public void addMessage(JSONObject s) {
        messages.add(s);
        lastMsgTime = System.currentTimeMillis();
    }

    public JSONObject considerMsgRecent() {
        if(messages.isEmpty()) {
            return null;
        }
        consideringMsg = messages.poll();
        return consideringMsg;
    }

    public JSONObject viewConsidered() {
        return consideringMsg;
    }

    public boolean gotAck() {
        if(isAck(messages.peek())) {
            messages.poll();
            return true;
        }
        return false;
    }

    public int getAttempts() {
        return msgAttempts;
    }

    public void resetMsgAttempts() {
        msgAttempts = 0;
    }

    private boolean isAck(JSONObject object) {
        if(object == null || !object.get("message").equals("AKIN")){
            return false;
        }
        return true;
    }

    public boolean getStatus() {
        return status;
    }

    public long getlastMsgTime() {
        return lastMsgTime;
    }

    public long getTimeSent() {
        return timeSent;
    }

    public void setStatus(boolean b) {
        status  = b;
    }

    public Queue<JSONObject> getMessages() {
        return messages;
    }

    protected Integer generateRandom() {
        return (int) (Math.random() * (30000 - 1000 + 1) + 1000);
    }
}