/*
 * $Log: QueriesAligner.java,v $
 * Revision 1.1  2008/05/08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.Set;
import java.util.concurrent.Callable;

public class QueriesAligner extends Aligner implements KmerProcessor,
		Callable<Object> {

	private final KmerIndex genomeIndex;
	private final int startPos;
	private final int endPos;
	private static int segmentCount = 0;
	private static IntSet locations = new IntOpenHashSet();
//	private static IntSet locations = new IntRBTreeSet();
	private static int locationCount = 0;
	
	public QueriesAligner(InputFile queriesFile, KmerIndex genomeIndex,
			int startPos, int endPos, ProgramParameters parameters,
			ObjectSet<Match> results, byte[] matchCounts) {
		super(genomeIndex.file.data, queriesFile.data, queriesFile.queryLength,
				parameters.minMatchLength, parameters.maxMismatches,
				genomeIndex.file.fileNum, results, matchCounts);
		this.genomeIndex = genomeIndex;
		this.startPos = startPos;
		this.endPos = endPos;
	}

	public void processKmer(long kmer, int queryPosition) {
		KmerPositionList genomePositions = genomeIndex.getKmerPositions(kmer);
		
		int queryOffset = queryPosition % queryLength;
		
		if (queryOffset == 0) {
//			System.out.println ("locationCount = " + locationCount + ", locations.size() = " + locations.size());
			for (IntIterator iterator = locations.iterator(); iterator.hasNext();) {
				doAlignment(iterator.nextInt(), queryPosition - queryLength);
			}
			locations.clear();
//			locationCount = 0;
		} 
		
		addLocations(genomePositions, queryOffset);
		
//		for (int i = 0; i < genomePositions.positionCount; ++i) {
//			doAlignment(genomePositions.positions[i], queryPosition);
//		}
	}

	private void addLocations(KmerPositionList genomePositions, int queryOffset) {
		for (int i = 0; i < genomePositions.positionCount; ++i) {
//			locationCount += genomePositions.positionCount;
			locations.add(genomePositions.positions[i] - queryOffset);
		}
	}

	public Object call() throws Exception {
		try {
			KmerUtil.generateQueryKmers(queries, startPos, endPos,
					genomeIndex.kmerLength, 1, queryLength, "Searching queries", this);
			++segmentCount;
			System.out.println("Completed segment " + segmentCount);

		} catch (Exception e) {
			System.out.println("Unexpected Exception in QueriesAligner:\n");
			e.printStackTrace(System.out);
			throw e;
		} catch (Error e) {
			System.out.println("Unexpected Error in QueriesAligner:\n");
			e.printStackTrace(System.out);
			throw e;
		}
		return null;
	}

	public static void resetSegmentCount() {
		segmentCount = 0;
	}
}
