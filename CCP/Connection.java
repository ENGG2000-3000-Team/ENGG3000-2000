package CCP;
import java.util.Queue;

public class Connection {
    private String name;
    private boolean status;
    private Queue<String> messages;

    Connection(String n, boolean s) {
        name = n;
        status = s;
    }
    public boolean establishConnection() {
        //TODO
        System.out.println("Established Connection with "+name);

        status = true;
        return status;
    }

    public boolean getStatus() {
        return status;
    }

    public void startListening() {
        //TODO
        System.out.println("Started Listening for "+name+" message");
    }

    public String viewMSGRecent() {
        return messages.peek();
    }

    public String getMSGRecent() {
        return messages.poll();
    }
    public void updateStatus(String cMsg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateStatus'");
    }
}
