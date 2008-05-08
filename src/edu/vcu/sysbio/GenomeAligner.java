/*
 * $Log: GenomeAligner.java,v $
 * Revision 1.1  2008/05/08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.Set;
import java.util.concurrent.Callable;

public class GenomeAligner extends Aligner implements KmerProcessor,
		Callable<Object> {

	private final KmerIndex queriesIndex;
	private final int startPos;
	private final int endPos;
	private static int segmentCount = 0;

	public GenomeAligner(InputFile genomeFile, KmerIndex queriesIndex,
			int startPos, int endPos, ProgramParameters parameters,
			ObjectSet<Match> results, byte[] matchCounts) {
		super(genomeFile.data, queriesIndex.file.data, genomeFile.queryLength,
				parameters.minMatchLength, parameters.maxMismatches,
				genomeFile.fileNum, results, matchCounts);
		this.queriesIndex = queriesIndex;
		this.startPos = startPos;
		this.endPos = endPos;
	}

	public void processKmer(long kmer, int genomePosition) {
		KmerPositionList queryPositions = queriesIndex.getKmerPositions(kmer);

		for (int i = 0; i < queryPositions.positionCount; ++i) {
			doAlignment( genomePosition, queryPositions.positions[i]);
		}
	}

	public Object call() throws Exception {
		try {
			KmerUtil.generateGenomeKmers(genome, startPos, endPos,
					queriesIndex.kmerLength, 1, this);
			++segmentCount;
			System.out.println("Completed segment " + segmentCount);

		} catch (Exception e) {
			System.out.println("Unexpected Exception in GenomeSearch:\n");
			e.printStackTrace(System.out);
			throw e;
		} catch (Error e) {
			System.out.println("Unexpected Error in GenomeSearch:\n");
			e.printStackTrace(System.out);
			throw e;
		}
		return null;
	}

	public static void resetSegmentCount() {
		segmentCount = 0;
	}
}
