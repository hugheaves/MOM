/*
 * $Log: ReferenceAligner.java,v $
 * Revision 1.1  2008/07/01 15:59:21  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.objects.ObjectSet;

public class ReferenceAligner extends Aligner implements KmerProcessor {

	private final KmerIndex queriesIndex;
	private int startPos;
	private int endPos;
	private static int segmentCount = 0;

	public ReferenceAligner(InputFile referenceFile, KmerIndex queriesIndex,
			ObjectSet<Match> results, byte[] matchCounts) {
		super(referenceFile.data, queriesIndex.file.data,
				referenceFile.fileNum, results, matchCounts, null);
		this.queriesIndex = queriesIndex;
	}

	public void processKmer(long kmer, int referencePosition) {
		KmerPositionList queryPositions = queriesIndex.getKmerPositions(kmer);

		for (int i = 0; i < queryPositions.positionCount; ++i) {
			doAlignment(referencePosition, queryPositions.positions[i]);
		}
	}

	public Object call() throws Exception {
		try {
			System.out.println("Searching reference from " + startPos + " to "
					+ endPos);
			KmerUtil.generateReferenceKmers(reference, startPos, endPos,
					ProgramParameters.kmerLength,
					ProgramParameters.referenceKmerInterval, this);
			++segmentCount;
			System.out.println("Completed segment " + segmentCount);
		} catch (Exception e) {
			System.out.println("Unexpected Exception in ReferenceSearch:\n");
			e.printStackTrace(System.out);
			throw e;
		} catch (Error e) {
			System.out.println("Unexpected Error in ReferenceSearch:\n");
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