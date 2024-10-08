package CCP;
import java.util.ArrayList;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public abstract class Connection {
    protected String name;
    protected boolean status;

    //Message Handeling
    protected JSONParser parser;
    protected ArrayList<JSONObject> messages;
    protected JSONObject consideringMsg;

    protected long lastMsgTime;
    protected long timeSent;
    protected int msgAttempts;

    Connection(String n, boolean s) {
        name = n;
        status = s;
        msgAttempts = 0;
        messages = new ArrayList<JSONObject>();
        consideringMsg = null;
    }

    public void startListening() {
        lastMsgTime = System.currentTimeMillis();
        msgAttempts = 0;
        status = true;
    }

    public void addMessage(JSONObject s) {
        messages.add(0,s);
        lastMsgTime = System.currentTimeMillis();
    }

    public JSONObject considerMsgRecent() {
        if(messages.isEmpty()) {
            return null;
        }
        consideringMsg = messages.remove(0);
        return consideringMsg;
    }

    public JSONObject viewConsidered() {
        return consideringMsg;
    }

    public int getAttempts() {
        return msgAttempts;
    }

    public void resetMsgAttempts() {
        msgAttempts = 0;
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

    public ArrayList<JSONObject> getMessages() {
        return messages;
    }

    protected Integer generateRandom() {
        return (int) (Math.random() * (30000 - 1000 + 1) + 1000);
    }

    protected boolean gotAckIN() {
        for(JSONObject o: messages) {
            if(o.get("message").equals("ACKIN")) {
                messages.remove(o);
                return true;
            }
        }
        return false;
    }
}