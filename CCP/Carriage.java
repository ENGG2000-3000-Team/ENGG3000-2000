package CCP;

public class Carriage {
    private int carriageID;
    private int speed;
    private int direction;
    private Segment currSegment;
    private String carriageState;

    Carriage(int spd, int dir, Segment cS, String state) {
        carriageID = 17;
        speed = spd;
        direction = dir;
        currSegment = cS;
        carriageState = state;
    }

    Carriage() {
        carriageID = 0;
        speed = 0;
        direction = 0;
        currSegment = 0;
        carriageState = "state";
    }

    public String getCarriageData() {
        return carriageID + "\n" + speed+"\n"+direction+"\n"+currSegment.getID();
    }

    public void update(String cMsg) {
        System.out.println("updated with this message: "+cMsg);
    }

    public String getState() {
        return carriageState;
    }
}
