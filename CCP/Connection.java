package CCP;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.Socket;
import java.util.LinkedList;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
// import com.google.gson.Gson;
import java.util.Map;

public abstract class Connection {
    protected String name;
    protected boolean status;

    //Message Handeling
    protected Queue<String> messages;
    protected String consideringMsg;

    protected long lastMsgTime;
    protected long timeSent;
    protected int msgAttempts;

    //Connection Handeling
    protected DatagramSocket socket;
    protected int port;
    protected ExecutorService threadPool;

    Connection(String n, boolean s, int p) {
        name = n;
        status = s;
        msgAttempts = 0;
        messages = new LinkedList<String>();
        consideringMsg = "";
        threadPool = Executors.newFixedThreadPool(4);
        port = p;
        // socket = new DatagramSocket(port);
    }

    synchronized public String recievePacket() {
        return "";
    }

    public void startListening() {
        lastMsgTime = System.currentTimeMillis();
        msgAttempts = 0;
        status = true;
        threadPool.submit(new ListenerThread(this));
    }

    synchronized public void addMessage(String s) {
        messages.add(s);
        lastMsgTime = System.currentTimeMillis();
    }

    synchronized public String considerMsgRecent() {
        if(messages.isEmpty()) {
            return "";
        }
        consideringMsg = messages.poll();
        return consideringMsg;
    }

    public String viewConsidered() {
        return consideringMsg;
    }

    private void close() {
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

    private boolean isAck(String peek) {
        //TODO
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

    public Queue<String> getMessages() {
        return messages;
    }
}
