package CCP;

public class ListenerThread implements Runnable{
    private Connection con;
    
    public ListenerThread(Connection con) {
        this.con = con;
    }

    public void run() {
        while(true) {
            //Replace with listening net code
            String msg = con.recievePacket();
            if(msg != "") {
                con.addMessage(msg);
            }
        }
    }
}
