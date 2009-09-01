/*
 * Copyright (c) 2007 Virginia Commonwealth University. All rights reserved.
 * 
 * $Log: ElandOutput.java,v $
 * Revision 1.5  2009/10/19 17:37:03  hugh
 * Revised.
 *
 * Revision 1.4  2009-03-31 15:47:28  hugh
 * Updated for 0.2 release
 *
 * Revision 1.3  2008-08-13 19:08:46  hugh
 * Updated.
 *
 * Revision 1.2  2008-07-01 15:59:21  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

/**
 * Outputs the results of the mapping in Eland format
 * 
 * @author hugh
 * 
 */
public class ElandOutput {
    public static final int OUTPUT_BUFFER_SIZE = 65536 * 4;

    public static void outputResults(Match[] resultsByQueryPos,
            char[][] mismatchCounts, InputFile queriesFile,
            List<InputFile> referenceFiles) {
        TimerEvent.EVENT_WRITE_OUTPUT.start();

        StringBuilder outputLine = new StringBuilder();

        Writer ps;
        try {
            ps = new BufferedWriter(new FileWriter(
                    ProgramParameters.outputFileName));

            char charBuf[] = new char[OUTPUT_BUFFER_SIZE];

            int totalMatches = 0;
            int totalUniqueMatches = 0;
            long totalBases = 0;

            // for (int queryPosition = queriesFile.segmentStart[0];
            // queryPosition < queriesFile.segmentEnd[0]; queryPosition +=
            // ProgramParameters.queryLength) {
            int numQueries = queriesFile.basesSize
                    / ProgramParameters.queryLength;

            for (int queryNum = 1; queryNum < numQueries - 1; ++queryNum) {
                outputLine.setLength(0);

                // int queryNum = queryPosition / ProgramParameters.queryLength;
                Match match = resultsByQueryPos[queryNum];

                try {
                    if (queriesFile.headers.length == 0) {
                        outputLine
                                .append(queriesFile.fileName + ":" + queryNum);
                    } else {
                        int len = queriesFile.headerStart[queryNum]
                                - queriesFile.headerStart[queryNum - 1];
                        outputLine.append(OutputUtil.bytesToChars(
                                queriesFile.headers, charBuf,
                                queriesFile.headerStart[queryNum - 1], len), 0,
                                len);
                    }
                } catch (Throwable t) {
                    System.out.println(t);
                }

                outputLine.append("\t");

                if (match == null) {
                    // TODO temporary addition of stats columns
                    // outputLine.append("0\t0\t0\t0\t0\t");

                    outputLine.append(OutputUtil.bytesToChars(
                            queriesFile.bases, charBuf, queryNum
                                    * ProgramParameters.queryLength,
                            ProgramParameters.queryLength), 0,
                            ProgramParameters.queryLength);
                    outputLine.append("\t");
                    outputLine.append("NM");
                } else {
                    // TODO temporary addition of stats columns
                    // outputLine.append(match.startOffset);
                    // outputLine.append("\t");
                    // outputLine.append(match.startOffset + match.length);
                    // outputLine.append("\t");
                    // outputLine.append(match.length);
                    // outputLine.append("\t");
                    // outputLine.append(match.numMismatches);
                    // outputLine.append("\t");
                    // outputLine.append(match.matchCount);
                    // outputLine.append("\t");

                    ++totalMatches;

                    if (mismatchCounts[match.numMismatches][queryNum] == 1) {
                        ++totalUniqueMatches;
                        totalBases += match.length;

                        int len = match.length;
                        outputLine.append(OutputUtil.bytesToChars(
                                queriesFile.bases, charBuf, queryNum
                                        * ProgramParameters.queryLength
                                        + match.startOffset, len), 0, len);
                        outputLine.append("\t");

                        outputLine.append("U");
                        outputLine.append(match.numMismatches);
                        outputLine.append("\t");
                    } else {
                        outputLine.append(OutputUtil.bytesToChars(
                                queriesFile.bases, charBuf, queryNum
                                        * ProgramParameters.queryLength,
                                ProgramParameters.queryLength), 0,
                                ProgramParameters.queryLength);
                        outputLine.append("\t");

                        if (mismatchCounts[match.numMismatches][queryNum] > 0) {
                            outputLine.append("R");
                            outputLine.append(match.numMismatches);
                            outputLine.append("\t");
                        }

                    }

                    for (int i = 0; i <= ProgramParameters.maxMismatches
                            && i <= 2; ++i) {
                        outputLine.append((int) mismatchCounts[i][queryNum]);
                        outputLine.append("\t");
                    }

                    for (int i = ProgramParameters.maxMismatches + 1; i <= 2; ++i) {
                        outputLine.append("0\t");
                    }

                    if (mismatchCounts[match.numMismatches][queryNum] == 1) {
                        int forwardPosition = 0;
                        boolean reverse = false;
                        InputFile referenceFile = referenceFiles
                                .get(match.referenceFile);

                        int segmentNum = Arrays.binarySearch(
                                referenceFile.segmentEnd,
                                match.referencePosition);
                        if (segmentNum < 0) {
                            segmentNum = (-segmentNum) - 1;
                        }

                        // while (match.referencePosition >
                        // referenceFile.segmentEnd[segmentNum])
                        // ++segmentNum

						if (!ProgramParameters.forwardStrandOnly
								&& (segmentNum % 2 == 1)) {
                            reverse = true;
                        }

                        if (reverse) {
                            forwardPosition = referenceFile.segmentEnd[segmentNum]
                                    + match.startOffset
                                    - match.referencePosition
                                    - ProgramParameters.queryLength + 1;
                            reverse = true;
                        } else {
                            forwardPosition = match.referencePosition
                                    + match.startOffset
                                    - referenceFile.segmentStart[segmentNum]
                                    + 1;
                        }

                        if (referenceFile.headers.length == 0) {
                            outputLine.append(referenceFile.fileName);
                        } else {
							int headerNum;
							if (ProgramParameters.forwardStrandOnly) {
								headerNum = segmentNum;
							} else {
								headerNum = segmentNum / 2;
							}
                            int len = referenceFile.headerStart[headerNum + 1]
                                    - referenceFile.headerStart[headerNum];
                            outputLine.append(OutputUtil.bytesToChars(
                                    referenceFile.headers, charBuf,
                                    referenceFile.headerStart[headerNum], len),
                                    0, len);
                        }

                        outputLine.append("\t");
                        outputLine.append(forwardPosition);
                        outputLine.append("\t");
                        outputLine.append(reverse ? "R" : "F");
                        outputLine.append("\t.\t");

                        for (int i = 0; i < match.numMismatches; ++i) {
                            outputLine.append(match.mismatchData[i * 2]
                                    - match.startOffset);
                            outputLine
                                    .append((char) match.mismatchData[i * 2 + 1]);
                            outputLine.append("\t");
                        }

                        for (int i = match.numMismatches; i < 2; ++i) {
                            outputLine.append("\t");
                        }
                    }
                }
                outputLine.append("\n");

                outputLine.getChars(0, outputLine.length(), charBuf, 0);
                ps.write(charBuf, 0, outputLine.length());
            }

            System.out.println("Total matches = " + totalMatches);
            System.out.println("Total unique matches = " + totalUniqueMatches);
            System.out.println("Total bases from unique matches = "
                    + totalBases);

            ps.flush();
            ps.close();
        } catch (IOException e) {
            throw new SearchException(e);
        }

        TimerEvent.EVENT_WRITE_OUTPUT.stop();
    }

}
