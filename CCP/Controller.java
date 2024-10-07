package CCP;
import org.json.simple.JSONObject;

class Controller {
    enum CCPState {
        Initialize,
        Listening,
        MCPCmdReceived,
        SendInstruction,
        SentStateREQ,
        AwaitingACKs,
        SendData,
        BRMsgReceived,
        Error
    }
    private static Carriage br17;
    private static ConHandler cHandler;
    private static CCPState currentState = CCPState.Initialize;

    public static void main(String[] args) {
        cHandler = new ConHandler();
        br17 = new Carriage();
        for (;;) {
            processControl();
        }
    }

    public static void processControl() {
        switch (currentState) {
            case Initialize:
                System.out.println("INIT");

                cHandler.sendInits();

                currentState = CCPState.AwaitingACKs;
                break;
            case AwaitingACKs:
                System.out.println("AwaitingACKs");
                long currA = System.currentTimeMillis();

                if (cHandler.gotMCPAck()) {
                    cHandler.getMCP().startListening();
                }
                if (cHandler.gotBRAck()) {
                    cHandler.getBR().startListening();
                }
                if (cHandler.getMCP().getStatus() && cHandler.getBR().getStatus()) {
                    currentState = CCPState.Listening;
                } else if (cHandler.getMCP().getAttempts() >= 10 || cHandler.getBR().getAttempts() >= 10) {
                    currentState = CCPState.Error; //Could not initalize with MCP and/or carriage
                } else if ((currA - cHandler.getMCP().getTimeSent() >= 1000) && (currA - cHandler.getBR().getTimeSent() >= 1000)) { //TODO just placed some arbitrary number
                    cHandler.sendInits();
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
                } else if((currL - cHandler.getBR().getlastMsgTime() > 2000)) {
                    cHandler.sendSTATRQ();
                    currentState = CCPState.SentStateREQ;
                }else if ((currL - cHandler.getMCP().getlastMsgTime() > 2000)) {
                    cHandler.getMCP().setStatus(false);
                    currentState = CCPState.Error;
                }
                
                break;
            case MCPCmdReceived: //Either gives carriage new instruction or asks status update
                System.out.println("MCPCmdReceived");
                if (isValid(cHandler.getMCP().viewConsidered(),1)) {
                    if (isStatusReq(cHandler.getMCP().viewConsidered())) {
                        currentState = CCPState.SendData;
                    } else {
                        currentState = CCPState.SendInstruction;
                    }
                } else { //Invalid msg
                    System.out.println("Ignoring invalid message MCP: <" + cHandler.getMCP().viewConsidered() + ">");
                    currentState = CCPState.Listening;
                }
                break;
            case BRMsgReceived:
                System.out.println("BRMsgReceived");
                if (isValid(cHandler.getBR().viewConsidered(), 0)) { //If the message is invalid
                    br17.update(cHandler.getBR().viewConsidered());
                    if (br17.getState() == "Error" || br17.getState() == "Stopped" || br17.getState() == "AtStation") {
                        currentState = CCPState.SendData;
                    } else {
                        currentState = CCPState.Listening;
                    }
                } else {
                    System.out.println("Ignoring invalid message MCP: <" + cHandler.getBR().viewConsidered() + ">");
                    currentState = CCPState.Listening;
                }
                break;
            case SendInstruction:
                System.out.println("SendInstruction");
                cHandler.sendEXEC((String)cHandler.getMCP().viewConsidered().get("action"));
                currentState = CCPState.Listening;
                break;
            case SentStateREQ:
                System.out.println("SentStateREQ");
                if(!cHandler.getBR().getMessages().isEmpty()) {
                    cHandler.getBR().resetMsgAttempts();
                    currentState = CCPState.BRMsgReceived;
                }else if(cHandler.getBR().getAttempts() >= 3) {
                    cHandler.getBR().setStatus(false);
                    currentState = CCPState.Error;
                }else if((System.currentTimeMillis() - cHandler.getBR().getTimeSent())>= 1000) {
                    System.out.println(cHandler.getBR().getAttempts());
                    cHandler.sendSTATRQ();
                }
                break;
            case SendData:
                System.out.println("SendData");
                cHandler.sendSTAT(br17.getState());
                currentState = CCPState.Listening;
                break;
            case Error:
                System.out.println("Error");
                //Types: Lost Connection with MCP and/or BR
                if (!cHandler.getMCP().getStatus()) { //lost connection with MCP
                    cHandler.sendEXEC("STOP");
                    cHandler.getMCP().resetMsgAttempts();
                    currentState = CCPState.Initialize;
                }
                if (!cHandler.getBR().getStatus()) { //lost connection with br17
                    cHandler.getBR().resetMsgAttempts();
                    currentState = CCPState.Initialize;
                }
                break;
        }
    }

    private static boolean isStatusReq(JSONObject msg) {
        if(msg.get("message").equals("STATRQ")) {
            return true;
        }
        return false;
    }

    public static boolean isValid(JSONObject cmd, int type) {
        if(cmd == null) {
            return false;
        }
        return true;
    }
}