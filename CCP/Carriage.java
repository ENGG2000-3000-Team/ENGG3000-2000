package CCP;

public class Carriage {
    private int carriageID;
    private int velocity;
    private Segment currSegment;
    private String carriageState;

    Carriage() {
        carriageID = 17;
        velocity = 0;//forawrd is + backward is -
        currSegment = new Segment(0,0);
        carriageState = "state";
    }

    public String getCarriageData() {
        return carriageID + "\n" + velocity+"\n"+currSegment.getID();
    }

    public void update(String cMsg) {
        System.out.println("updated with this message: "+cMsg);
    }

    public String getState() {
        return carriageState;
    }
}
