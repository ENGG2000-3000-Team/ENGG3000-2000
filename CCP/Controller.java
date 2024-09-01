package CCP;
class Controller {
    enum CCPState {
        Initialize, Listening, MCPCmdReceived, SendInstruction,
        InstructionSent, BRDataSent, SendStatus, BRMsgReceived, Error
    }
    private static Carriage br17 = new Carriage();
    private static Connection mcp = new Connection("MCP",false);
    private static Connection carriage = new Connection("Carriage",false);
    private static CCPState currentState = CCPState.Initialize;

    public static void main(String[] args) {     
        while(true) {
            processControl();
        }
    }

    public static void processControl() {
        switch (currentState) {
            case Initialize:
                mcp.establishConnection();
                carriage.establishConnection();
                if(mcp.getStatus() && carriage.getStatus()) {
                    currentState = CCPState.Error;
                }else {
                    currentState = CCPState.Listening;
                    mcp.startListening();
                    carriage.startListening();
                }
                break;
            case Listening:
                if(!mcp.viewMSGRecent().isEmpty()) {
                    currentState = CCPState.MCPCmdReceived;
                }else if(!carriage.viewMSGRecent().isEmpty()) {
                    currentState = CCPState.BRMsgReceived;
                }
                break;
            case MCPCmdReceived:
                if(isValid(mcp.viewMSGRecent())) {
                    processCmd(mcp.getMSGRecent());
                    currentState = CCPState.SendInstruction;
                }else {
                    currentState = CCPState.Error;
                }
                break;
            case BRMsgReceived:
                String CMsg = carriage.getMSGRecent();
                if(!isValid(CMsg)) {
                    currentState = CCPState.Error;
                }else if(furtherActionNeeded(CMsg)){
                    carriage.updateStatus(CMsg);
                    currentState = CCPState.SendInstruction;
                }else {
                    //TODO do we periodically get status updates or do we only request them
                    //Will change if we go to listening or SendStatus
                    carriage.updateStatus(CMsg);
                    currentState = CCPState.Listening;
                }
                break;
            case SendInstruction:
                break;
            case InstructionSent:
                break;
            case BRDataSent:
                break;
            case SendStatus:
                break;
            case Error:
                // Handle error state
                break;
        }
    }

    private static boolean furtherActionNeeded(String cMsg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'furtherActionNeeded'");
    }

    private static void processCmd(String msgRecent) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'processCmd'");
    }

    public static boolean isValid(String cmd) {
        //TODO
        return true;
    }
}