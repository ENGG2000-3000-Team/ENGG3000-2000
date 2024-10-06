package CCP;
import java.util.Queue;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.LinkedList;

public abstract class Connection {
    protected String name;
    protected boolean status;

    //Message Handeling
    //TODO adds message class so messages have id's
    protected Queue<String> messages;
    protected String consideringMsg;

    protected long lastMsgTime;
    protected long timeSent;
    protected int msgAttempts;

    //Connection Handeling
    protected DatagramSocket socket;

    protected byte[] buf;

    Connection(String n, boolean s) {
        name = n;
        status = s;
        msgAttempts = 0;
        messages = new LinkedList<String>();
        consideringMsg = "";

        try {
            socket = new DatagramSocket();
        }catch(Exception e) {
            System.out.println(""+e);
        }
    }

    public String recievePacket() {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);
        }catch(Exception e) {
            System.out.println(""+e);
        }
        String received = new String(packet.getData(), 0, packet.getLength());

        return received;
    }

    public void startListening() {
        lastMsgTime = System.currentTimeMillis();
        msgAttempts = 0;
        status = true;
    }

    public void addMessage(String s) {
        messages.add(s);
        lastMsgTime = System.currentTimeMillis();
    }

    public String considerMsgRecent() {
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
        socket.close();
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
