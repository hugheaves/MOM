/* 
 * $Log: KmerProcessor.java,v $
 * Revision 1.3  2009/03/31 15:47:28  hugh
 * Updated for 0.2 release
 *
 * Revision 1.2  2008-07-01 15:59:21  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import java.util.concurrent.Callable;

public interface KmerProcessor extends Callable<Object>, Cloneable {
    public void processKmer(long kmer, int position);

    public void setStart(int start);

    public int getStart();

    public void setEnd(int end);

    public int getEnd();

    public Object clone() throws CloneNotSupportedException;
}
