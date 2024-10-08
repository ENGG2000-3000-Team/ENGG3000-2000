package CCP;
import org.json.simple.JSONObject;

class Controller {
    enum CCPState {
        Initialize,
        Listening,
        MCPCmdReceived,
        SentInstruction,
        SentStateREQ,
        AwaitingACKs,
        SentData,
        BRMsgReceived,
        Error,
        DEAD
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
                cHandler.recievePacket();

                if (cHandler.gotMCPAckIN()) {
                    cHandler.getMCP().startListening();
                }
                if (cHandler.gotBRAckIN()) {
                    cHandler.getBR().startListening();
                }
                if (cHandler.getMCP().getStatus() && cHandler.getBR().getStatus()) {
                    currentState = CCPState.Listening;
                } else if (cHandler.getMCP().getAttempts() >= 10 || cHandler.getBR().getAttempts() >= 10) {
                    currentState = CCPState.Error; //Could not initalize with MCP and/or carriage
                } else {
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
                        cHandler.sendSTAT(br17.getState());
                        currentState = CCPState.SentData;
                    } else {
                        cHandler.sendEXEC((String)cHandler.getMCP().viewConsidered().get("action"));
                        currentState = CCPState.SentInstruction;
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
                    if (br17.getState() == "Error" || br17.getState() == "Stopped" || br17.getState() == "AtStation") {//TODO CHange states
                        cHandler.sendSTAT(br17.getState());
                        currentState = CCPState.SentData;
                    } else {
                        currentState = CCPState.Listening;
                    }
                } else {
                    System.out.println("Ignoring invalid message MCP: <" + cHandler.getBR().viewConsidered() + ">");
                    currentState = CCPState.Listening;
                }
                break;
            case SentInstruction:
                System.out.println("SentInstruction");
                cHandler.recievePacket();

                if(!cHandler.getBR().gotAckEx()) {
                    cHandler.getBR().resetMsgAttempts();
                    currentState = CCPState.BRMsgReceived;
                }else if(cHandler.getBR().getAttempts() >= 3) {
                    cHandler.getBR().setStatus(false);
                    currentState = CCPState.Error;
                }else {
                    System.out.println(cHandler.getBR().getAttempts());
                    cHandler.sendSTATRQ();
                }
                break;
            case SentStateREQ:
                System.out.println("SentStateREQ");
                cHandler.recievePacket();
                if(!cHandler.getBR().gotStateUpdate()) {
                    cHandler.getBR().resetMsgAttempts();
                    currentState = CCPState.BRMsgReceived;
                }else if(cHandler.getBR().getAttempts() >= 3) {
                    cHandler.getBR().setStatus(false);
                    currentState = CCPState.Error;
                }else {
                    System.out.println(cHandler.getBR().getAttempts());
                    cHandler.sendSTATRQ();
                }
                break;
            case SentData:
                System.out.println("SentData");
                
                cHandler.recievePacket();
                if(!cHandler.getMCP().gotAckSt()) {
                    cHandler.getMCP().resetMsgAttempts();
                    currentState = CCPState.Listening;
                }else if(cHandler.getMCP().getAttempts() >= 3) {
                    cHandler.getMCP().setStatus(false);
                    currentState = CCPState.Error;
                }else {
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
                if (!cHandler.getBR().getStatus()) { //lost connection with br17
                    cHandler.sendSTAT("ERR");
                    currentState = CCPState.DEAD;
                }
                break;
            case DEAD:
                System.out.println("- Program Died -");
                System.out.println("MCP: "+cHandler.getMCP().getStatus());
                System.out.println("BR17: "+cHandler.getBR().getStatus());
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