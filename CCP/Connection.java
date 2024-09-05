package CCP;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.Socket;
import java.util.LinkedList;

public class Connection {
    //Message Handeling
    private String name;
    private boolean status;
    public Queue<String> messages;
    private long timeSent;
    private int resentCounter;

    //Connection Handeling
    Socket clientSocket;
    Socket ServerSocket;
    ExecutorService threadPool;

    private int j;

    Connection(String n, boolean s) {
        name = n;
        status = s;
        resentCounter = 0;
        messages = new LinkedList<String>();
        threadPool = Executors.newFixedThreadPool(10);
    }
    public boolean establishConnection() {
        if(status) return true;
        //TODO
        System.out.println("Established Connection with "+name);

        status = true;
        return status;
    }

    public void sendPacketData(String br17) {
        resentCounter++;
        // TODO 
        System.out.println("Sent packet with carriage data");
    }

    public String recievePacket() {
        return "Test String";
    }

    public void startListening() {
        j++;
        threadPool.submit(new ListenerThread(this, j));
    }

    private void close() {

    }

    public boolean getStatus() {
        return status;
    }

    public long getTimeSent() {
        return timeSent;
    }

    public void setTimeSent(long t) {
        timeSent = t;
    }

    synchronized public String viewMSGRecent() {
        if(messages.isEmpty()) {
            return "";
        }
        return messages.peek();
    }

    synchronized public String popMSGRecent() {
        return messages.poll();
    }

    public void sendPacketCmd(String cmd) {
        resentCounter++;
        //TODO
        System.out.println("Sent BR instructions");
    }
    public boolean gotAck() {
        if(isAck(messages.peek())) {
            messages.poll();
            return true;
        }
        return false;
    }

    public int getResentCount() {
        return resentCounter;
    }

    public void resetResentCount() {
        resentCounter = 0;
    }

    private boolean isAck(String peek) {
        //TODO
        return true;
    }
    
    synchronized public void addMessage(String s, ListenerThread t) {
        messages.add(s);
    }
}
