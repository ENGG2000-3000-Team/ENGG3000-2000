package CCP;

import org.json.simple.JSONObject;

public class Carriage {
    private String carriageState;

    Carriage() {
        carriageState = "state";
    }

    public void update(JSONObject cMsg) {
        System.out.println("updated with this message: "+cMsg);
    }

    public String getState() {
        return carriageState;
    }
}
