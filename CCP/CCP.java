import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.Gson;
import java.util.Map;

public class CCP {
    public static void main (String[] args) {
        int port = 1234;
        Gson gson = new Gson();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try (DatagramSocket socket = new DatagramSocket(port)) {
            Runnable receiveTask = () -> {
                try {
                    byte[] buffer = new byte[1024];

                    while (true) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String receivedData = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                        // System.out.println("Received: " + receivedData);

                        try Map<String, Object> jsonMap = gson.fromJson(receivedData, Map.class);
                        catch (Exception e) System.err.println("Failed to parse JSON: " + e.getMessage());
                    }
                }

                catch (Exception e) System.err.println("Error in receiving task: " + e.getMessage());
            };

            Runnable sendTask = () -> {
                try {
                    String message = "message";
                    byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
                    InetAddress receiverAddress = InetAddress.getByName("localhost");
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receiverAddress, port);
                    socket.send(sendPacket);
                }

                catch (Exception e) System.err.println("Error in sending task: " + e.getMessage());
            };

            executor.submit(receiveTask);
            executor.submit(sendTask);
        }

        catch (Exception e) System.err.println("Socket error: " + e.getMessage());
        finally executor.shutdown();
    }
}
