package CCP;
public class MCP extends Connection{
    MCP(String n, boolean s) {
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
