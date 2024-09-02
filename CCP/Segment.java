package CCP;

public class Segment {
    private int SegmentID;
    private int SegmentLength;

    Segment(int id,int l) {
        SegmentID = id;
        SegmentLength = l;
    }

    public int getID() {
        return SegmentID;
    }

    public int getLength() {
        return SegmentLength;
    }
}
