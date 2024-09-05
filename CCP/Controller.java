package CCP;
class Controller {
    enum CCPState {
        Initialize, Listening, MCPCmdReceived, SendInstruction,
        InstructionSent, BRDataSent, SendData, BRMsgReceived, Error
    }
    private static Carriage br17 = new Carriage();
    private static Connection mcp = new Connection("MCP",false);
    private static Connection carriage = new Connection("Carriage",false);
    private static CCPState currentState = CCPState.Initialize;

    public static void main(String[] args) {
        for(;;) {
            processControl();
        }
    }

    public static void processControl() {
        switch (currentState) {
            case Initialize:
            System.out.println("INIT");
                mcp.establishConnection();
                carriage.establishConnection();
                if(!mcp.getStatus() || !carriage.getStatus()) {
                    currentState = CCPState.Error;
                }else {
                    mcp.startListening();
                    carriage.startListening();
                    currentState = CCPState.Listening;
                }
                break;
            case Listening:
                if(!mcp.viewMSGRecent().isEmpty()) {
                    currentState = CCPState.MCPCmdReceived;
                }else if(!carriage.viewMSGRecent().isEmpty()) {
                    currentState = CCPState.BRMsgReceived;
                }else if(!mcp.getStatus() || !carriage.getStatus()) {//Lost Connection
                    currentState = CCPState.Error;
                }
                break;
            case MCPCmdReceived: //Either gives carriage new instruction or asks status update
            System.out.println("MCPCmdReceived");
                if(isValid(mcp.viewMSGRecent())) {
                    if(isStatusReq(mcp.viewMSGRecent())) {
                        currentState = CCPState.SendData;
                    }else {
                        currentState = CCPState.SendInstruction;
                    }
                }else {//Invalid msg
                    currentState = CCPState.Error;
                }
                break;
            case BRMsgReceived: 
            System.out.println("BRMsgReceived");
                String CMsg = carriage.viewMSGRecent();
                if(!isValid(CMsg)) {//If the message is invalid
                    currentState = CCPState.Error;
                }else {
                    br17.update(CMsg);
                    carriage.popMSGRecent();
                    if(br17.getState() == "Error") {
                        currentState = CCPState.SendData;
                    }else {
                        currentState = CCPState.Listening;
                    }
                }
                break;
            case SendInstruction:
            System.out.println("SendInstruction");
                if(carriage.getStatus()) {
                    carriage.sendPacketCmd(processCmd(mcp.popMSGRecent())); //TODO
                    carriage.setTimeSent(System.currentTimeMillis());
                    currentState = CCPState.InstructionSent;
                }else {
                    currentState = CCPState.Error;//lost connection
                }
                break;
            case InstructionSent:
            System.out.println("InstructionSent");
                if(carriage.gotAck()) {
                    currentState = CCPState.Listening;
                }else if(carriage.getResentCount()>=10) {
                    currentState = CCPState.Error;
                }else if((System.currentTimeMillis()-carriage.getTimeSent())>= 100) {//TODO just placed some arbitrary number
                    currentState = CCPState.SendInstruction;
                }
                break;
            case BRDataSent:
            System.out.println("BRDataSent");
                if(mcp.gotAck()) {
                    currentState = CCPState.Listening;
                }else if(mcp.getResentCount()>=10) {
                    currentState = CCPState.Error;
                }else if((System.currentTimeMillis() - mcp.getTimeSent())>= 100) {//TODO just placed some arbitrary number
                    currentState = CCPState.SendData;
                }
                break;
            case SendData:
            System.out.println("SendData");
                if(mcp.getStatus()) {
                    mcp.sendPacketData(br17.getCarriageData()); //TODO
                    mcp.setTimeSent(System.currentTimeMillis());
                    currentState = CCPState.BRDataSent;
                }else {
                    currentState = CCPState.Error;//Lost connection
                }
                break;
            case Error:
            System.out.println("Error");
                //Types: Lost Connection, Never received Ack, Error in MCPCmd, Error in BR data, 
                if(!mcp.getStatus() || !carriage.getStatus()) {//lost connection
                    currentState = CCPState.Initialize;
                }else if(mcp.viewMSGRecent() != "" && !isValid(mcp.viewMSGRecent())) {
                    //TODO What protocol do we want to do if the was an invalid msg
                    System.out.println("Ignoring invalid message MCP: <"+mcp.popMSGRecent()+">");
                    currentState = CCPState.Listening;
                }else if(mcp.viewMSGRecent() != "" && !isValid(carriage.viewMSGRecent())) {
                    //TODO What protocol do we want to do if the was an invalid msg
                    System.out.println("Ignoring invalid message Br: <"+carriage.popMSGRecent()+">");
                    currentState = CCPState.Listening;
                }else {//Never received Ack I assumed that we just ignore this 
                    currentState = CCPState.Listening;
                    carriage.resetResentCount();
                    mcp.resetResentCount();
                }
                break;
        }
    }

    private static boolean isStatusReq(String viewMSGRecent) {
        //TODO check if it was a status request
        return true;
    }

    private static String processCmd(String msgRecent) {
        //TODO process the command
        return "Processed Command";
    }

    public static boolean isValid(String cmd) {
        //TODO Check msg validity
        return true;
    }
}