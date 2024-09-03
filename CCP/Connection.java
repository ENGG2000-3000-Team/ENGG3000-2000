package CCP;
import java.util.Queue;
import java.util.LinkedList;

public class Connection {
    private String name;
    public boolean status;
    public Queue<String> messages;
    private long timeSent;
    private int resentCounter;

    Connection(String n, boolean s) {
        name = n;
        status = s;
        resentCounter = 0;
        messages = new LinkedList<String>();
    }
    public boolean establishConnection() {
        if(status) return true;

        //TODO
        System.out.println("Established Connection with "+name);

        status = true;
        return status;
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

    public void startListening() {
        //TODO
        System.out.println("Started Listening for "+name+" message");
    }

    public String viewMSGRecent() {
        if(messages.isEmpty()) {
            return "";
        }
        return messages.peek();
    }

    public String popMSGRecent() {
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
    
    public void sendPacketData(String br17) {
        resentCounter++;
        // TODO 
        System.out.println("Sent packet with carriage data");
    }
}
