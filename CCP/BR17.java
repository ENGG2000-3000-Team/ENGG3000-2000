package CCP;

public class BR17 extends Connection{
    BR17() {
        super("BR17",false);
    }
    public void sendInit() {
        if(status) return;
        timeSent = System.currentTimeMillis();
        msgAttempts++;
        
        //TODO
    }

    public void sendPacketMsg(String cmd) {
        msgAttempts++;
        //TODO
    }
}
