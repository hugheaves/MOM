/*
 * $Log: KmerUtil.java,v $
 * Revision 1.5  2009/03/31 15:47:28  hugh
 * Updated for 0.2 release
 *
 * Revision 1.4  2008-09-27 17:08:38  hugh
 * Updated.
 *
 * Revision 1.3  2008-08-13 19:08:46  hugh
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
 * @version $Revision: 1.5 $
 * 
 */
public class KmerUtil {
    private static Logger log = Logger.getLogger(KmerUtil.class.toString());

    public static final byte[] BYTE_TO_BITS = {
            // 0..15
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // 16..31
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // 32..47
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // 48..63
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // 64..79
            0, 0 /* A */, 0, 1 /* C */, 0, 0, 0, 2 /* G */, 0, 0, 0, 0, 0, 0,
            0, 0,
            // 80..95
            0, 0, 0, 0, 3 /* T */, 3 /* U */, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // 96..111
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // 112..127
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

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

        currentKmer += BYTE_TO_BITS[nextCharacter];

        return currentKmer;
    }

    public static final String kmerToString(long kmer, int kmerLength) {
        long mask = 3;
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < kmerLength; ++i) {
            long bits = kmer & mask;
            if (bits == 1) {
                buf.insert(0, 'C');
            } else if (bits == 2) {
                buf.insert(0, 'G');
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

        for (int i = start; i < end; ++i) {

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

    public static Collection<Callable<Object>> buildTasks(InputFile file,
            KmerProcessor prototypeTask) {
        int numTasks = 0;
        int maxTasks = ProgramParameters.numThreads * 50;

        Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>();

        System.out.println("TOTAL DATA: start = " + file.segmentStart[0]
                + ", end = " + file.segmentEnd[file.numSegments - 1]);

        if (file.referenceFile) {
            int totalLength = file.segmentEnd[file.numSegments - 1]
                    - file.segmentStart[0];

            numTasks = totalLength / 5000;
            if (numTasks > maxTasks) {
                numTasks = maxTasks;
            } else if (numTasks < 1) {
                numTasks = 1;
            }

            for (int i = 0; i < numTasks; ++i) {
                int start = (int) ((long) file.segmentStart[0] + (((long) totalLength * i) / numTasks));
                int end = (int) ((long) file.segmentStart[0] + (((long) totalLength * (i + 1)) / numTasks));
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
            int numQueries = (file.segmentEnd[file.numSegments - 1] - file.segmentStart[0])
                    / ProgramParameters.queryLength;

            numTasks = numQueries / 10;
            if (numTasks > maxTasks) {
                numTasks = maxTasks;
            } else if (numTasks < 1) {
                numTasks = 1;
            }

            for (int i = 0; i < numTasks; ++i) {
                int start = (int) (((long) file.segmentStart[0] + ((((long) numQueries * i) / numTasks)) * ProgramParameters.queryLength));
                int end = (int) (((long) file.segmentStart[0] + ((((long) numQueries * (i + 1)) / numTasks)) * ProgramParameters.queryLength));
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
