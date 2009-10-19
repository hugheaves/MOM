/*
 * $Log: ReferenceAlignerMulti.java,v $
 * Revision 1.1  2009/10/19 17:37:03  hugh
 * Revised.
 *
 * Revision 1.5  2009-04-10 17:49:39  hugh
 * Minor bug fixes.
 *
 * Revision 1.4  2009-03-31 15:47:28  hugh
 * Updated for 0.2 release
 *
 * Revision 1.3  2008-09-27 17:08:38  hugh
 * Updated.
 *
 * Revision 1.2  2008-08-13 19:08:46  hugh
 * Updated.
 *
 * Revision 1.1  2008-07-01 15:59:21  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Aligns indexed query file with unindexed reference genome.
 * 
 * @author hugh
 * 
 */
public class ReferenceAlignerMulti extends Aligner implements KmerProcessor {

    private final KmerIndex queriesIndex;
    private int startPos;
    private int endPos;
    private static int segmentCount = 0;
    protected final LongSet hits;

    public ReferenceAlignerMulti(InputFile referenceFile,
            KmerIndex queriesIndex, Match[] results, char[][] mismatchCounts,
            LongSet hits) {
        super(referenceFile.bases, queriesIndex.file.bases,
                referenceFile.fileNum, results, mismatchCounts);
        this.queriesIndex = queriesIndex;
        this.hits = hits;
    }

    public void processKmer(long kmer, int referencePosition) {
        KmerPositionList queryPositions = queriesIndex.getKmerPositions(kmer);

        for (int i = 0; i < queryPositions.positionCount; ++i) {
            // System.out.println ("kmer = " + KmerUtil.kmerToString(kmer,
            // ProgramParameters.kmerLength));
            doAlignment(referencePosition, queryPositions.positions[i]);
        }
    }

    protected void processMatch(int referencePosition, int queryPosition,
            final int queryNum, Match match, final int compareOffset,
            int longestMatchLength, int longestMatchPosition,
            int longestMatchMismatches, int longestMatchMismatchesOffset) {

        if (match == null) {
            synchronized (results) {
                if (results[queryNum] == null) {
                    match = results[queryNum] = new LinkedMatch();
                } else {
                    match = results[queryNum];
                }
            }
        }

        synchronized (match) {

            if (longestMatchLength > match.length
                    || longestMatchLength == match.length
                    && longestMatchMismatches < match.numMismatches) {
                match.setMatchData(fileNum, referencePosition, queryPosition,
                        longestMatchPosition, longestMatchLength,
                        longestMatchMismatches, compareOffset, reference,
                        longestMatchMismatchesOffset, mismatchOffsets);

                ((LinkedMatch) match).nextMatch = null;

                for (int i = 0; i <= ProgramParameters.maxMismatches; ++i) {
                    mismatchCounts[i][queryNum] = 0;
                }
                mismatchCounts[longestMatchMismatches][queryNum] = 1;

                long hit = ((long) referencePosition + (long) compareOffset) << 32;
                hit += queryPosition + compareOffset;
                hits.add(hit);
            } else if (longestMatchLength == match.length) {
                long hit = ((long) referencePosition + (long) compareOffset) << 32;
                hit += queryPosition + compareOffset;
                if (hits.add(hit)) {
                    ++mismatchCounts[longestMatchMismatches][queryNum];

                    if (longestMatchMismatches == match.length) {
                        LinkedMatch newMatch = new LinkedMatch();
                        newMatch.setMatchData(fileNum, referencePosition,
                                queryPosition, longestMatchPosition,
                                longestMatchLength, longestMatchMismatches,
                                compareOffset, reference,
                                longestMatchMismatchesOffset, mismatchOffsets);
                        newMatch.nextMatch = ((LinkedMatch) match).nextMatch;
                        ((LinkedMatch) match).nextMatch = newMatch;
                    }
                }
            }
        }

    }

    public Object call() throws Exception {
        try {
            System.out.println("Searching reference from " + startPos + " to "
                    + endPos);
            KmerUtil.generateReferenceKmers(reference, startPos, endPos,
                    ProgramParameters.kmerLength,
                    ProgramParameters.referenceKmerInterval, this);
            ++segmentCount;
            System.out.println("Completed segment " + segmentCount);
        } catch (Exception e) {
            System.out.println("Unexpected Exception in ReferenceSearch:\n");
            e.printStackTrace(System.out);
            System.exit(1);
        } catch (Error e) {
            System.out.println("Unexpected Error in ReferenceSearch:\n");
            e.printStackTrace(System.out);
            System.exit(1);
        }
        return null;
    }

    public static void resetSegmentCount() {
        segmentCount = 0;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public int getEnd() {
        return endPos;
    }

    public int getStart() {
        return startPos;
    }

    public void setEnd(int end) {
        this.endPos = end;

    }

    public void setStart(int start) {
        this.startPos = start;
    }
}
