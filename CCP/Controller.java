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
    private static Connection mcp;
    private static Connection br17Con;
    private static CCPState currentState = CCPState.Initialize;

    public static void main(String[] args) {
        br17Con = new Connection("BR17", false);
        mcp = new Connection("MCP", false);
        br17 = new Carriage();
        for (;;) {
            processControl();
        }
    }

    public static void processControl() {
        switch (currentState) {
            case Initialize:
                System.out.println("INIT");

                mcp.sendInit();
                br17Con.sendInit();

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
                    mcp.sendInit();
                    br17Con.sendInit();
                }
                break;
            case Listening://Possibly chnage the way it checks times
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
                } else if((currL - br17Con.getlastMsgTime() > 2500)) {
                    br17Con.sendPacket("Get_BR_Status");//TODO change string
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
                br17Con.sendPacket(processCmd(mcp.viewConsidered()));
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
                mcp.sendPacket(br17.getCarriageData()); //TODO
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