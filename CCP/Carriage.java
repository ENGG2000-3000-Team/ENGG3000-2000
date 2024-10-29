package CCP;

import org.json.simple.JSONObject;

public class Carriage {
    private String carriageState;

    Carriage() {
        carriageState = "OFLN";
    }

    public void update(JSONObject cMsg) {
        try {
        carriageState = cMsg.get("state").toString();
        }catch(Exception e) {
            
        }
    }

    public String getState() {
        return carriageState;
    }
}
