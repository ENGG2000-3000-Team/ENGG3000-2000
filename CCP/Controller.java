package CCP;

import org.json.simple.JSONObject;

class Controller {
    enum CCPState {
        Initialize,
        Listening,
        MCPCmdReceived,
        SentInstruction,
        SentStateREQ,
        MCPConnected,
        SentData,
        BRMsgReceived,
        Error,
        DEAD
    }
    private static Carriage br17;
    private static ConnectionHandler cHandler;
    private static CCPState currentState = CCPState.Initialize;

    public static void main(String[] args) {
        cHandler = new ConnectionHandler();
        br17 = new Carriage();
        for (;;) {
            processControl();
        }
    }

    public static void processControl() {
        switch (currentState) {
            case Initialize:
                cHandler.recievePacket();
                long currA = System.currentTimeMillis();
                
                if (cHandler.gotMCPAckIN()) {
                    cHandler.getMCP().startListening();
                }
                if (cHandler.getMCP().getStatus()) {
                    currentState = CCPState.MCPConnected;
                } else if (cHandler.getMCP().getAttempts() >= 10) {
                    System.out.println("Couldn't Init");
                    currentState = CCPState.Error; //Could not initalize with MCP and/or carriage
                } else if (currA - cHandler.getMCP().getTimeSent() > 1000){
                    cHandler.sendInit();
                    System.out.println("SentInit");
                }
                break;
            case MCPConnected:
                cHandler.recievePacket();

                if (cHandler.getBR().gotINIT()) {
                    cHandler.sendAKINIT();
                    cHandler.getBR().startListening();
                    currentState = CCPState.Listening;
                }
                break;
            case Listening:
                System.out.println("Listening");
                long currL = System.currentTimeMillis();
                cHandler.recievePacket();

                if (!cHandler.getMCP().getMessages().isEmpty()) {
                    cHandler.getMCP().considerMsgRecent();
                    currentState = CCPState.MCPCmdReceived;
                } else if (!cHandler.getBR().getMessages().isEmpty()) {
                    cHandler.getBR().considerMsgRecent();
                    currentState = CCPState.BRMsgReceived;
                } else if((currL - cHandler.getBR().getlastMsgTime() > 1000)) {
                    cHandler.sendSTATRQ();
                    currentState = CCPState.SentStateREQ;
                }else if ((currL - cHandler.getMCP().getlastMsgTime() > 2000)) {
                    cHandler.getMCP().setStatus(false);
                    currentState = CCPState.Error;
                }
                
                break;
            case MCPCmdReceived: //Either gives carriage new instruction or asks status update
                System.out.println("MCPCmdReceived");
                if (isStatusReq(cHandler.getMCP().viewConsidered())) {
                    cHandler.sendSTAT(br17.getState());
                    currentState = CCPState.Listening;
                } else {
                    cHandler.sendEXEC((String)cHandler.getMCP().processCmd());
                    currentState = CCPState.SentInstruction;
                }
                break;
            case BRMsgReceived:
                System.out.println("BRMsgReceived");
                br17.update(cHandler.getBR().viewConsidered()); 
                if (br17.getState() == "ERROR_HAZARD" || br17.getState() == "STOPPED" || br17.getState() == "STOP_AT_STATION" || br17.getState() == "FSLOW" || br17.getState() == "FFAST") {
                    cHandler.sendSTAT(translate(br17.getState()));
                    currentState = CCPState.SentData;
                } else {
                    currentState = CCPState.Listening;
                }
                break;
            case SentInstruction:
                System.out.println("SentInstruction");
                cHandler.recievePacket();

                if(cHandler.getBR().gotAckEx()) {
                    cHandler.getBR().resetMsgAttempts();
                    currentState = CCPState.Listening;
                }else if(cHandler.getBR().getAttempts() >= 3) {
                    cHandler.getBR().setStatus(false);
                    currentState = CCPState.Error;
                }else if (System.currentTimeMillis() - cHandler.getBR().getlastMsgTime() > 1000){
                    System.out.println(cHandler.getBR().getAttempts());
                    cHandler.sendSTATRQ();
                }
                break;
            case SentStateREQ:
                System.out.println("SentStateREQ");
                cHandler.recievePacket();
                if(cHandler.getBR().gotStateUpdate()) {
                    cHandler.getBR().resetMsgAttempts();
                    currentState = CCPState.BRMsgReceived;
                }else if(cHandler.getBR().getAttempts() >= 3) {
                    cHandler.getBR().setStatus(false);
                    currentState = CCPState.Error;
                }else if(System.currentTimeMillis() - cHandler.getBR().getlastMsgTime() > 1000){
                    System.out.println(cHandler.getBR().getAttempts());
                    cHandler.sendSTATRQ();
                }
                break;
            case SentData:
                System.out.println("SentData");
                
                cHandler.recievePacket();
                if(cHandler.getMCP().gotAckSt()) {
                    cHandler.getMCP().resetMsgAttempts();
                    currentState = CCPState.Listening;
                }else if(cHandler.getMCP().getAttempts() >= 3) {
                    cHandler.getMCP().setStatus(false);
                    currentState = CCPState.Error;
                }else if(System.currentTimeMillis() - cHandler.getMCP().getlastMsgTime() > 1000){
                    System.out.println(cHandler.getMCP().getAttempts());
                    cHandler.sendSTATRQ();
                }

                currentState = CCPState.Listening;
                break;
            case Error:
                System.out.println("Error");
                //Types: Lost Connection with MCP and/or BR
                if (!cHandler.getMCP().getStatus()) { //lost connection with MCP
                    cHandler.sendEXEC("STOP");
                    currentState = CCPState.DEAD;
                }
                if (!cHandler.getBR().getStatus() && cHandler.getMCP().getStatus()) { //lost connection with br17
                    cHandler.sendSTAT("ERR");
                    currentState = CCPState.MCPConnected;
                }
                if(!cHandler.getBR().getStatus() && !cHandler.getMCP().getStatus()) {
                    currentState = CCPState.Initialize;
                }
                break;
            case DEAD:
                System.out.println("- Program Died -");
                System.out.println("MCP: "+cHandler.getMCP().getStatus());
                System.out.println("BR17: "+cHandler.getBR().getStatus());
                System.exit(0);
        }
    }

    private static String translate(String state) {
        String result = "";
        switch (state) {
            case "SLOW_DOWN":
                result = "STOPC";
            break;
            case "STOP_AT_STATION":
                result = "STOPO";
            break;
            case "FSLOW":
                result = "FSLOWC";
            break;
            case "FFAST":
                result = "FFASTC";
            break;
            case "BACKWARDS_SLOW":
                result = "RSLOWC";
            break;
            case "E_STOP":
                result = "DISCONNECT";
            break;
        }
        return result;
    }

    private static boolean isStatusReq(JSONObject msg) {
        if(msg.get("message").equals("STRQ")) {
            return true;
        }
        return false;
    }
}