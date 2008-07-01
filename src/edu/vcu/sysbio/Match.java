/*
 * $Log: Match.java,v $
 * Revision 1.2  2008/07/01 15:59:22  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

public 	class Match {
	int referenceFile;
	int referencePosition;
	int queryPosition;
	int startOffset;
	int endOffset;
	int numMismatches;
	byte[] mismatches;

	public Match(int referenceFile, int referencePosition, int queryPosition,
			int startOffset, int endOffset, int numMismatches, byte[] mismatches) {
		this.referenceFile = referenceFile;
		this.referencePosition = referencePosition;
		this.queryPosition = queryPosition;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.numMismatches = numMismatches;
		this.mismatches = mismatches;
	}

	public int hashCode() {
		return referencePosition ^ queryPosition ^ referenceFile;
	}

	public boolean equals(Object o) {
		if (((Match) o).referencePosition == referencePosition
				&& ((Match) o).queryPosition == queryPosition
				&& ((Match) o).referenceFile == referenceFile)
			return true;
		return false;
	}
}
