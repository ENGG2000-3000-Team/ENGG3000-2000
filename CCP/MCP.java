package CCP;
public class MCP extends Connection{
    MCP() {
        super("MCP",false);
    }

    public void sendInit() {
        if(status) return;
        timeSent = System.currentTimeMillis();
        msgAttempts++;
        
        //TODO
    }

    public void sendPacketData(String br17) {//Go to MCP
        msgAttempts++;
        // TODO 
    }
}
