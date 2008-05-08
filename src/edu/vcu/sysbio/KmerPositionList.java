/*
 * 
 * $Log: KmerPositionList.java,v $
 * Revision 1.1  2008/05/08 18:50:08  hugh
 * Updated.
 *
 * Revision 1.1  2008-03-05 23:15:21  hugh
 * Updated.
 *
 */
package edu.vcu.sysbio;

import java.util.Arrays;

class KmerPositionList {
	KmerPositionList() {
		allocateAndClear();
	}

	int[] positions;
	int positionCount;

	void clear() {
		positionCount = 0;
	}

	void allocateAndClear() {
		clear();
		positions = new int[KmerIndex.INITIAL_ARRAY_SIZE];
	}

	void addPosition(int position) {
		if (positionCount >= positions.length) {
			positions = Arrays
					.copyOf(positions, (positions.length * 3) / 2 + 1);
		}

		positions[positionCount] = position;
		positionCount++;
	}
}