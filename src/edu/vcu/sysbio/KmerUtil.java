/*
 * $Log: KmerUtil.java,v $
 * Revision 1.3  2008/08/13 19:08:46  hugh
 * Updated.
 *
 * Revision 1.2  2008-07-01 15:59:21  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Various utility functions to compute bit-packed kmers from bytes
 * 
 * @author Hugh
 * @since Feb 19, 2008
 * @version $Revision: 1.3 $
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
	 * Builts a kmer using bytes from reference[0] to reference[kmerLength - 2]
	 * (i.e. number of bytes used is one less than the size of the kmer)
	 * 
	 * @param reference
	 * @param mask
	 * @param startPos
	 * @return
	 */
	public static final long initializeKmer(byte[] reference, int kmerLength,
			int startPos) {
		long kmer = 0;
		for (int i = startPos; i < startPos + kmerLength - 1; ++i) {
			kmer = kmer << 2; // shift our hash value
			kmer += byteToBits(reference[i]);
		}
		return kmer;
	}

	public static final int byteToBits(byte b) {
		if (b == 'G') {
			return 1;
		} else if (b == 'C') {
			return 2;
		} else if (b == 'T' || b == 'U') {
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

	public static void generateQueryKmers(byte[] data, int start, int end,
			int kmerLength, int kmerInterval, int queryLength,
			String statusMessage, KmerProcessor kmerProcessor) {

		long mask = computeMask(kmerLength);
		byte ch;
		int currentLength = 0;
		long kmer = 0;
		int dataLength = end - start;

		for (int i = start; i < end; ++i) {
			// if (i % (dataLength / 99) == 0) {
			// System.out.println(statusMessage + " - "
			// + ((long) i * 100 / dataLength) + "% done");
			// }

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

	public static void generateReferenceKmers(byte[] data, int start, int end,
			int kmerLength, int kmerInterval, KmerProcessor kmerProcessor) {

		long mask = computeMask(kmerLength);
		byte ch;
		int currentLength = 0;
		long kmer = 0;
		int dataLength = end - start;

		for (int i = start; i < end; ++i) {
			// if (i % (dataLength / 99) == 0) {
			// System.out.println(
			// (((long) i * 100 / dataLength)) + "% done");
			// }

			ch = data[i];
			if (ch != InputFile.NOMATCH_CHAR && ch != InputFile.WILDCARD_CHAR) {
				++currentLength;
				kmer = addByteToKmer(kmer, mask, data[i]);
				if ((currentLength % kmerInterval == 0)
						&& currentLength >= kmerLength) {
					kmerProcessor.processKmer(kmer, i - kmerLength + 1);
				}
			} else {
				if (currentLength % kmerInterval != 0
						&& currentLength >= kmerLength) {
					kmerProcessor.processKmer(kmer, i - kmerLength + 1);
				}
				currentLength = 0;
			}
		}
		if (currentLength % kmerInterval != 0 && currentLength >= kmerLength) {
			kmerProcessor.processKmer(kmer, end - kmerLength + 1);
		}
	}

	public static int calculateIndexSize(InputFile file) {
		long maxKmers = 0;
		long numKmers = 0;
		long indexSize = 0;

		long dataLength = file.dataOffsets[file.dataOffsets.length - 1]
				- file.dataOffsets[0];

		if (ProgramParameters.kmerLength <= 31) {
			maxKmers = 4 << ((ProgramParameters.kmerLength - 1) * 2);
		} else {
			maxKmers = Long.MAX_VALUE;
		}

		if (file.referenceFile) {
			numKmers = dataLength / ProgramParameters.referenceKmerInterval;
		} else {
			long kmersPerQuery = ProgramParameters.queryLength
					/ ProgramParameters.queryKmerInterval;
			if (ProgramParameters.queryLength
					% ProgramParameters.queryKmerInterval != 0) {
				++kmersPerQuery;
			}
			numKmers = (dataLength / ProgramParameters.queryLength)
					* kmersPerQuery;
		}

		if (numKmers > maxKmers) {
			indexSize = maxKmers;
		} else {
			indexSize = (numKmers / 2);
		}

		System.out.println("Using initial index size of " + indexSize + " for "
				+ file.fileName);
		if (indexSize > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		} else {
			return (int) indexSize;
		}
	}

	public static Collection<Callable<Object>> buildTasks(InputFile file,
			KmerProcessor prototypeTask, int numTasks) {
		Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>();

		System.out.println("TOTAL DATA: start = " + file.dataOffsets[0]
				+ ", end = " + file.dataOffsets[file.dataOffsets.length - 1]);

		if (file.referenceFile) {
			int totalLength = file.dataOffsets[file.dataOffsets.length - 1]
					- file.dataOffsets[0];
			for (int i = 0; i < numTasks; ++i) {
				int start = (int) ((long) file.dataOffsets[0] + (((long) totalLength * i) / numTasks));
				int end = (int) ((long) file.dataOffsets[0] + (((long) totalLength * (i + 1)) / numTasks));
				if (i > 0) {
					start = start - ProgramParameters.kmerLength + 1;
				}
				System.out.println("start = " + start + ", end = " + end);
				KmerProcessor processor;
				try {
					processor = (KmerProcessor) prototypeTask.clone();
				} catch (CloneNotSupportedException e) {
					throw new SearchException(e);
				}
				processor.setStart(start);
				processor.setEnd(end);
				tasks.add(processor);
			}
		} else {
			int numQueries = (file.dataOffsets[file.dataOffsets.length - 1] - file.dataOffsets[0])
					/ ProgramParameters.queryLength;
			for (int i = 0; i < numTasks; ++i) {
				int start = (int) (((long) file.dataOffsets[0] + (((long) numQueries * i) / numTasks)) * ProgramParameters.queryLength);
				int end = (int) (((long) file.dataOffsets[0] + (((long) numQueries * (i + 1)) / numTasks)) * ProgramParameters.queryLength);
				System.out.println("start = " + start + ", end = " + end);
				KmerProcessor processor;
				try {
					processor = (KmerProcessor) prototypeTask.clone();
				} catch (CloneNotSupportedException e) {
					throw new SearchException(e);
				}
				processor.setStart(start);
				processor.setEnd(end);
				tasks.add(processor);
			}
		}

		return tasks;
	}
}
