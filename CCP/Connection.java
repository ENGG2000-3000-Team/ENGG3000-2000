package CCP;
import java.util.Vector;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public abstract class Connection {
    protected String name;
    protected boolean status;

    //Message Handeling
    protected JSONParser parser;
    protected Vector<JSONObject> messages;
    protected JSONObject consideringMsg;

    protected long lastMsgTime;
    protected long timeSent;
    protected int msgAttempts;
    protected int internalSeq = 3291;
    protected int expectedSeq;

    Connection(String n, boolean s) {
        name = n;
        status = s;
        msgAttempts = 0;
        messages = new Vector<JSONObject>();
        consideringMsg = null;
    }

    public void startListening() {
        lastMsgTime = System.currentTimeMillis();
        msgAttempts = 0;
        status = true;
    }

    public void addMessage(JSONObject s) {
        if(expectedSeq < Integer.valueOf(s.get("sequence_number").toString())) {
            expectedSeq = Integer.valueOf(s.get("sequence_number").toString());
        }else if(expectedSeq > Integer.valueOf(s.get("sequence_number").toString())) {
            return;
        }

        if(messages.isEmpty()) {
            messages.add(s);
            return;
        }
        for(int i=0; i<messages.size(); i++) {
            if(Integer.valueOf(messages.get(i).get("sequence_number").toString())>Integer.valueOf(s.get("sequence_number").toString())) {
                messages.add(i,s);
                break;
            }
        }
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

    public void setlastMsgTime(long x) {
        lastMsgTime = x;
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

    public Vector<JSONObject> getMessages() {
        return messages;
    }

    protected boolean gotAckIN() {
        if(messages == null) return false;
        for(int i=0; i<messages.size(); i++) {
            if(messages.get(i).get("message").equals("AKIN")) {
                expectedSeq = Integer.valueOf(messages.get(i).get("sequence_number").toString());
                messages.remove(i);
                return true;
            }
        }
        return false;
    }

    public int getExpectedSeq() {
        return expectedSeq;
    }
}