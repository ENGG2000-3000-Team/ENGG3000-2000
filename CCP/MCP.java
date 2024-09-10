package CCP;
import java.net.InetAddress;

public class MCP extends Connection{
    MCP () {
        super("MCP", false, 1234);
    }

    public void sendInit (InetAddress address) {
        if (status) return;
        timeSent = System.currentTimeMillis();
        msgAttempts++;
    }

    public void sendPacketData (String br17, InetAddress address) {
        msgAttempts++;
    }
}
