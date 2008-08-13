/*
 * $Log: Match.java,v $
 * Revision 1.3  2008/08/13 19:08:46  hugh
 * Updated.
 *
 * Revision 1.2  2008-07-01 15:59:22  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class Match {
	int referenceFile;
	int referencePosition;
	int queryPosition;
	int startOffset;
	int numMismatches;
	byte[] mismatches;
	int length;
    int matchCount;
    
	public Match() {
	}

	public void setValues(int referenceFile, int referencePosition,
			int queryPosition, int startOffset, int length, int numMismatches) {
		this.referenceFile = referenceFile;
		this.referencePosition = referencePosition;
		this.queryPosition = queryPosition;
		this.startOffset = startOffset;
		this.length = length;
		this.numMismatches = numMismatches;
		this.matchCount = 1;
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
