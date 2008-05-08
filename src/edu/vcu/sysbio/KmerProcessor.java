/* 
 * $Log: KmerProcessor.java,v $
 * Revision 1.1  2008/05/08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

public interface KmerProcessor {
	public void processKmer(long kmer, int position);
}
