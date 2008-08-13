/*
 * $Log: Aligner.java,v $
 * Revision 1.4  2008/08/13 19:08:46  hugh
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

public class Aligner {
	private int mismatchOffsets[];
	// protected final int queryLength;
	private final int maxMismatches;
	protected final byte[] reference;
	protected final byte[] queries;
	private final int fileNum;
	private final Match[] results;
	private final int[] alreadyCompared;
	private final int maxTotalMismatches;
	private final LongSet hits;

	public Aligner(byte[] reference, byte[] queries, int fileNum,
			Match[] results, int[] alreadyCompared, LongSet hits) {
		this.mismatchOffsets = new int[ProgramParameters.queryLength];
		// this.queryLength = ProgramParameters.queryLength;
		this.maxMismatches = ProgramParameters.maxMismatches;
		this.reference = reference;
		this.queries = queries;
		this.fileNum = fileNum;
		this.results = results;
		this.alreadyCompared = alreadyCompared;
		this.maxTotalMismatches = ProgramParameters.queryLength
				- ProgramParameters.minMatchLength
				+ ProgramParameters.maxMismatches;
		this.hits = hits;
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

		if ((ProgramParameters.maxMatchesPerQuery != -1 && results[queryNum] != null)
				&& results[queryNum].matchCount >= ProgramParameters.maxMatchesPerQuery) {
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
			Match match = results[queryNum];

			if (match == null) {
				synchronized (results) {
					if (results[queryNum] == null) {
						match = results[queryNum] = new Match();
					} else {
						match = results[queryNum];
					}
				}
			}

			synchronized (match) {
				if (longestMatchLength > match.length
						|| longestMatchLength == match.length
						&& longestMatchMismatches < match.numMismatches) {
					match.setValues(fileNum, referencePosition + compareOffset,
							queryPosition + compareOffset, longestMatchPosition
									- compareOffset, longestMatchLength,
							longestMatchMismatches);

					if (longestMatchMismatches > 0) {
						if (match.mismatches == null) {
							match.mismatches = new byte[ProgramParameters.maxMismatches * 2];
						}
						int j = 0;
						for (int i = 0; i < longestMatchMismatches; ++i) {
							int offset = mismatchOffsets[longestMatchMismatchesOffset
									- longestMatchMismatches + i];
							match.mismatches[j++] = (byte) (offset
									- compareOffset + 1);
							match.mismatches[j++] = reference[referencePosition
									+ offset];
						}
					}
					long hit = ((long) referencePosition + (long) compareOffset) << 32;
					hit += queryPosition + compareOffset;
					hits.add(hit);
				} else if (longestMatchLength == match.length && longestMatchMismatches == match.numMismatches) {
					long hit = ((long) referencePosition + (long) compareOffset) << 32;
					hit += queryPosition + compareOffset;
					if (hits.add(hit)) {
						++match.matchCount;
					}
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
