/*
 * 
 * $Log: KmerIndex.java,v $
 * Revision 1.4  2009/10/19 17:37:03  hugh
 * Revised.
 *
 * Revision 1.3  2009-03-31 15:47:28  hugh
 * Updated for 0.2 release
 *
 * Revision 1.2  2008-07-01 15:59:22  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 * Revision 1.2  2008-03-25 19:50:42  hugh
 * Fixes for merge w/ new KmerUtil
 *
 * Revision 1.1  2008-03-05 23:15:21  hugh
 * Updated.
 *
 * Revision 1.1  2008-02-29 16:50:31  hugh
 * Initial revision.
 *
 */

package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * Basic index structure to store the locations of the kmers within a file.
 * 
 * @author hugh
 * 
 */
public class KmerIndex implements KmerProcessor {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(KmerIndex.class.toString());

    public final InputFile file;
    public static final int INITIAL_ARRAY_SIZE = 1;
    private KmerPositionList nullList = new KmerPositionList();
    int start;
    int end;
    static int segment = 0;

    private Long2ObjectOpenHashMap<KmerPositionList> kmerPositionsMap = null;

    public KmerIndex(InputFile file) {
        this.file = file;
        kmerPositionsMap = new Long2ObjectOpenHashMap<KmerPositionList>();
    }

    public Object call() throws Exception {
        if (file.referenceFile) {
            KmerUtil.generateReferenceKmers(file.bases, start, end,
                    ProgramParameters.kmerLength,
                    ProgramParameters.referenceKmerInterval, this);

        } else {
            KmerUtil.generateQueryKmers(file.bases, start, end,
                    ProgramParameters.kmerLength,
                    ProgramParameters.queryKmerInterval,
                    ProgramParameters.queryLength, "Indexing queries", this);
        }

        ++segment;
        System.out.println("Finished indexing segment " + segment);
        return null;
    }

    /**
     * Adds the kmer at the given position to the index
     * 
     * @param kmer
     * @param position
     * @param i
     */
    public void processKmer(long kmer, int position) {
        KmerPositionList positions;
        // System.out.println("pos = " + position + ", kmer = "
        // + KmerUtil.kmerToString(kmer, ProgramParameters.kmerLength));

        synchronized (kmerPositionsMap) {
            positions = kmerPositionsMap.get(kmer);

            if (positions == null) {
                positions = new KmerPositionList();
                kmerPositionsMap.put(kmer, positions);
            }
        }

        synchronized (positions) {
            positions.addPosition(position);
        }

    }

    public KmerPositionList getKmerPositions(long kmer) {
        KmerPositionList positions = kmerPositionsMap.get(kmer);
        return (positions != null ? positions : nullList);
    }

    public Long2ObjectMap<KmerPositionList> getKmerPositionsMap() {
        return kmerPositionsMap;
    }

    public void dumpKmers() {
        for (Iterator<Entry<Long, KmerPositionList>> i = kmerPositionsMap
                .entrySet().iterator(); i.hasNext();) {
            Entry<Long, KmerPositionList> entry = i.next();
            System.out.println("kmer: "
                    + KmerUtil.kmerToString(entry.getKey(),
                            ProgramParameters.kmerLength));
            for (int j = 0; j < entry.getValue().positionCount; ++j) {
                System.out.println("  position:"
                        + entry.getValue().positions[j]);
            }
        }
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
