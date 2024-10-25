package CCP;

import org.json.simple.JSONObject;

public class Carriage {
    private String carriageState;

    Carriage() {
        carriageState = "OFLN";
    }

    public void update(JSONObject cMsg) {
        carriageState = cMsg.get("state").toString();
    }

    public String getState() {
        return carriageState;
    }
}
