package CCP;

public class ListenerThread implements Runnable{
    private Connection con;
    private int id;
    
    public ListenerThread(Connection con, int id) {
        this.con = con;
        this.id = id;
    }

    public void run() {
        while(true) {

            //Replace with listening net code
            System.out.println("Listening on a thread"+id);
            String msg = con.recievePacket();
            if(msg != "") {
                con.addMessage(msg, this);
            }
        }
    }

    synchronized public void interrupt() {
        Thread.currentThread().interrupt();
    }
}
