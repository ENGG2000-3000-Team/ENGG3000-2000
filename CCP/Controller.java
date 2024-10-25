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
        System.out.println("Initialize");
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
                    System.out.println("MCPConnected");
                    cHandler.getMCP().resetMsgAttempts();
                    currentState = CCPState.MCPConnected;
                } else if (cHandler.getMCP().getAttempts() >= 10) {
                    System.out.println("Error");
                    currentState = CCPState.Error; //Could not initalize with MCP and/or carriage
                } else if (currA - cHandler.getMCP().getTimeSent() > 1000) {
                    cHandler.sendInit();
                    System.out.println("SentInit");
                }
                break;
            case MCPConnected:
                cHandler.recievePacket();
                if (cHandler.getBR().gotINIT()) {
                    cHandler.sendAKINIT();
                    cHandler.getBR().startListening();
                    System.out.println("Listening");
                    currentState = CCPState.Listening;
                }
                break;
            case Listening:
                long currL = System.currentTimeMillis();
                cHandler.recievePacket();
                if (!cHandler.getMCP().getMessages().isEmpty()) {
                    cHandler.getMCP().considerMsgRecent();
                    System.out.println("MCPCmdReceived");
                    currentState = CCPState.MCPCmdReceived;
                } else if (!cHandler.getBR().getMessages().isEmpty()) {
                    cHandler.getBR().considerMsgRecent();
                    System.out.println("BRMsgReceived");
                    currentState = CCPState.BRMsgReceived;
                } else if ((currL - cHandler.getBR().getlastMsgTime() > 2000)) {
                    cHandler.sendSTATRQ();
                    cHandler.getBR().setlastMsgTime(System.currentTimeMillis());
                    System.out.println("SentStateREQ");
                    currentState = CCPState.SentStateREQ;
                } else if ((currL - cHandler.getMCP().getlastMsgTime() > 2000)) {
                    cHandler.getMCP().setStatus(false);
                    System.out.println("Error");
                    currentState = CCPState.Error;
                }
                break;
            case MCPCmdReceived: //Either gives carriage new instruction or asks status update
                if (isStatusReq(cHandler.getMCP().viewConsidered())) {
                    cHandler.sendSTAT(br17.getState());
                    System.out.println("Listening");
                    currentState = CCPState.Listening;
                } else {
                    cHandler.sendEXEC((String) cHandler.getMCP().viewConsidered().get("action"));
                    System.out.println("SentInstruction");
                    currentState = CCPState.SentInstruction;
                }
                break;
            case BRMsgReceived:
                br17.update(cHandler.getBR().viewConsidered());
                if (br17.getState() == "ERR" || br17.getState() == "OFLN" || br17.getState() == "STOPO" || br17.getState() == "RSLOWC" || br17.getState() == "FFASTC" || br17.getState() == "FSLOWC" || br17.getState() == "STOPC") {
                    cHandler.sendSTAT(br17.getState());
                    System.out.println("SentData");
                    currentState = CCPState.SentData;
                } else {
                    System.out.println("Listening");
                    currentState = CCPState.Listening;
                }
                break;
            case SentInstruction:
                cHandler.getBR().resetMsgAttempts();
                cHandler.recievePacket();
                if (cHandler.getBR().gotAckEx()) {
                    cHandler.getBR().resetMsgAttempts();
                    System.out.println("Listening");
                    currentState = CCPState.Listening;
                } else if (cHandler.getBR().getAttempts() > 3) {
                    cHandler.getBR().setStatus(false);
                    System.out.println("Error");
                    currentState = CCPState.Error;
                } else if (System.currentTimeMillis() - cHandler.getBR().getTimeSent() > 1000) {
                    System.out.println(cHandler.getBR().getAttempts());
                    cHandler.sendSTATRQ();
                }
                break;
            case SentStateREQ:
                cHandler.getBR().resetMsgAttempts();
                cHandler.recievePacket();
                if (cHandler.getBR().gotStateUpdate()) {
                    cHandler.getBR().resetMsgAttempts();
                    System.out.println("BRMsgReceived");
                    currentState = CCPState.BRMsgReceived;
                } else if (cHandler.getBR().getAttempts() >= 3) {
                    cHandler.getBR().setStatus(false);
                    System.out.println("Error");
                    currentState = CCPState.Error;
                } else if (System.currentTimeMillis() - cHandler.getBR().getTimeSent() > 1000) {
                    System.out.println(cHandler.getBR().getAttempts());
                    cHandler.sendSTATRQ();
                }
                break;
            case SentData:
                cHandler.getMCP().resetMsgAttempts();
                cHandler.recievePacket();
                if (cHandler.getMCP().gotAckSt()) {
                    cHandler.getMCP().resetMsgAttempts();
                    System.out.println("Listening");
                    currentState = CCPState.Listening;
                } else if (cHandler.getMCP().getAttempts() > 3) {
                    cHandler.getMCP().setStatus(false);
                    System.out.println("Error");
                    currentState = CCPState.Error;
                } else if (System.currentTimeMillis() - cHandler.getMCP().getTimeSent() > 1000) {
                    System.out.println(cHandler.getMCP().getAttempts());
                    cHandler.sendSTAT(br17.getState());
                }
                break;
            case Error:
                //Types: Lost Connection with MCP and/or BR
                if (!cHandler.getMCP().getStatus()) { //lost connection with MCP
                    cHandler.sendEXEC("STOP");
                    currentState = CCPState.DEAD;
                }
                if (!cHandler.getBR().getStatus() && cHandler.getMCP().getStatus()) { //lost connection with br17
                    cHandler.sendSTAT("ERR");
                    System.out.println("MCPConnected");
                    currentState = CCPState.MCPConnected;
                }
                if (!cHandler.getBR().getStatus() && !cHandler.getMCP().getStatus()) {
                    System.out.println("Initialize");
                    currentState = CCPState.Initialize;
                }
                break;
            case DEAD:
                System.out.println("- CCP17 Program Died -");
                System.out.println("MCP: " + cHandler.getMCP().getStatus());
                System.out.println("BR17: " + cHandler.getBR().getStatus());
                System.exit(0);
        }
    }
    private static boolean isStatusReq(JSONObject msg) {
        if (msg.get("message").equals("STRQ")) {
            return true;
        }
        return false;
    }
}
