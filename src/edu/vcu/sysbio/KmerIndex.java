/*
 * 
 * $Log: KmerIndex.java,v $
 * Revision 1.1  2008/05/08 18:50:08  hugh
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

public class KmerIndex implements KmerProcessor {
	private static Logger log = Logger.getLogger(KmerIndex.class.toString());

	public final InputFile file;
	public final int kmerLength;
	// public int kmerCount = 0;

	public static final int INITIAL_ARRAY_SIZE = 1;
	private KmerPositionList nullList = new KmerPositionList();
	private int kmerInterval;

	private Long2ObjectMap<KmerPositionList> kmerPositionsMap = new Long2ObjectOpenHashMap<KmerPositionList>();

	// private Map<Long, KmerPositionList> kmerPositionsMap = new HashMap<Long,
	// KmerPositionList>();

	public KmerIndex(InputFile file, int kmerLength, int kmerInterval) {
		this.file = file;
		this.kmerLength = kmerLength;
		this.kmerInterval = kmerInterval;
	}

	public void buildIndex() {
		// kmerCount = 0;
		if (file.genomeFile) {
			KmerUtil.generateGenomeKmers(file.data, 0, file.data.length,
					kmerLength, kmerInterval, this);

		} else {
			KmerUtil.generateQueryKmers(file.data, 0, file.data.length,
					kmerLength, kmerInterval, file.queryLength,
					"Indexing queries", this);

		}
		// System.out.println ("kmerCont = " + kmerCount);
	}

	/**
	 * Adds the kmer at the given position to the index
	 * 
	 * @param kmer
	 * @param position
	 * @param i
	 */
	public void processKmer(long kmer, int position) {
		// ++kmerCount;

//		 System.out.println ("kmer = " + KmerUtil.kmerToString(kmer,
//		 kmerLength) + ", pos = " + position);

		KmerPositionList positions = kmerPositionsMap.get(kmer);

		if (positions == null) {
			positions = new KmerPositionList();
			kmerPositionsMap.put(kmer, positions);
		}

		positions.addPosition(position);

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
					+ KmerUtil.kmerToString(entry.getKey(), kmerLength));
			for (int j = 0; j < entry.getValue().positionCount; ++j) {
				System.out.println("  position:"
						+ entry.getValue().positions[j]);
			}
		}
	}
}
