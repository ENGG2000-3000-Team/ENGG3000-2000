package CCP;
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
    private static MCP mcp;
    private static BR17 br17Con;
    private static CCPState currentState = CCPState.SendInstruction;

    public static void main(String[] args) {
        br17Con = new BR17();
        // mcp = new MCP();
        // br17 = new Carriage();
        processControl();
        // for (;;) {
        //     processControl();
        // }
    }

    public static void processControl() {
        String brAddress = "10.20.30.117";
        switch (currentState) {
            case Initialize:
                System.out.println("INIT");

                // mcp.sendInit();
                // br17Con.sendInit(brAddress);

                currentState = CCPState.AwaitingACKs;
                break;
            case AwaitingACKs:
                System.out.println("AwaitingACKs");
                long currA = System.currentTimeMillis();

                if (!mcp.getStatus() && mcp.gotAck()) {
                    mcp.startListening();
                }
                if (!br17Con.getStatus() && br17Con.gotAck()) {
                    br17Con.startListening();
                }

                if (mcp.getStatus() && br17Con.getStatus()) {
                    currentState = CCPState.Listening;
                } else if (mcp.getAttempts() >= 10 || br17Con.getAttempts() >= 10) {
                    currentState = CCPState.Error; //Could not initalize with MCP and/or carriage
                } else if ((currA - mcp.getTimeSent() >= 100) && (currA - br17Con.getTimeSent() >= 100)) { //TODO just placed some arbitrary number
                    // mcp.sendInit();
                    // br17Con.sendInit();
                }
                break;
            case Listening://Possibly chnage the way it checks times
                System.out.println("Listening");
                long currL = System.currentTimeMillis();

                if (!mcp.getMessages().isEmpty()) {
                    mcp.considerMsgRecent();
                    currentState = CCPState.MCPCmdReceived;
                } else if (!br17Con.getMessages().isEmpty()) {
                    br17Con.considerMsgRecent();
                    currentState = CCPState.BRMsgReceived;
                } else if((currL - br17Con.getlastMsgTime() > 2500)) {
                    // br17Con.sendPacketMsg("Get_BR_Status");//TODO change string
                    currentState = CCPState.SentStateREQ;
                }else if ((currL - mcp.getlastMsgTime() > 2000)) { //Lost Connection
                    mcp.setStatus(false);
                    currentState = CCPState.Error;
                }
                
                break;
            case MCPCmdReceived: //Either gives carriage new instruction or asks status update
                System.out.println("MCPCmdReceived");
                if (isValid(mcp.viewConsidered(),1)) {
                    if (isStatusReq(mcp.viewConsidered(), 1)) {
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
                // br17Con.sendPacketMsg(processCmd(mcp.viewConsidered()));
                br17Con.sendPacketMsg("FORWARD",brAddress);
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
                    // br17Con.sendPacketMsg("Get_BR_Status");//TODO change string
                }
                break;
            case SendData:
                System.out.println("SendData");
                // mcp.sendPacketData(br17.getCarriageData()); //TODO
                currentState = CCPState.Listening;
                break;
            case Error:
                System.out.println("Error");
                //Types: Lost Connection with MCP and/or BR
                if (!mcp.getStatus()) { //lost connection with MCP
                    // br17Con.sendPacketMsg("Stop at Checkpoint");
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

    private static boolean isStatusReq(String viewMSGRecent, int type) {
        //TODO check if it was a status request
        return true;
    }

    private static String processCmd(String msgRecent) {
        //TODO process the command
        return "Processed Command";
    }

    public static boolean isValid(String cmd, int type) {
        //TODO Check msg validity
        return true;
    }
}
