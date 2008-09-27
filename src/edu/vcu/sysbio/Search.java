/*
 * 
 * $Log: Search.java,v $
 * Revision 1.5  2008/09/27 17:08:38  hugh
 * Updated.
 *
 * Revision 1.4  2008-08-13 19:08:46  hugh
 * Updated.
 *
 * Revision 1.3  2008-07-01 15:59:22  hugh
 * Updated.
 *
 * Revision 1.2  2008-06-10 13:48:48  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 * Revision 1.12  2008-04-03 02:49:10  hugh
 * Fixed various bugs
 *
 * Revision 1.11  2008-03-26 22:01:21  hugh
 * Optimized brute force approach.
 *
 * Revision 1.10  2008-03-26 19:06:44  hugh
 * Updated w/ Brute Force approach
 *
 * Revision 1.9  2008-03-26 16:53:11  hugh
 * Used "no q" algorithm for performance comparison.
 *
 * Revision 1.8  2008-03-26 16:50:36  hugh
 * Performance updates.
 *
 * Revision 1.6  2008-03-26 00:40:51  hugh
 * Merged with Phillips code
 *
 * Revision 1.2  2008-03-21 15:04:22  hugh
 * Modified to use DetailedSearchResult
 *
 * Revision 1.1  2008-03-05 23:15:21  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Search {
	private static final int NUM_TASKS_PER_THREAD = 50;

	private static Logger log = Logger.getLogger(Search.class.toString());

	private Match[] results;

	private ExecutorService executor;

	public void search() throws SearchException {
		System.out.println("Using kmer length of "
				+ ProgramParameters.kmerLength + ".");
		System.out.println("Using reference kmer interval of "
				+ ProgramParameters.referenceKmerInterval + ".");
		System.out.println("Using queries kmer interval of "
				+ ProgramParameters.queryKmerInterval + ".");
		System.out.println("Using max matches per query of "
				+ ProgramParameters.maxMatchesPerQuery + ".");
		System.out.println("Number of threads is "
				+ ProgramParameters.numThreads + ".");
		System.out.flush();

		executor = Executors.newFixedThreadPool(ProgramParameters.numThreads);

		if (ProgramParameters.SEARCH_METHOD_INDEX_QUERIES
				.equals(ProgramParameters.searchMethod)) {
			indexQueriesSearch();
		} else if (ProgramParameters.SEARCH_METHOD_INDEX_REFERENCE
				.equals(ProgramParameters.searchMethod)) {
			indexReferenceSearch();
		} else if (ProgramParameters.SEARCH_METHOD_GEN_READS
				.equals(ProgramParameters.searchMethod)) {
			generateReads();
		}

		TimerEvent.printTotals();
	}

	private InputFile loadReferenceFile(int i) {
		TimerEvent.EVENT_LOAD_REFERENCE_FILE.start();
		InputFile referenceFile = new InputFile(
				ProgramParameters.referenceFileNames.get(i), i, true, true);
		referenceFile.loadFile();
		TimerEvent.EVENT_LOAD_REFERENCE_FILE.stop();
		return referenceFile;
	}

	private InputFile loadQueriesFile() {
		TimerEvent.EVENT_LOAD_QUERY_FILE.start();
		InputFile queriesFile = new InputFile(ProgramParameters.queryFileName,
				0, false, false);
		queriesFile.loadFile();
		TimerEvent.EVENT_LOAD_QUERY_FILE.stop();

		return queriesFile;
	}

	private KmerIndex indexQueriesFile(InputFile queriesFile) {
		TimerEvent.EVENT_INDEX_QUERIES.start();
		KmerIndex queriesIndex = new KmerIndex(queriesFile);
		Collection<Callable<Object>> tasks;

		tasks = KmerUtil.buildTasks(queriesFile, queriesIndex,
				ProgramParameters.numThreads * NUM_TASKS_PER_THREAD);
		try {
			executor.invokeAll(tasks);
		} catch (InterruptedException e) {
			System.out.println("Exception: " + e);
			throw new SearchException(e);
		}

		TimerEvent.EVENT_INDEX_QUERIES.stop();
		return queriesIndex;
	}

	private KmerIndex indexReferenceFile(InputFile referenceFile) {
		TimerEvent.EVENT_INDEX_REFERENCE.start();
		KmerIndex referenceIndex = new KmerIndex(referenceFile);
		Collection<Callable<Object>> tasks;
		try {
			tasks = KmerUtil.buildTasks(referenceFile, referenceIndex,
					ProgramParameters.numThreads * NUM_TASKS_PER_THREAD);
			executor.invokeAll(tasks);
		} catch (InterruptedException e) {
			System.out.println("Exception: " + e);
			throw new SearchException(e);
		}
		TimerEvent.EVENT_INDEX_REFERENCE.stop();
		return referenceIndex;
	}

	private void indexReferenceSearch() {
		List<InputFile> referenceFiles = new ArrayList();

		int[] checkedPositions;

		InputFile queriesFile = loadQueriesFile();

		results = new Match[queriesFile.data.length
				/ ProgramParameters.queryLength + 1];

		for (int i = 0; i < ProgramParameters.referenceFileNames.size(); ++i) {
			InputFile referenceFile = loadReferenceFile(i);
			referenceFiles.add(referenceFile);
			KmerIndex referenceIndex = indexReferenceFile(referenceFile);

			checkedPositions = null;
			checkedPositions = new int[referenceFile.dataOffsets[2]
					- referenceFile.dataOffsets[0]];

			TimerEvent.EVENT_SEARCH_QUERIES.start();

			QueriesAligner aligner = new QueriesAligner(queriesFile,
					referenceIndex, results, checkedPositions);
			QueriesAligner.resetSegmentCount();

			try {
				executor.invokeAll(KmerUtil.buildTasks(queriesFile, aligner,
						ProgramParameters.numThreads * NUM_TASKS_PER_THREAD));
			} catch (InterruptedException e) {
				System.out.println("Interrupted error");
			}
			TimerEvent.EVENT_SEARCH_QUERIES.stop();

			referenceFile.clearData();
		}

		executor.shutdown();

		ElandOutput.processResults(results, queriesFile, referenceFiles);

	}

	private void indexQueriesSearch() {
		List<InputFile> referenceFiles = new ArrayList();

		InputFile queriesFile = loadQueriesFile();

		KmerIndex queriesIndex = indexQueriesFile(queriesFile);

		results = new Match[queriesFile.data.length
		    				/ ProgramParameters.queryLength + 1];
		LongSet hits =  LongSets.synchronize(new LongOpenHashSet());

		for (int i = 0; i < ProgramParameters.referenceFileNames.size(); ++i) {
			InputFile referenceFile = loadReferenceFile(i);

			TimerEvent.EVENT_SEARCH_REFERENCE.start();
			referenceFiles.add(referenceFile);

			ReferenceAligner.resetSegmentCount();
			ReferenceAligner aligner = new ReferenceAligner(referenceFile,
					queriesIndex, results, hits);

			Collection<Callable<Object>> tasks = KmerUtil.buildTasks(
					referenceFile, aligner, ProgramParameters.numThreads
							* NUM_TASKS_PER_THREAD);

			try {
				executor.invokeAll(tasks);
			} catch (InterruptedException e) {
				System.out.println("Interrupted error");
			}

			referenceFile.clearData();
			hits.clear();
			TimerEvent.EVENT_SEARCH_REFERENCE.stop();
		}

		executor.shutdown();

		ElandOutput.processResults(results, queriesFile, referenceFiles);

	}

	private void indexBothSearch() {
		List<InputFile> referenceFiles = new ArrayList();

		InputFile queriesFile = loadQueriesFile();

		KmerIndex queriesIndex = indexQueriesFile(queriesFile);

		results = new Match[queriesFile.data.length
		    				/ ProgramParameters.queryLength + 1];

		for (int i = 0; i < ProgramParameters.referenceFileNames.size(); ++i) {
			InputFile referenceFile = loadReferenceFile(i);

			KmerIndex referenceIndex = indexReferenceFile(referenceFile);

			TimerEvent.EVENT_SEARCH_REFERENCE.start();
			referenceFiles.add(referenceFile);

			Collection<Callable<Object>> tasks = buildTasks(referenceIndex,
					queriesIndex);

			try {
				executor.invokeAll(tasks);
			} catch (InterruptedException e) {
				System.out.println("Interrupted error");
			}

			referenceFile.clearData();
			TimerEvent.EVENT_SEARCH_REFERENCE.stop();
		}

		executor.shutdown();

		ElandOutput.processResults(results, queriesFile, referenceFiles);

	}

	private Collection<Callable<Object>> buildTasks(KmerIndex referenceIndex,
			KmerIndex queriesIndex) {
		Long2ObjectMap<KmerPositionList> index = referenceIndex
				.getKmerPositionsMap();

		return null;
	}

	public static void main(String[] args) {
		System.out.println ("Maximal Oligo Mapping Version 0.1 - PreRelease");
		try {

			if (ProgramParameters.loadFromCommandLine(args)) {
				Search search = new Search();
				search.search();
			}

		} catch (Throwable e) {
			System.out.println("Unexpected Error:");
			e.printStackTrace(System.out);
		}
	}

	private void generateReads() {
		InputFile referenceFile = loadReferenceFile(0);
		PrintStream ps;
		try {
			ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(
					ProgramParameters.outputFileName)));
		} catch (FileNotFoundException e) {
			throw new SearchException(e);
		}

		Random random = new Random();
		int referenceLength = referenceFile.dataOffsets[1]
				- referenceFile.dataOffsets[0] - ProgramParameters.queryLength;
		int minGap = 300;
		int maxGap = 900;

		for (int i = 0; i < 10000000; ++i) {
			int gap = random.nextInt(maxGap - minGap) + minGap;
			int firstPos = random.nextInt(referenceLength - maxGap
					- ProgramParameters.queryLength)
					+ ProgramParameters.queryLength;
			if (random.nextBoolean()) {
				firstPos += referenceFile.dataOffsets[1];
			}
			int secondPos = firstPos + gap;

			ps.println(mutate(new String(referenceFile.data, firstPos,
					ProgramParameters.queryLength))
					+ "\t"
					+ mutate(new String(referenceFile.data, secondPos,
							ProgramParameters.queryLength)));
		}

		ps.close();
	}

	private String mutate(String string) {
		Random random = new Random();

		int numMutations = random.nextInt(3);

		StringBuffer buffer = new StringBuffer(string);

		for (int i = 0; i < numMutations; ++i) {
			char ch;
			int pos = random.nextInt(buffer.length());
			int charNum = random.nextInt(4);
			switch (charNum) {
			case 0:
				ch = 'A';
				break;
			case 1:
				ch = 'G';
				break;
			case 2:
				ch = 'T';
				break;
			case 3:
				ch = 'C';
				break;
			default:
				ch = 'A';
			}
			buffer.setCharAt(pos, ch);
		}

		return buffer.toString();
	}
}
