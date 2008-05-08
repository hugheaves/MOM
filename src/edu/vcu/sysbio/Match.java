/*
 * $Log: Match.java,v $
 * Revision 1.1  2008/05/08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

public 	class Match {
	int genomeFile;
	int genomePosition;
	int queryPosition;
	int startOffset;
	int endOffset;
	int numMismatches;
	byte[] mismatches;

	public Match(int genomeFile, int genomePosition, int queryPosition,
			int startOffset, int endOffset, int numMismatches, byte[] mismatches) {
		this.genomeFile = genomeFile;
		this.genomePosition = genomePosition;
		this.queryPosition = queryPosition;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.numMismatches = numMismatches;
		this.mismatches = mismatches;
	}

	public int hashCode() {
		return genomePosition ^ queryPosition ^ genomeFile;
	}

	public boolean equals(Object o) {
		if (((Match) o).genomePosition == genomePosition
				&& ((Match) o).queryPosition == queryPosition
				&& ((Match) o).genomeFile == genomeFile)
			return true;
		return false;
	}
}
