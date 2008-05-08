/*
 * Copyright (c) 2007 Virginia Commonwealth University. All rights reserved.
 * 
 * $Log: ElandOutput.java,v $
 * Revision 1.1  2008/05/08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ElandOutput {
	public static void processResults(ObjectSet<Match> results,
			InputFile queriesFile, List<InputFile> genomeFiles,
			int maxMismatches, int queryLength, String outputFileName)
			throws SearchException {

		// index all results by query location
		TimerEvent.EVENT_PROCESS_RESULTS.start();
		Int2ObjectMap<ObjectSet<Match>> resultsByQueryPos = new Int2ObjectOpenHashMap<ObjectSet<Match>>();
		for (Iterator<Match> i = results.iterator(); i.hasNext();) {
			Match currentMatch = i.next();
			ObjectSet<Match> matches = resultsByQueryPos
					.get(currentMatch.queryPosition);
			if (matches == null) {
				matches = new ObjectArraySet();
				resultsByQueryPos.put(currentMatch.queryPosition, matches);
			}
			matches.add(currentMatch);
		}
		TimerEvent.EVENT_PROCESS_RESULTS.stop();

		TimerEvent.EVENT_WRITE_OUTPUT.start();
		outputResults(resultsByQueryPos, queriesFile, genomeFiles,
				maxMismatches, queryLength, outputFileName);
		TimerEvent.EVENT_WRITE_OUTPUT.stop();
	}

	private static void outputResults(
			Int2ObjectMap<ObjectSet<Match>> resultsByQueryPos,
			InputFile queriesFile, List<InputFile> genomeFiles,
			int maxMismatches, int queryLength, String outputFileName)
			throws SearchException {
		StringBuffer outputLine = new StringBuffer();

		PrintStream ps;
		try {
			ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(
					outputFileName)));
		} catch (FileNotFoundException e) {
			throw new SearchException(e);
		}

		int matchCounts[] = new int[maxMismatches <= 2 ? 3 : maxMismatches + 1];

		for (int queryPosition = queriesFile.dataOffsets[0]
				+ queriesFile.queryLength; queryPosition < queriesFile.dataOffsets[1]; queryPosition += queryLength) {

//			if (queryPosition % (queriesFile.dataOffsets[1] / 99) == 0) {
//				System.out
//						.println("writing output file - "
//								+ ((long) queryPosition * 100 / queriesFile.dataOffsets[1])
//								+ "% done");
//			}

			outputLine.setLength(0);

			int queryNum = queryPosition / queriesFile.queryLength;
			Set<Match> matches = resultsByQueryPos.get(queryPosition);
			Match match = null;

			if (queriesFile.headers.length == 0) {
				outputLine.append(queriesFile.fileName + ":" + queryNum);
			} else {
				outputLine.append(new String(queriesFile.headers,
						queriesFile.headerOffsets[queryNum - 1],
						queriesFile.headerOffsets[queryNum]
								- queriesFile.headerOffsets[queryNum - 1]));
			}

			outputLine.append("\t");

			if (matches == null || matches.size() > 1) {
				outputLine.append(new String(queriesFile.data, queryPosition,
						queryLength));
				outputLine.append("\t");
			} 


			if (matches == null) {
				outputLine.append("NM");
			} else {
				Arrays.fill(matchCounts, 0);
				for (Iterator<Match> i = matches.iterator(); i.hasNext();) {
					match = i.next();
					matchCounts[match.numMismatches]++;
				}

				if (matches.size() == 1) {
					outputLine.append(new String(queriesFile.data, queryPosition
							+ match.startOffset, match.endOffset
							- match.startOffset + 1));
					outputLine.append("\t");
					outputLine.append("U");
					outputLine.append(match.numMismatches);
					outputLine.append("\t");
				} else {
					for (int i = maxMismatches; i >= 0; --i) {
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

				if (matches.size() == 1) {
					int forwardPosition = 0;
					boolean reverse = false;
					InputFile genomeFile = genomeFiles.get(match.genomeFile);

					// System.out.println (new String(genomeFile.data));
					// System.out.println (new String(queriesFile.data));
					// System.out.println (new String(queriesFile.data,
					// match.queryPosition, queriesFile.queryLength));

					int segmentNum = 0;
					while (match.genomePosition > genomeFile.dataOffsets[segmentNum + 1])
						++segmentNum;

					if (segmentNum % 2 == 1) {
						reverse = true;
					}

					if (reverse) {
						forwardPosition = genomeFile.dataOffsets[segmentNum + 1]
								+ match.startOffset
								- match.genomePosition
								- queriesFile.queryLength + 1;
						reverse = true;
					} else {
						forwardPosition = match.genomePosition
								+ match.startOffset
								- genomeFile.dataOffsets[segmentNum]
								- queriesFile.queryLength + 1;
					}

					if (genomeFile.headers.length == 0) {
						outputLine.append(genomeFile.fileName);
					} else {
						int headerNum = segmentNum / 2;
						outputLine.append(new String(genomeFile.headers,
								genomeFile.headerOffsets[headerNum],
								genomeFile.headerOffsets[headerNum + 1]
										- genomeFile.headerOffsets[headerNum]));
					}

					outputLine.append("\t");
					outputLine.append(forwardPosition);
					outputLine.append("\t");
					outputLine.append(reverse ? "R" : "F");
					outputLine.append("\t.\t");

					for (int i = 0; i < match.numMismatches; ++i) {
						outputLine.append(match.mismatches[i * 2]
								- match.startOffset);
						outputLine.append((char) match.mismatches[i * 2 + 1]);
						outputLine.append(" ");
					}
				}
			}
			ps.println(outputLine.toString());
		}

		ps.close();
	}
}
