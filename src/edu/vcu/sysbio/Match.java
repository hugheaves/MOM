/*
 * $Log: Match.java,v $
 * Revision 1.4  2009/03/31 15:47:28  hugh
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
     * This array stores data on mismatch counts and SNPS.
     * 
     * First section of array keeps track of counts of matches for each possible
     * number of mismatches. (i.e. there were 2 matches with 0 mismatches, 6
     * matches with one mismatch, 32 matches with two mismatches, etc.) There is
     * one int in this part of the array for each possible number of mismatches.
     * 
     * The second part of the array keeps track of the actual SNP's. There are
     * two ints for each SNP. The first in is the offset of the SNP from the
     * beginning of the read. The second int is the char value of the base in
     * the reference sequence.
     * 
     * The total size of the array = Maximum Number of Mismatches * 3 + 1
     */
    int[] mismatchData;

    public Match() {
        this.mismatchData = new int[ProgramParameters.maxMismatches * 3 + 1];
    }

    public void setBestMatch(int referenceFile, int referencePosition,
            int queryPosition, int startOffset, int length, int numMismatches) {
        this.referenceFile = referenceFile;
        this.referencePosition = referencePosition;
        this.queryPosition = queryPosition;
        this.startOffset = startOffset;
        this.length = length;
        this.numMismatches = numMismatches;
        for (int i = 0; i <= ProgramParameters.maxMismatches; ++i) {
            mismatchData[i] = 0;
        }
        this.mismatchData[numMismatches] = 1;
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

}
