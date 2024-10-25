package CCP;

public class RecieverThread implements Runnable{
    private ConnectionHandler handle;
    
    public RecieverThread(ConnectionHandler handle) {
        this.handle = handle;
    }

    public void run() {
        while(true) {
            handle.recievePacketAsync();
        }
    }
}
