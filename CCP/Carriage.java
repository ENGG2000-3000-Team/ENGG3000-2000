package CCP;

public class Carriage {
    private int carriageID;
    private int speed;
    private int direction;
    private Segment currSegment;
    private String carriageState;

    public String getCarriageData() {
        return carriageID + "\n" + speed+"\n"+direction+"\n"+currSegment.getID();
    }

    public void update(String cMsg) {
        //TODO updates carriage 
    }

    public String getState() {
        return carriageState;
    }
}
