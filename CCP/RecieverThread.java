package CCP;

public class RecieverThread implements Runnable{
    private ConHandler handle;
    
    public RecieverThread(ConHandler handle) {
        this.handle = handle;
    }

    public void run() {
        handle.recievePacketAsync();
    }
}
