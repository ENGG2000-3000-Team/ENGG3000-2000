package CCP;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.Socket;
import java.util.LinkedList;

public class Connection {
    private String name;
    private boolean status;

    //Message Handeling
    private Queue<String> messages;
    private String consideringMsg;

    private long lastMsgTime;
    private long timeSent;
    private int msgAttempts;

    //Connection Handeling
    private Socket clientSocket;
    private Socket ServerSocket;
    private ExecutorService threadPool;

    Connection(String n, boolean s) {
        name = n;
        status = s;
        msgAttempts = 0;
        messages = new LinkedList<String>();
        consideringMsg = "";
        threadPool = Executors.newFixedThreadPool(4);
    }
    public void sendInit() {
        if(status) return;
        timeSent = System.currentTimeMillis();
        msgAttempts++;
        
        //TODO
    }

    public void sendPacketData(String br17) {
        msgAttempts++;
        // TODO 
    }

    synchronized public String recievePacket() {
        //TODO
        return "";
    }

    public void startListening() {
        lastMsgTime = System.currentTimeMillis();
        msgAttempts = 0;
        status = true;
        threadPool.submit(new ListenerThread(this));
    }

    public void sendPacketMsg(String cmd) {
        msgAttempts++;
        //TODO
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
