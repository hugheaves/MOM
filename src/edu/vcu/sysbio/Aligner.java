/*
 * $Log: Aligner.java,v $
 * Revision 1.2  2008/06/25 19:05:56  hugh
 * Performance optimizations
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.objects.ObjectSet;

public class Aligner {
	protected int mismatchOffsets[];
	// protected final int queryLength;
	// protected final int maxMismatches;
	protected final byte[] reference;
	protected final byte[] queries;
	protected final byte[] matchCounts;
	protected final int fileNum;
	protected final ObjectSet<Match> results;
	protected final int[] alreadyCompared;

	public Aligner(byte[] reference, byte[] queries, int fileNum,
			ObjectSet<Match> results, byte[] matchCounts, int[] alreadyCompared) {
		this.mismatchOffsets = new int[ProgramParameters.queryLength];
		// this.queryLength = ProgramParameters.queryLength;
		// this.maxMismatches = ProgramParameters.maxMismatches;
		this.reference = reference;
		this.queries = queries;
		this.fileNum = fileNum;
		this.results = results;
		this.matchCounts = matchCounts;
		this.alreadyCompared = alreadyCompared;
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

		if ((ProgramParameters.maxMatchesPerQuery != -1)
				&& (matchCounts[queryNum] >= ProgramParameters.maxMatchesPerQuery)) {
			return;
		}

		final int compareOffset = -(queryPosition % ProgramParameters.queryLength);

		if (alreadyCompared != null) {
			if (alreadyCompared[referencePosition + compareOffset] == queryNum) {
				return;
			} else {
				alreadyCompared[referencePosition + compareOffset] = queryNum;
			}
		}

		int startPos = compareOffset;
		int endPos = compareOffset;
		final int finalPos = compareOffset + ProgramParameters.queryLength;

		int longestMatchLength = 0;
		int longestMatchPosition = startPos;
		int longestMatchMismatches = 0;
		int longestMatchMismatchesOffset = 0;
		int currentNumMismatches = 0;
		int totalMismatches = 0;
		final int maxTotalMismatches = ProgramParameters.queryLength
				- ProgramParameters.minMatchLength
				+ ProgramParameters.maxMismatches;

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

				if (currentNumMismatches > ProgramParameters.maxMismatches) {
					// if we're "maxed out" on mismatches, we "eat" a mismatch
					// from the beginning
					startPos = mismatchOffsets[totalMismatches
							- ProgramParameters.maxMismatches - 1] + 1;
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
			Match match = new Match(fileNum, referencePosition + compareOffset,
					queryPosition + compareOffset, longestMatchPosition
							- compareOffset, longestMatchPosition
							- compareOffset + longestMatchLength - 1,
					longestMatchMismatches, null);

			if (results.add(match)) {
				++matchCounts[queryPosition / ProgramParameters.queryLength];
				if (longestMatchMismatches > 0) {

					byte[] mismatches = new byte[longestMatchMismatches * 2];
					int j = 0;
					for (int i = 0; i < longestMatchMismatches; ++i) {
						int offset = mismatchOffsets[longestMatchMismatchesOffset
								- longestMatchMismatches + i];
						mismatches[j++] = (byte) (offset - compareOffset + 1);
						mismatches[j++] = reference[referencePosition + offset];
					}

					match.mismatches = mismatches;
				}
			}

		}
	}

	public Object clone() throws CloneNotSupportedException {
		Aligner newAligner = (Aligner) super.clone();
		newAligner.mismatchOffsets = new int[ProgramParameters.queryLength];
		return newAligner;
	}
}
