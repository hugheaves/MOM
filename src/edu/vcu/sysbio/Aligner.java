/*
 * $Log: Aligner.java,v $
 * Revision 1.1  2008/05/08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.HashSet;
import java.util.Set;

public class Aligner {
	protected final int mismatchOffsets[];
	protected final int queryLength;
	protected final int minMatchLength;
	protected final int maxMismatches;
	protected final byte[] genome;
	protected final byte[] queries;
	protected final byte[] matchCounts;
	protected final int fileNum;
	// protected final Int2ObjectMap<ObjectSet<Match>> results;
	// protected final Set<Match> results;
	protected final ObjectSet<Match> results;

	public Aligner(byte[] genome, byte[] queries, int queryLength,
			int minMatchLength, int maxMismatches, int fileNum,
			ObjectSet<Match> results, byte[] matchCounts) {
		this.mismatchOffsets = new int[queryLength];
		this.queryLength = queryLength;
		this.minMatchLength = minMatchLength;
		this.maxMismatches = maxMismatches;
		this.genome = genome;
		this.queries = queries;
		this.fileNum = fileNum;
		this.results = results;
		this.matchCounts = matchCounts;
	}

	/**
	 * Find the longest matching substring between the genome and query with "k"
	 * mismatches and "q" minimum match size
	 * 
	 * @param genomePosition
	 * @param queryPosition
	 * @param forwardDirection
	 */
	public final void doAlignment(int genomePosition, int queryPosition) {
		int compareOffset = -(queryPosition % queryLength);

		int startPos = compareOffset;
		int endPos = compareOffset;
		int finalPos = compareOffset + queryLength;

		int longestMatchLength = 0;
		int longestMatchPosition = startPos;
		int longestMatchMismatches = 0;
		int longestMatchMismatchesOffset = 0;
		int currentNumMismatches = 0;
		int totalMismatches = 0;
		int maxTotalMismatches = queryLength - minMatchLength + maxMismatches;

		if (matchCounts[queryPosition / queryLength] >= 2) {
			return;
		}

		// "caterpillar" through the string to find the largest matching
		// substring with k mismatches
		while (endPos < finalPos) {

			if (genome[genomePosition + endPos] != queries[queryPosition
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
			if ((endPos - startPos) > longestMatchLength
					|| ((endPos - startPos) == longestMatchLength && currentNumMismatches < longestMatchMismatches)) {
				longestMatchLength = endPos - startPos;
				longestMatchMismatches = currentNumMismatches;
				longestMatchPosition = startPos;
				longestMatchMismatchesOffset = totalMismatches;
			}

		}

		if (longestMatchLength >= minMatchLength) {
			int queryPos = queryPosition + compareOffset;

			// ObjectSet<Match> matchSet;
			// synchronized (results) {
			// matchSet = results.get(queryPos);
			// if (matchSet == null) {
			// matchSet = new ObjectArraySet<Match>(2);
			// results.put(queryPos, matchSet);
			// }
			// }
			//
			// if (matchSet.size() >= 2) {
			// return;
			// }

			Match match = new Match(fileNum, genomePosition + compareOffset,
					queryPosition + compareOffset, longestMatchPosition
							- compareOffset, longestMatchPosition
							- compareOffset + longestMatchLength - 1,
					longestMatchMismatches, null);

			if (results.add(match)) {
				++matchCounts[queryPosition / queryLength];
				// boolean added;
				// synchronized (matchSet) {
				// added = matchSet.add(match);
				// }
				// if (added && longestMatchMismatches > 0) {
				if (longestMatchMismatches > 0) {

					byte[] mismatches = new byte[longestMatchMismatches * 2];
					int j = 0;
					for (int i = 0; i < longestMatchMismatches; ++i) {
						int offset = mismatchOffsets[longestMatchMismatchesOffset
								- longestMatchMismatches + i];
						mismatches[j++] = (byte) (offset - compareOffset + 1);
						mismatches[j++] = genome[genomePosition + offset];
					}

					match.mismatches = mismatches;
				}
			}

		}

	}
}
