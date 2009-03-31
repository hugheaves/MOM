/*
 * $Log: QueriesAligner.java,v $
 * Revision 1.6  2009/03/31 15:47:28  hugh
 * Updated for 0.2 release
 *
 * Revision 1.5  2008-09-27 17:08:38  hugh
 * Updated.
 *
 * Revision 1.4  2008-08-13 19:08:46  hugh
 * Updated.
 *
 * Revision 1.3  2008-07-01 15:59:21  hugh
 * Updated.
 *
 * Revision 1.2  2008-06-10 13:48:46  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Aligns unindexed query file with indexed reference genome.
 * 
 * @author hugh
 * 
 */
public class QueriesAligner extends Aligner implements KmerProcessor {

    private int startPos;
    private int endPos;
    private static int segmentCount = 0;

    private IntSet matchPositions;
    private KmerIndex referenceIndex;

    public QueriesAligner(InputFile queriesFile, KmerIndex referenceIndex,
            Match[] results, int[] checkedPositions) {
        super(referenceIndex.file.bases, queriesFile.bases,
                referenceIndex.file.fileNum, results);

        this.referenceIndex = referenceIndex;
        this.matchPositions = new IntOpenHashSet();
        // this.matchPositions = new int[0];
        // matchCount = 0;
    }

    public void processKmer(long kmer, int queryPosition) {
        KmerPositionList referencePositions = referenceIndex
                .getKmerPositions(kmer);

        // System.out.println("QueryPos = " + queryPosition + ", kmer = "
        // + KmerUtil.kmerToString(kmer, ProgramParameters.kmerLength));

        int offset = queryPosition % ProgramParameters.queryLength;
        for (int i = 0; i < referencePositions.positionCount; ++i) {
            // System.out.println (" ReferencePos[i] = " +
            // referencePositions.positions[i]);
            // doAlignment(referencePositions.positions[i], queryPosition);
            matchPositions.add(referencePositions.positions[i] - offset);
        }
    }

    // private void merge(int[] positions, int positionCount, int offset) {
    // if (matchCount + positionCount < newMatchPositions.length) {
    // newMatchPositions = new int[matchCount + positionCount];
    // }
    //
    // int i = 0;
    // int j = 0;
    // int k = 0;
    // if (matchPositions[i] < positions[j] - offset) {
    // newMatchPositions[k++] = matchPositions[i++];
    // }
    // else if (matchPositions[i] == positions[j] - offset) {
    // newMatchPositions[k++] = matchPositions[i++];
    // j++;
    // }
    // else {
    // newMatchPositions[k++] = positions[j++] - offset;
    // }
    //
    // }

    public Object call() throws Exception {
        try {
            long mask = KmerUtil.computeMask(ProgramParameters.kmerLength);
            byte ch;
            int currentLength = 0;
            long kmer = 0;

            for (int i = startPos; i < endPos; ++i) {

                ch = queries[i];
                if (ch != InputFile.NOMATCH_CHAR
                        && ch != InputFile.WILDCARD_CHAR) {
                    ++currentLength;
                    kmer = KmerUtil.addByteToKmer(kmer, mask, queries[i]);
                    if (currentLength >= ProgramParameters.kmerLength) {
                        processKmer(kmer, i - ProgramParameters.kmerLength + 1);
                    }
                    if ((i + 1) % ProgramParameters.queryLength == 0) {
                        currentLength = 0;
                        doAlignments(i);
                        matchPositions.clear();
                    }
                } else {
                    currentLength = 0;
                }
            }

            ++segmentCount;
            System.out.println("Completed segment " + segmentCount);

        } catch (Exception e) {
            System.out.println("Unexpected Exception in QueriesAligner:\n");
            e.printStackTrace(System.out);
            System.exit(1);
        } catch (Error e) {
            System.out.println("Unexpected Error in QueriesAligner:\n");
            e.printStackTrace(System.out);
            System.exit(1);
        }
        return null;
    }

    private void doAlignments(int queryPos) {
        queryPos -= queryPos % ProgramParameters.queryLength;
        for (IntIterator i = matchPositions.iterator(); i.hasNext();) {
            int pos = i.nextInt();
            doAlignment(pos, queryPos);
        }

    }

    protected void processMatch(int referencePosition, int queryPosition,
            final int queryNum, Match match, final int compareOffset,
            int longestMatchLength, int longestMatchPosition,
            int longestMatchMismatches, int longestMatchMismatchesOffset) {

        if (match == null) {
            match = results[queryNum] = new Match();
        }

        if (longestMatchLength > match.length
                || longestMatchLength == match.length
                && longestMatchMismatches < match.numMismatches) {
            match.setBestMatch(fileNum, referencePosition + compareOffset,
                    queryPosition + compareOffset, longestMatchPosition
                            - compareOffset, longestMatchLength,
                    longestMatchMismatches);

            if (longestMatchMismatches > 0) {
                int j = ProgramParameters.maxMismatches + 1;
                for (int i = 0; i < longestMatchMismatches; ++i) {
                    int offset = mismatchOffsets[longestMatchMismatchesOffset
                            - longestMatchMismatches + i];
                    match.mismatchData[j++] = (byte) (offset - compareOffset + 1);
                    match.mismatchData[j++] = reference[referencePosition
                            + offset];
                }
            }
            matchPositions.add(referencePosition + compareOffset);
        } else if (longestMatchLength == match.length) {
            if (matchPositions.add(referencePosition + compareOffset)) {
                ++match.mismatchData[longestMatchMismatches];
            }
        }
    }

    public static void resetSegmentCount() {
        segmentCount = 0;
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

    public Object clone() throws CloneNotSupportedException {
        QueriesAligner newAligner = (QueriesAligner) super.clone();
        IntSet newHits = new IntOpenHashSet();
        newAligner.matchPositions = newHits;
        return newAligner;
    }
}
