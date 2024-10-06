package CCP;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class BR17 extends Connection{
    protected InetAddress address;
    byte IPAddress[] = {0,0,0,0};
    BR17() {
        super("BR17", false);

        try {
            address = InetAddress.getByAddress(IPAddress);
        } catch (Exception e) {
            System.out.println(""+e);
        }
    }

    public void sendInit() {
        if(status) return;
        timeSent = System.currentTimeMillis();
        msgAttempts++;

        byte[] buffer = "Where are you BR17".getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 0000);
        try {
            socket.send(packet);
        }catch(Exception e) {
            System.out.println(""+e);
        }
    }

    public void sendPacket(String msg) {
        byte[] buffer = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 0000);
        try {
            socket.send(packet);
        }catch(Exception e) {
            System.out.println(""+e);
        }
    }
    
}
