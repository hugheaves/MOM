/*
 * 
 * $Log: KmerPositionList.java,v $
 * Revision 1.3  2009/03/31 15:47:28  hugh
 * Updated for 0.2 release
 *
 * Revision 1.2  2008-07-01 15:59:21  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 * Revision 1.1  2008-03-05 23:15:21  hugh
 * Updated.
 *
 */
package edu.vcu.sysbio;

import java.util.Arrays;

/**
 * Stores a list of kmer positions.
 * 
 * @author hugh
 * 
 */
class KmerPositionList {
    // array storing positions
    int[] positions;
    // number of valid positions in the array
    int positionCount;

    KmerPositionList() {
        positionCount = 0;
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
