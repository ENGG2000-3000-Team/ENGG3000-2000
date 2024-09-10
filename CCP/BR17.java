package CCP;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class BR17 extends Connection{
    BR17 () {
        super("BR17", false, 1017);
    }

    public void sendInit (InetAddress address) {
        if (status) return;
        timeSent = System.currentTimeMillis();
        msgAttempts++;
    }

    public void sendPacketMsg (String cmd, String a) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] data = cmd.getBytes(StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName(a);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
            timeSent = System.currentTimeMillis();
            msgAttempts++;
        }

        catch (Exception e) { System.err.println("Socket error: " +
                                                 e.getMessage()); }
    }
}
