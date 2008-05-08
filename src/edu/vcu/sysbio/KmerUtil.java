/*
 * $Log: KmerUtil.java,v $
 * Revision 1.1  2008/05/08 18:50:08  hugh
 * Updated.
 *
 *
 * Revision 1.3  2008-03-25 19:50:57  hugh
 * Merge w/ Phillips version
 *
 * Revision 1.2  2008-03-05 23:13:34  hugh
 * Changed to use long instead of int
 *
 * Revision 1.1  2008-02-29 16:50:31  hugh
 * Initial revision.
 *
 */

package edu.vcu.sysbio;

import java.util.logging.Logger;

/**
 * Various utility functions to compute bit-packed kmers from bytes
 * 
 * @author Hugh
 * @since Feb 19, 2008
 * @version $Revision: 1.1 $
 * 
 */
public class KmerUtil {
	private static Logger log = Logger.getLogger(KmerUtil.class.toString());

	/**
	 * Creates the bitmask used for creating kmers based on the kmer length
	 * 
	 * @param kmerLength
	 * @return
	 */
	public static final long computeMask(int kmerLength) {
		long mask = 0xffffffff;
		mask = mask >>> (64 - (kmerLength * 2));
		log.fine("mask = " + mask + ", kmerLength = " + kmerLength);
		return mask;
	}

	/**
	 * Adds one byte to an existing kmer
	 * 
	 * @param currentHash
	 * @param mask
	 * @param nextCharacter
	 * @return
	 */
	public static final long addByteToKmer(long currentKmer, long mask,
			byte nextCharacter) {
		currentKmer = currentKmer << 2; // shift our hash value
		currentKmer = currentKmer & mask; // clear the high order bits

		currentKmer += byteToBits(nextCharacter);

		return currentKmer;
	}

	/**
	 * Builts a kmer using bytes from genome[0] to genome[kmerLength - 2] (i.e.
	 * number of bytes used is one less than the size of the kmer)
	 * 
	 * @param genome
	 * @param mask
	 * @param startPos
	 * @return
	 */
	public static final long initializeKmer(byte[] genome, int kmerLength,
			int startPos) {
		long kmer = 0;
		for (int i = startPos; i < startPos + kmerLength - 1; ++i) {
			kmer = kmer << 2; // shift our hash value
			kmer += byteToBits(genome[i]);
		}
		return kmer;
	}

	public static final int byteToBits(byte b) {
		if (b == 'G') {
			return 1;
		} else if (b == 'C') {
			return 2;
		} else if (b == 'T') {
			return 3;
		}
		return 0;
	}

	public static final String kmerToString(long kmer, int kmerLength) {
		long mask = 3;
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < kmerLength; ++i) {
			long bits = kmer & mask;
			if (bits == 1) {
				buf.insert(0, 'G');
			} else if (bits == 2) {
				buf.insert(0, 'C');
			} else if (bits == 3) {
				buf.insert(0, 'T');
			} else {
				buf.insert(0, 'A');
			}
			kmer = kmer >> 2;
		}

		return buf.toString();
	}

	public static void generateQueryKmers(byte[] data,
			int start, int end, int kmerLength, int kmerInterval,
			int queryLength, String statusMessage, KmerProcessor kmerProcessor) {

		long mask = computeMask(kmerLength);
		byte ch;
		int currentLength = 0;
		long kmer = 0;
		int dataLength = end - start;

		for (int i = start; i < end; ++i) {
			if (i % (dataLength / 99) == 0) {
				System.out.println(statusMessage + " - "
						+ ((long) i * 100 / dataLength) + "% done");
			}

			ch = data[i];
			if (ch != InputFile.NOMATCH_CHAR && ch != InputFile.WILDCARD_CHAR) {
				++currentLength;
				kmer = addByteToKmer(kmer, mask, data[i]);
				if (((currentLength % kmerInterval == 0) || (currentLength
						% queryLength == 0))
						&& currentLength >= kmerLength) {
					kmerProcessor.processKmer(kmer, i - kmerLength + 1);
					if (currentLength >= queryLength) {
						currentLength = 0;
					}
				}
			} else {
				if (((currentLength % kmerInterval != 0) && (currentLength
						% queryLength == 0))
						&& currentLength >= kmerLength) {
					kmerProcessor.processKmer(kmer, i - kmerLength + 1);
				}
				currentLength = 0;
			}
		}
		if (((currentLength % kmerInterval != 0) && (currentLength
				% queryLength == 0))
				&& currentLength >= kmerLength) {
			kmerProcessor.processKmer(kmer, end - kmerLength + 1);
		}
	}

	public static void generateGenomeKmers(byte[] data,
			int start, int end, int kmerLength, int kmerInterval, KmerProcessor kmerProcessor) {

		long mask = computeMask(kmerLength);
		byte ch;
		int currentLength = 0;
		long kmer = 0;
		int dataLength = end - start;

		for (int i = start; i < end; ++i) {
//			if (i % (dataLength / 99) == 0) {
//				System.out.println(
//						(((long) i * 100 / dataLength)) + "% done");
//			}

			ch = data[i];
			if (ch != InputFile.NOMATCH_CHAR && ch != InputFile.WILDCARD_CHAR) {
				++currentLength;
				kmer = addByteToKmer(kmer, mask, data[i]);
				if ((currentLength % kmerInterval == 0) && currentLength >= kmerLength) {
					kmerProcessor.processKmer(kmer, i - kmerLength + 1);
				}
			} else {
				if (currentLength % kmerInterval != 0 && currentLength >= kmerLength) {
					kmerProcessor.processKmer(kmer, i - kmerLength + 1);
				}
				currentLength = 0;
			}
		}
		if (currentLength % kmerInterval != 0&& currentLength >= kmerLength) {
			kmerProcessor.processKmer(kmer, end - kmerLength + 1);
		}
	}

}
