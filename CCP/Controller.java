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
    private static ConHandler connectionHandler;
    private static CCPState currentState = CCPState.Initialize;

    public static void main(String[] args) {
        connectionHandler = new ConHandler();
        br17 = new Carriage();
        for (;;) {
            processControl();
        }
    }

    public static void processControl() {
        switch (currentState) {
            case Initialize:
                System.out.println("INIT");

                connectionHandler.mcp.sendInit();
                connectionHandler.br17Con.sendInit();

                currentState = CCPState.AwaitingACKs;
                break;
            case AwaitingACKs:
                System.out.println("AwaitingACKs");
                long currA = System.currentTimeMillis();

                if (!connectionHandler.mcp.getStatus() && connectionHandler.mcp.gotAck()) {
                    connectionHandler.mcp.startListening();
                }
                if (!connectionHandler.br17Con.getStatus() && connectionHandler.br17Con.gotAck()) {
                    br17Con.startListening();
                }

                if (mcp.getStatus() && br17Con.getStatus()) {
                    currentState = CCPState.Listening;
                } else if (mcp.getAttempts() >= 10 || br17Con.getAttempts() >= 10) {
                    currentState = CCPState.Error; //Could not initalize with MCP and/or carriage
                } else if ((currA - mcp.getTimeSent() >= 100) && (currA - br17Con.getTimeSent() >= 100)) { //TODO just placed some arbitrary number
                    mcp.sendInit();
                    br17Con.sendInit();
                }
                break;
            case Listening:
                System.out.println("Listening");
                long currL = System.currentTimeMillis();
                mcp.recievePacket();
                br17Con.recievePacket();

                if (!mcp.getMessages().isEmpty()) {
                    mcp.considerMsgRecent();
                    currentState = CCPState.MCPCmdReceived;
                } else if (!br17Con.getMessages().isEmpty()) {
                    br17Con.considerMsgRecent();
                    currentState = CCPState.BRMsgReceived;
                } else if((currL - br17Con.getlastMsgTime() > 2000)) {
                    br17Con.sendPacket("");
                    currentState = CCPState.SentStateREQ;
                }else if ((currL - mcp.getlastMsgTime() > 2000)) {
                    mcp.setStatus(false);
                    currentState = CCPState.Error;
                }
                
                break;
            case MCPCmdReceived: //Either gives carriage new instruction or asks status update
                System.out.println("MCPCmdReceived");
                if (isValid(mcp.viewConsidered(),1)) {
                    if (isStatusReq(mcp.viewConsidered())) {
                        currentState = CCPState.SendData;
                    } else {
                        currentState = CCPState.SendInstruction;
                    }
                } else { //Invalid msg
                    System.out.println("Ignoring invalid message MCP: <" + mcp.viewConsidered() + ">");
                    currentState = CCPState.Listening;
                }
                break;
            case BRMsgReceived:
                System.out.println("BRMsgReceived");
                if (isValid(br17Con.viewConsidered(), 0)) { //If the message is invalid
                    br17.update(br17Con.viewConsidered());
                    if (br17.getState() == "Error" || br17.getState() == "Stopped" || br17.getState() == "AtStation") {
                        currentState = CCPState.SendData;
                    } else {
                        currentState = CCPState.Listening;
                    }
                } else {
                    System.out.println("Ignoring invalid message MCP: <" + br17Con.viewConsidered() + ">");
                    currentState = CCPState.Listening;
                }
                break;
            case SendInstruction:
                System.out.println("SendInstruction");
                br17Con.sendPacket((String)mcp.viewConsidered().get("action"));
                currentState = CCPState.Listening;
                break;
            case SentStateREQ:
                System.out.println("SentStateREQ");
                if(!br17Con.getMessages().isEmpty()) {
                    br17Con.resetMsgAttempts();
                    currentState = CCPState.BRMsgReceived;
                }else if(br17Con.getAttempts() >= 10) {
                    br17Con.setStatus(false);
                    currentState = CCPState.Error;
                }else if((System.currentTimeMillis() - br17Con.getTimeSent())>= 100) {//TODO just placed some arbitrary number
                    System.out.println(br17Con.getAttempts());
                    br17Con.sendPacket("Get_BR_Status");//TODO change string
                }
                break;
            case SendData:
                System.out.println("SendData");
                mcp.sendPacket(br17.getState());
                currentState = CCPState.Listening;
                break;
            case Error:
                System.out.println("Error");
                //Types: Lost Connection with MCP and/or BR
                if (!mcp.getStatus()) { //lost connection with MCP
                    br17Con.sendPacket("Stop at Checkpoint");
                    mcp.resetMsgAttempts();
                    currentState = CCPState.Initialize;
                }
                if (!br17Con.getStatus()) { //lost connection with br17
                    br17Con.resetMsgAttempts();
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