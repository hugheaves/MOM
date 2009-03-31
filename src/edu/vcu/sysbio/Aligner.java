/*
 * $Log: Aligner.java,v $
 * Revision 1.5  2009/03/31 15:47:28  hugh
 * Updated for 0.2 release
 *
 * Revision 1.4  2008-08-13 19:08:46  hugh
 * Updated.
 *
 * Revision 1.3  2008-07-01 15:59:22  hugh
 * Updated.
 *
 * Revision 1.2  2008-06-25 19:05:56  hugh
 * Performance optimizations
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Base class for all alignment functions. Provides basic alignment algorithm in
 * doAlignment() method. Subclasses implement either query on genome index bases
 * searches.
 * 
 * @author hugh
 * 
 */
public abstract class Aligner {
    protected int mismatchOffsets[];
    protected final int maxMismatches;
    protected final byte[] reference;
    protected final byte[] queries;
    protected final int fileNum;
    protected final Match[] results;
    protected final int maxTotalMismatches;

    public Aligner(byte[] reference, byte[] queries, int fileNum,
            Match[] results) {
        this.mismatchOffsets = new int[ProgramParameters.queryLength];
        this.maxMismatches = ProgramParameters.maxMismatches;
        this.reference = reference;
        this.queries = queries;
        this.fileNum = fileNum;
        this.results = results;
        this.maxTotalMismatches = ProgramParameters.queryLength
                - ProgramParameters.minMatchLength
                + ProgramParameters.maxMismatches;
    }

    /**
     * Find the longest matching substring between the reference and query with
     * "k" mismatches and "q" minimum match size
     * 
     * @param referencePosition
     * @param queryPosition
     * @param forwardDirection
     */
    public final void doAlignment(int referencePosition, int queryPosition) {
        final int queryNum = queryPosition / ProgramParameters.queryLength;

        Match match = results[queryNum];

        if (ProgramParameters.maxMatchesPerQuery != -1
                && match != null
                && match.mismatchData[match.numMismatches] >= ProgramParameters.maxMatchesPerQuery) {
            return;
        }

        final int compareOffset = -(queryPosition % ProgramParameters.queryLength);

        int startPos = compareOffset;
        int endPos = compareOffset;
        final int finalPos = compareOffset + ProgramParameters.queryLength;

        int longestMatchLength = 0;
        int longestMatchPosition = startPos;
        int longestMatchMismatches = 0;
        int longestMatchMismatchesOffset = 0;
        int currentNumMismatches = 0;
        int totalMismatches = 0;

        // "caterpillar" through the string to find the largest matching
        // substring with k mismatches
        while (endPos < finalPos) {

            if (reference[referencePosition + endPos] != queries[queryPosition
                    + endPos]) {

                mismatchOffsets[totalMismatches++] = endPos;

                // quit early if we have more than the max number of
                // possible mismatches (performance optimization)
                if (totalMismatches > maxTotalMismatches) {
                    break;
                }

                // increment mismatching character count
                ++currentNumMismatches;

                if (currentNumMismatches > maxMismatches) {
                    // if we're "maxed out" on mismatches, we "eat" a mismatch
                    // from the beginning
                    startPos = mismatchOffsets[totalMismatches - maxMismatches
                            - 1] + 1;
                    --currentNumMismatches;
                }
            }

            ++endPos;

            // check to see if we have a new "longest substring"
            if ((endPos - startPos) > longestMatchLength) {
                // || ((endPos - startPos) == longestMatchLength &&
                // currentNumMismatches < longestMatchMismatches)) {
                longestMatchLength = endPos - startPos;
                longestMatchMismatches = currentNumMismatches;
                longestMatchPosition = startPos;
                longestMatchMismatchesOffset = totalMismatches;
            }

        }

        if (longestMatchLength >= ProgramParameters.minMatchLength) {
            processMatch(referencePosition, queryPosition, queryNum, match,
                    compareOffset, longestMatchLength, longestMatchPosition,
                    longestMatchMismatches, longestMatchMismatchesOffset);

        }
    }

    protected abstract void processMatch(int referencePosition,
            int queryPosition, final int queryNum, Match match,
            final int compareOffset, int longestMatchLength,
            int longestMatchPosition, int longestMatchMismatches,
            int longestMatchMismatchesOffset);

    public Object clone() throws CloneNotSupportedException {
        Aligner newAligner = (Aligner) super.clone();
        newAligner.mismatchOffsets = new int[ProgramParameters.queryLength];
        return newAligner;
    }
}
