/*
 * $Log: QueriesAligner.java,v $
 * Revision 1.3  2008/07/01 15:59:21  hugh
 * Updated.
 *
 * Revision 1.2  2008-06-10 13:48:46  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.concurrent.Callable;

public class QueriesAligner extends Aligner implements KmerProcessor {

	private final KmerIndex referenceIndex;
	private  int startPos;
	private  int endPos;
	private static int segmentCount = 0;

	public QueriesAligner(InputFile queriesFile, KmerIndex referenceIndex,
			ObjectSet<Match> results, byte[] matchCounts, int[] checkedPositions) {
		super(referenceIndex.file.data, queriesFile.data, 
				referenceIndex.file.fileNum, results, matchCounts,
				checkedPositions);
		this.referenceIndex = referenceIndex;
	}

	public void processKmer(long kmer, int queryPosition) {
		KmerPositionList referencePositions = referenceIndex
				.getKmerPositions(kmer);

		for (int i = 0; i < referencePositions.positionCount; ++i) {
			doAlignment(referencePositions.positions[i], queryPosition);
		}
	}

	public Object call() throws Exception {
		try {
			KmerUtil.generateQueryKmers(queries, startPos, endPos,
					ProgramParameters.kmerLength,
					ProgramParameters.queryKmerInterval, ProgramParameters.queryLength,
					"Searching queries", this);
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
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	public int getEnd() {
		return endPos;
	}

	public int getStart() {
		return startPos;
	}

	public void setEnd(int end) {
		this.endPos = end;
		
	}

	public void setStart(int start) {
		this.startPos = start;
	}
}
