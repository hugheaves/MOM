/*
 * Copyright (c) 2007 Virginia Commonwealth University. All rights reserved.
 * 
 * $Log: ElandOutput.java,v $
 * Revision 1.3  2008/08/13 19:08:46  hugh
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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ElandOutput {
	public static void processResults(Match[] results, InputFile queriesFile,
			List<InputFile> referenceFiles) {

		// index all results by query location
		TimerEvent.EVENT_PROCESS_RESULTS.start();
		// Int2ObjectMap<ObjectSet<Match>> resultsByQueryPos = new
		// Int2ObjectOpenHashMap<ObjectSet<Match>>();
		// for (Iterator<Match> i = results.iterator(); i.hasNext();) {
		// Match currentMatch = i.next();
		// ObjectSet<Match> matches = resultsByQueryPos
		// .get(currentMatch.queryPosition);
		// if (matches == null) {
		// matches = new ObjectArraySet();
		// resultsByQueryPos.put(currentMatch.queryPosition, matches);
		// }
		// matches.add(currentMatch);
		// }
		TimerEvent.EVENT_PROCESS_RESULTS.stop();

		TimerEvent.EVENT_WRITE_OUTPUT.start();
		outputResults(results, queriesFile, referenceFiles);
		TimerEvent.EVENT_WRITE_OUTPUT.stop();
	}

	private static void outputResults(Match[] resultsByQueryPos,
			InputFile queriesFile, List<InputFile> referenceFiles) {
		StringBuilder outputLine = new StringBuilder();

		Writer ps;
		try {
			ps = new BufferedWriter(new FileWriter(
					ProgramParameters.outputFileName));

			char charBuf[] = new char[1024];

			int totalMatches = 0;
			int totalUniqueMatches = 0;
			long totalBases = 0;

			int matchCounts[] = new int[ProgramParameters.maxMismatches <= 2 ? 3
					: ProgramParameters.maxMismatches + 1];

			for (int queryPosition = queriesFile.dataOffsets[0]
					+ ProgramParameters.queryLength; queryPosition < queriesFile.dataOffsets[1]; queryPosition += ProgramParameters.queryLength) {

				outputLine.setLength(0);

				int queryNum = queryPosition / ProgramParameters.queryLength;
				Match match = resultsByQueryPos[queryNum];

				if (queriesFile.headers.length == 0) {
					outputLine.append(queriesFile.fileName + ":" + queryNum);
				} else {
					int len = queriesFile.headerOffsets[queryNum]
							- queriesFile.headerOffsets[queryNum - 1];
					outputLine.append(bytesToChars(queriesFile.headers,
							charBuf, queriesFile.headerOffsets[queryNum - 1],
							len), 0, len);
				}

				outputLine.append("\t");

				if (match == null) {
					// TODO temporary addition of stats columns
//					outputLine.append("0\t0\t0\t0\t0\t");

					outputLine.append(bytesToChars(queriesFile.data, charBuf,
							queryPosition, ProgramParameters.queryLength), 0,
							ProgramParameters.queryLength);
					outputLine.append("\t");
					outputLine.append("NM");
				} else {
					// TODO temporary addition of stats columns
//					outputLine.append(match.startOffset);
//					outputLine.append("\t");
//					outputLine.append(match.startOffset + match.length);
//					outputLine.append("\t");		
//					outputLine.append(match.length);
//					outputLine.append("\t");
//					outputLine.append(match.numMismatches);
//					outputLine.append("\t");
//					outputLine.append(match.matchCount);
//					outputLine.append("\t");

					++totalMatches;

					Arrays.fill(matchCounts, 0);

					matchCounts[match.numMismatches] = match.matchCount;

					if (match.matchCount == 1) {
						++totalUniqueMatches;
						totalBases += match.length;

						int len = match.length;
						outputLine
								.append(
										bytesToChars(queriesFile.data, charBuf,
												queryPosition
														+ match.startOffset,
												len), 0, len);
						outputLine.append("\t");

						outputLine.append("U");
						outputLine.append(match.numMismatches);
						outputLine.append("\t");
					} else {
						outputLine.append(bytesToChars(queriesFile.data,
								charBuf, queryPosition,
								ProgramParameters.queryLength), 0,
								ProgramParameters.queryLength);
						outputLine.append("\t");

						for (int i = ProgramParameters.maxMismatches; i >= 0; --i) {
							if (matchCounts[i] > 0) {
								outputLine.append("R");
								outputLine.append(i);
								outputLine.append("\t");
								break;
							}
						}
					}

					for (int i = 0; i <= 2; ++i) {
						outputLine.append(matchCounts[i]);
						outputLine.append("\t");
					}

					if (match.matchCount == 1) {
						int forwardPosition = 0;
						boolean reverse = false;
						InputFile referenceFile = referenceFiles
								.get(match.referenceFile);

						int segmentNum = 0;
						while (match.referencePosition > referenceFile.dataOffsets[segmentNum + 1])
							++segmentNum;

						if (segmentNum % 2 == 1) {
							reverse = true;
						}

						if (reverse) {
							forwardPosition = referenceFile.dataOffsets[segmentNum + 1]
									+ match.startOffset
									- match.referencePosition
									- ProgramParameters.queryLength + 1;
							reverse = true;
						} else {
							forwardPosition = match.referencePosition
									+ match.startOffset
									- referenceFile.dataOffsets[segmentNum]
									- ProgramParameters.queryLength + 1;
						}

						if (referenceFile.headers.length == 0) {
							outputLine.append(referenceFile.fileName);
						} else {
							int headerNum = segmentNum / 2;
							int len = referenceFile.headerOffsets[headerNum + 1]
									- referenceFile.headerOffsets[headerNum];
							outputLine
									.append(
											bytesToChars(
													referenceFile.headers,
													charBuf,
													referenceFile.headerOffsets[headerNum],
													len), 0, len);
						}

						outputLine.append("\t");
						outputLine.append(forwardPosition);
						outputLine.append("\t");
						outputLine.append(reverse ? "R" : "F");
						outputLine.append("\t.\t");

						for (int i = 0; i < match.numMismatches; ++i) {
							outputLine.append(match.mismatches[i * 2]
									- match.startOffset);
							outputLine
									.append((char) match.mismatches[i * 2 + 1]);
							outputLine.append(" ");
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

	}

	private static char[] bytesToChars(byte[] byteBuf, char charBuf[],
			int offset, int length) {
		int byteBufPos = offset;
		for (int i = 0; i < length; ++i) {
			charBuf[i] = (char) byteBuf[byteBufPos++];
		}
		return charBuf;
	}
}
