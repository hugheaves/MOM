package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/*
 * @author hugh
 * 
 *         Output Format:
 * 
 *         1. Match Status -
 *         N - No Match - 
 *         U - Unique Match, In Range -
 *         M - Multiple Matches, In Range -
 *         E - Unique Match, Outside Range - 
 *         X - Multiple Matches, Outside Range 
 *         
 *         2. Distance between reads (999,999,999 for different regions) 
 *
 *         3. Read 1 - Read Id
 *         4. Read 1 - Total number of matches against this read
 *         5. Read 1 - Matching sequence (for a single match)
 *         6. Read 1 - Read start match position (for a single match)
 *         7. Read 1 - Read match length (for a single match)
 *         8. Read 1 - SNP's  (for a single match)
 *         9. Read 1 - Match Position on Reference
 *         10. Read 1 - Forward or reverse direction (F/R)
 *         11. Read 1 - Reference Sequence Id
 *         
 *         12. Read 2 - Read Id
 *         13. Read 2 - Total number of matches against this read
 *         14. Read 2 - Matching sequence (for a single match)
 *         15. Read 2 - Read start match position (for a single match)
 *         16. Read 2 - Read match length (for a single match)
 *         17. Read 2 - SNP's  (for a single match)
 *         18. Read 2 - Match Position on Reference
 *         19. Read 2 - Forward or reverse direction (F/R)
 *         20. Read 2 - Reference Sequence Id
 *         
 *
 */
public class PairedOutput {
    static char[] charBuf = new char[32768];
    static StringBuilder outputBuffer = new StringBuilder();

    public static Comparator<MatchWrapper> comparator = new Comparator<MatchWrapper>() {

        public int compare(MatchWrapper a, MatchWrapper b) {
            if (a.match.referenceFile < b.match.referenceFile) {
                return -1;
            } else if (a.match.referenceFile > b.match.referenceFile) {
                return 1;
            } else if (a.segmentNum < b.segmentNum) {
                return -1;
            } else if (a.segmentNum > b.segmentNum) {
                return 1;
            } else if (a.position < b.position) {
                return -1;
            } else if (a.position > b.position) {
                return 1;
            } else {
                return 0;
            }
        }
    };

    public static class MatchWrapper {
        public LinkedMatch match;
        public int segmentNum;
        public int position;
        boolean forward;

        public MatchWrapper(LinkedMatch match, int segmentNum, int position,
                boolean forward) {
            this.match = match;
            this.segmentNum = segmentNum;
            this.position = position;
            this.forward = forward;
        }

        public void setAll(LinkedMatch match, int segmentNum, int position,
                boolean forward) {
            this.match = match;
            this.segmentNum = segmentNum;
            this.position = position;
            this.forward = forward;
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("referenceFile = " + match.referenceFile + ", ");
            buffer.append("segmentNum = " + segmentNum + ", ");
            buffer.append("position = " + position);
            buffer.append("forward = " + forward);
            return buffer.toString();
        }

    }

    public static int distance(MatchWrapper a, MatchWrapper b) {
        if (a.match.referenceFile < b.match.referenceFile) {
            return Integer.MIN_VALUE + 1;
        } else if (a.match.referenceFile > b.match.referenceFile) {
            return Integer.MAX_VALUE;
        } else if (a.segmentNum < b.segmentNum) {
            return Integer.MIN_VALUE + 1;
        } else if (a.segmentNum > b.segmentNum) {
            return Integer.MAX_VALUE;
        } else {
            return a.position - b.position;
        }
    }

    public static void processResults(LinkedMatch[][] results,
            char[][][] mismatchCounts, InputFile queriesFiles[],
            List<InputFile> referenceFiles) {

        BufferedWriter ps;
        try {
            ps = new BufferedWriter(new FileWriter(
                    ProgramParameters.outputFileName));

            ObjectArrayList<MatchWrapper> listA = ObjectArrayList
                    .wrap(new MatchWrapper[0]);
            ObjectArrayList<MatchWrapper> listB = ObjectArrayList
                    .wrap(new MatchWrapper[0]);

            int numQueries = queriesFiles[0].basesSize
                    / ProgramParameters.queryLength;
            for (int queryNum = 1; queryNum < numQueries - 1; ++queryNum) {
                int inRange = 0;
                int outOfRange = 0;
                int matchNumA = 0;
                int matchNumB = 0;
                int matchDistance = 0;

                loadMatches(listA, referenceFiles, results[0][queryNum]);
                loadMatches(listB, referenceFiles, results[1][queryNum]);

                int pos = 0;

                // System.out.println("a.size() = " + listA.size()
                // + ", b.size() = " + listB.size());

                for (int i = 0; i < listA.size(); ++i) {
                    for (int j = pos; j < listB.size(); ++j) {
                        // System.out.println("a[" + i + "] = " + listA.get(i));
                        // System.out.println("b[" + j + "] = " + listB.get(j));
                        int distance = distance(listA.get(i), listB.get(j));
                        if (Math.abs(distance) <= ProgramParameters.pairedReadMaxGap
                                && Math.abs(distance) >= ProgramParameters.pairedReadMinGap) {
                            // we have a match :)
                            ++inRange;
                            matchNumA = i;
                            matchNumB = j;
                            matchDistance = distance;
                        } else {
                            // we don't have a match :(
                            if (distance > 0) {
                                pos = j + 1;
                            }
                            ++outOfRange;
                            if (inRange == 0) {
                                matchNumA = i;
                                matchNumB = j;
                                matchDistance = distance;
                            }
                            if (distance < 0) {
                                break;
                            }
                        }
                    }
                }

                // System.out.println("inRange = " + inRange + ", outOfRange = "
                // + outOfRange + ", queryNumA = " + queryNumA
                // + ", queryNumB = " + queryNumB);

                if (listA.size() == 0 || listB.size() == 0) {
                    // one or the other has no match, so we have no paired match
                    outputMatches(ps, "N", 0, referenceFiles, queriesFiles,
                            queryNum, matchNumA, listA, matchNumB, listB, false);
                } else if (inRange == 1) {
                    // we have one unique paired match
                    outputMatches(ps, "U", matchDistance, referenceFiles,
                            queriesFiles, queryNum, matchNumA, listA,
                            matchNumB, listB, true);
                } else if (inRange > 1) {
                    // we have more than one paired match
                    outputMatches(ps, "M", 0, referenceFiles, queriesFiles,
                            queryNum, matchNumA, listA, matchNumB, listB, false);
                } else if (inRange == 0 && outOfRange == 1 && listA.size() == 1
                        && listB.size() == 1) {
                    // we have a unique "out of range" match
                    outputMatches(ps, "E", matchDistance, referenceFiles,
                            queriesFiles, queryNum, matchNumA, listA,
                            matchNumB, listB, true);
                } else {
                    outputMatches(ps, "X", 0, referenceFiles, queriesFiles,
                            queryNum, matchNumA, listA, matchNumB, listB, false);
                }

            }

            ps.close();

        } catch (IOException e) {
            throw new SearchException(e);
        }
    }

    private static void outputMatches(BufferedWriter output, String status,
            int distance, List<InputFile> referenceFiles,
            InputFile[] queriesFiles, int queryNum, int matchNumA,
            ObjectArrayList<MatchWrapper> listA, int matchNumB,
            ObjectArrayList<MatchWrapper> listB, boolean uniqueMatch)
            throws IOException {
        outputBuffer.setLength(0);
        outputBuffer.append(status);
        outputBuffer.append("\t");
        outputBuffer.append(Integer.toString(distance));
        outputBuffer.append("\t");
        if (listA.size() == 1 || uniqueMatch) {
            outputMatch(referenceFiles, queriesFiles[0], queryNum, matchNumA,
                    listA);

        } else {
            outputNoMatch(referenceFiles, queriesFiles[0], queryNum, matchNumA,
                    listA);
        }
        outputBuffer.append("\t");
        if (listB.size() == 1 || uniqueMatch) {
            outputMatch(referenceFiles, queriesFiles[1], queryNum, matchNumB,
                    listB);

        } else {
            outputNoMatch(referenceFiles, queriesFiles[1], queryNum, matchNumB,
                    listB);
        }
        outputBuffer.append("\n");
        output.append(outputBuffer);

    }

    private static void outputMatch(List<InputFile> referenceFiles,
            InputFile queriesFile, int queryNum, int matchNum,
            ObjectArrayList<MatchWrapper> list) throws IOException {
        MatchWrapper wrapper = list.get(matchNum);
        LinkedMatch match = wrapper.match;
        int queryIdLen = OutputUtil.getQueryId(queriesFile, charBuf, queryNum);
        outputBuffer.append(charBuf, 0, queryIdLen);
        outputBuffer.append("\t");
        outputBuffer.append(list.size());
        outputBuffer.append("\t");
        outputBuffer.append(OutputUtil.bytesToChars(queriesFile.bases, charBuf,
                queryNum * ProgramParameters.queryLength + match.startOffset,
                match.length), 0, match.length);
        outputBuffer.append("\t");
        outputBuffer.append(match.startOffset);
        outputBuffer.append("\t");
        outputBuffer.append(match.length);
        outputBuffer.append("\t");
        for (int i = 0; i < match.numMismatches; ++i) {
            outputBuffer.append((int) match.mismatchData[i * 2]
                    - match.startOffset);
            outputBuffer.append((char) match.mismatchData[i * 2 + 1]);
            outputBuffer.append(" ");
        }
        outputBuffer.append("\t");
        outputBuffer.append(wrapper.position);
        outputBuffer.append("\t");
        outputBuffer.append(wrapper.forward ? "F" : "R");
        outputBuffer.append("\t");
        InputFile referenceFile = referenceFiles.get(match.referenceFile);
        if (referenceFile.headers.length == 0) {
            outputBuffer.append(referenceFile.fileName);
        } else {
            int len = referenceFile.headerStart[wrapper.segmentNum + 1]
                    - referenceFile.headerStart[wrapper.segmentNum];
            outputBuffer
                    .append(
                            OutputUtil
                                    .bytesToChars(
                                            referenceFile.headers,
                                            charBuf,
                                            referenceFile.headerStart[wrapper.segmentNum],
                                            len), 0, len);
        }
    }

    private static void outputNoMatch(List<InputFile> referenceFiles,
            InputFile queriesFile, int queryNum, int matchNum,
            ObjectArrayList<MatchWrapper> list) {
        int queryIdLen = OutputUtil.getQueryId(queriesFile, charBuf, queryNum);
        outputBuffer.append(charBuf, 0, queryIdLen);
        outputBuffer.append("\t");
        outputBuffer.append(list.size());
        outputBuffer.append("\t");
        outputBuffer.append(" ");
        outputBuffer.append("\t");
        outputBuffer.append("0");
        outputBuffer.append("\t");
        outputBuffer.append("0");
        outputBuffer.append("\t");
        outputBuffer.append(" ");
        outputBuffer.append("\t");
        outputBuffer.append("0");
        outputBuffer.append("\t");
        outputBuffer.append("F");
        outputBuffer.append("\t");
        outputBuffer.append(" ");
    }

    private static void loadMatches(ObjectArrayList<MatchWrapper> matches,
            List<InputFile> referenceFiles, LinkedMatch linkedMatch) {

        matches.clear();

        while (linkedMatch != null) {
            InputFile referenceFile = referenceFiles
                    .get(linkedMatch.referenceFile);
            int segmentNum = Arrays.binarySearch(referenceFile.segmentEnd,
                    linkedMatch.referencePosition);
            if (segmentNum < 0) {
                segmentNum = -segmentNum - 1;
            }

            int forwardPosition = 0;
            boolean forward = false;
            if (segmentNum % 2 == 0) {
                forward = true;
                forwardPosition = referenceFile.segmentEnd[segmentNum]
                        + linkedMatch.startOffset
                        - linkedMatch.referencePosition
                        - ProgramParameters.queryLength + 1;
            } else {
                forwardPosition = linkedMatch.referencePosition
                        + linkedMatch.startOffset
                        - referenceFile.segmentStart[segmentNum] + 1;
            }

            matches.add(new MatchWrapper(linkedMatch, segmentNum / 2,
                    forwardPosition, forward));

            linkedMatch = linkedMatch.nextMatch;
        }

        Arrays.sort(matches.elements(), 0, matches.size(), comparator);
    }
}
