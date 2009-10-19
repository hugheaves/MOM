/*
 * $Log: Match.java,v $
 * Revision 1.5  2009/10/19 17:37:03  hugh
 * Revised.
 *
 * Revision 1.4  2009-03-31 15:47:28  hugh
 * Updated for 0.2 release
 *
 * Revision 1.3  2008-08-13 19:08:46  hugh
 * Updated.
 *
 * Revision 1.2  2008-07-01 15:59:22  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

/**
 * Stores data about an individual match (match location, num mismatches, etc.)
 * 
 * @author hugh
 * 
 */
public class Match {
    int referenceFile;
    int referencePosition;
    int queryPosition;
    int startOffset;
    int length;
    int numMismatches;
    /*
     * This array stores data SNPS. There are two bytes for each SNP. The first
     * in is the offset of the SNP from the beginning of the read. The second
     * byte is the char value of the base in the reference sequence.
     */
    final byte[] mismatchData;

    public Match() {
        this.mismatchData = new byte[ProgramParameters.maxMismatches * 2];
    }

    public final void setMatchData(int referenceFile, int referencePosition,
            int queryPosition, int startOffset, int length, int numMismatches,
            int compareOffset, byte[] reference, int mismatchesOffset,
            int mismatchOffsets[]) {

        this.referenceFile = referenceFile;
        this.referencePosition = referencePosition + compareOffset;
        this.queryPosition = queryPosition + compareOffset;
        this.startOffset = startOffset - compareOffset;
        this.length = length;
        this.numMismatches = numMismatches;

        int j = 0;
        for (int i = 0; i < numMismatches; ++i) {
            int offset = mismatchOffsets[mismatchesOffset - numMismatches + i];
            mismatchData[j++] = (byte) (offset - compareOffset + 1);
            mismatchData[j++] = reference[referencePosition + offset];
        }
    }

    public int hashCode() {
        return referencePosition ^ queryPosition ^ referenceFile;
    }

    public boolean equals(Object o) {
        if (((Match) o).referencePosition == referencePosition
                && ((Match) o).queryPosition == queryPosition
                && ((Match) o).referenceFile == referenceFile)
            return true;
        return false;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("referenceFile = " + referenceFile + ", ");
        buffer.append("referencePosition = " + referencePosition + ", ");
        buffer.append("queryPosition = " + queryPosition + ", ");
        buffer.append("startOffset = " + startOffset + ", ");
        buffer.append("length = " + length + ", ");
        buffer.append("numMismatches = " + numMismatches);
        return buffer.toString();
    }
}
