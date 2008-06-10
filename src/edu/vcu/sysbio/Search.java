/*
 * 
 * $Log: Search.java,v $
 * Revision 1.2  2008/06/10 13:48:48  hugh
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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Search {
	private static Logger log = Logger.getLogger(Search.class.toString());

	private int kmerLength;

	// private Set<Match> results = new HashSet<Match>();
	// private Int2ObjectMap<ObjectSet<Match>> results = new
	// Int2ObjectOpenHashMap<ObjectSet<Match>>();
	private ObjectSet<Match> results = new ObjectOpenHashSet<Match>();

	private byte[] matchCounts = null;

	private ProgramParameters parameters;

	public void search(ProgramParameters parameters) throws SearchException {
		this.parameters = parameters;

		kmerLength = parameters.minMatchLength / (parameters.maxMismatches + 1);

		if (kmerLength > 32) {
			kmerLength = 32;
		}

		System.out.println("Using kmer length of " + kmerLength + ".");

		if (parameters.numThreads > 1) {
			results = ObjectSets.synchronize(results);
		}

		if (parameters.searchMethod == ProgramParameters.SEARCH_METHOD_GENOME) {
			indexQueriesSearch();
		} else if (parameters.searchMethod == ProgramParameters.SEARCH_METHOD_QUERIES) {
			indexGenomeSearch();
		} else if (parameters.searchMethod == ProgramParameters.SEARCH_METHOD_GEN_READS) {
			generateReads();
		}

		TimerEvent.printTotals();
	}

	private InputFile loadGenomeFile(int i) throws SearchException {
		TimerEvent.EVENT_LOAD_GENOME_FILE.start();
		InputFile genomeFile = new InputFile(parameters.genomeFileNames.get(i),
				i, true, true, parameters.queryLength);
		genomeFile.loadFile();
		TimerEvent.EVENT_LOAD_GENOME_FILE.stop();
		return genomeFile;
	}

	private InputFile loadQueriesFile() throws SearchException {
		TimerEvent.EVENT_LOAD_QUERY_FILE.start();
		InputFile queriesFile = new InputFile(parameters.queryFileName, 0,
				false, false, parameters.queryLength);
		queriesFile.loadFile();
		TimerEvent.EVENT_LOAD_QUERY_FILE.stop();

		return queriesFile;
	}

	private KmerIndex indexQueriesFile(InputFile queriesFile) {
		TimerEvent.EVENT_INDEX_QUERIES.start();
		KmerIndex queriesIndex = new KmerIndex(queriesFile, kmerLength,
				parameters.queryKmerInterval);
		queriesIndex.buildIndex();
		TimerEvent.EVENT_INDEX_QUERIES.stop();
		return queriesIndex;
	}

	private KmerIndex indexGenomeFile(InputFile genomeFile) {
		TimerEvent.EVENT_INDEX_GENOME.start();
		KmerIndex genomeIndex = new KmerIndex(genomeFile, kmerLength, 1);
		genomeIndex.buildIndex();
		TimerEvent.EVENT_INDEX_GENOME.stop();
		return genomeIndex;
	}

	private void indexGenomeSearch() throws SearchException {
		List<InputFile> genomeFiles = new ArrayList();
		InputFile queriesFile = loadQueriesFile();

		matchCounts = new byte[queriesFile.data.length
				/ queriesFile.queryLength + 1];

		for (int i = 0; i < parameters.genomeFileNames.size(); ++i) {
			InputFile genomeFile = loadGenomeFile(i);
			genomeFiles.add(genomeFile);
			KmerIndex genomeIndex = indexGenomeFile(genomeFile);
			byte[] checkedPositions = new byte[genomeFile.dataOffsets[2] - genomeFile.dataOffsets[0]];
			
			QueriesAligner aligner = new QueriesAligner(queriesFile,
					genomeIndex, 0, queriesFile.data.length, parameters,
					results, matchCounts, checkedPositions);

			TimerEvent.EVENT_SEARCH_QUERIES.start();
			try {
				aligner.call();
			} catch (Exception e) {
			}
			TimerEvent.EVENT_SEARCH_QUERIES.stop();

			genomeFile.clearData();
		}

		ElandOutput.processResults(results, queriesFile, genomeFiles,
				parameters.maxMismatches, parameters.queryLength,
				parameters.outputFileName);

	}

	private void indexQueriesSearch() throws SearchException {
		List<InputFile> genomeFiles = new ArrayList();
		ExecutorService executor = Executors
				.newFixedThreadPool(parameters.numThreads);

		InputFile queriesFile = loadQueriesFile();

		KmerIndex queriesIndex = indexQueriesFile(queriesFile);

		matchCounts = new byte[queriesFile.data.length
				/ queriesFile.queryLength + 1];

		// queriesIndex.dumpKmers();
		// System.out.println ("kmers = " +
		// queriesIndex.getKmerPositionsMap().size());

		for (int i = 0; i < parameters.genomeFileNames.size(); ++i) {
			InputFile genomeFile = loadGenomeFile(i);

			// System.out.println (new String(genomeFile.data, 3087459, 36));
			// System.out.println (new String(genomeFile.data, 2979021 + 36 - 1,
			// 36));
			// System.out.println (new String(genomeFile.data, 6300366, 36));

			// System.exit(0);

			TimerEvent.EVENT_SEARCH_GENOME.start();
			genomeFiles.add(genomeFile);

			Collection<Callable<Object>> tasks = buildTasks(genomeFile,
					queriesIndex);

			try {
				executor.invokeAll(tasks);
			} catch (InterruptedException e) {
				System.out.println("Interrupted error");
			}

			genomeFile.clearData();
			TimerEvent.EVENT_SEARCH_GENOME.stop();
		}

		executor.shutdown();

		ElandOutput.processResults(results, queriesFile, genomeFiles,
				parameters.maxMismatches, parameters.queryLength,
				parameters.outputFileName);

	}

	private void indexBothSearch() throws SearchException {
		List<InputFile> genomeFiles = new ArrayList();
		ExecutorService executor = Executors
				.newFixedThreadPool(parameters.numThreads);

		InputFile queriesFile = loadQueriesFile();

		KmerIndex queriesIndex = indexQueriesFile(queriesFile);

		matchCounts = new byte[queriesFile.data.length
				/ queriesFile.queryLength + 1];

		for (int i = 0; i < parameters.genomeFileNames.size(); ++i) {
			InputFile genomeFile = loadGenomeFile(i);

			KmerIndex genomeIndex = indexGenomeFile(genomeFile);

			TimerEvent.EVENT_SEARCH_GENOME.start();
			genomeFiles.add(genomeFile);

			Collection<Callable<Object>> tasks = buildTasks(genomeIndex,
					queriesIndex);

			try {
				executor.invokeAll(tasks);
			} catch (InterruptedException e) {
				System.out.println("Interrupted error");
			}

			genomeFile.clearData();
			TimerEvent.EVENT_SEARCH_GENOME.stop();
		}

		executor.shutdown();

		ElandOutput.processResults(results, queriesFile, genomeFiles,
				parameters.maxMismatches, parameters.queryLength,
				parameters.outputFileName);

	}

	private Collection<Callable<Object>> buildTasks(KmerIndex genomeIndex,
			KmerIndex queriesIndex) {
		Long2ObjectMap<KmerPositionList> index = genomeIndex
				.getKmerPositionsMap();

		return null;
	}

	private Collection<Callable<Object>> buildTasks(InputFile genomeFile,
			KmerIndex queriesIndex) {
		Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>();

		int totalLength = genomeFile.dataOffsets[genomeFile.dataOffsets.length - 1]
				- genomeFile.dataOffsets[0];
		int numSegments = parameters.numThreads * 50;
		// int numSegments = 1;

		System.out.println("TOTAL DATA: start = " + genomeFile.dataOffsets[0]
				+ ", end = "
				+ genomeFile.dataOffsets[genomeFile.dataOffsets.length - 1]);

		GenomeAligner.resetSegmentCount();

		for (int i = 0; i < numSegments; ++i) {
			int start = (int) ((long) genomeFile.dataOffsets[0] + (((long) totalLength * i) / numSegments));
			int end = (int) ((long) genomeFile.dataOffsets[0] + (((long) totalLength * (i + 1)) / numSegments));
			if (i > 0) {
				start = start - kmerLength + 1;
			}
			System.out.println("start = " + start + ", end = " + end);

			GenomeAligner genomeAligner = new GenomeAligner(genomeFile,
					queriesIndex, start, end, parameters, results, matchCounts);
			tasks.add(genomeAligner);

		}
		return tasks;
	}

	public static void main(String[] args) {
		try {

			ProgramParameters parameters = new ProgramParameters();
			if (parameters.loadFromCommandLine(args)) {
				Search search = new Search();
				search.search(parameters);
			}

		} catch (Throwable e) {
			System.out.println("Unexpected Error:");
			e.printStackTrace(System.out);
		}
	}

	private void generateReads() throws SearchException {
		InputFile genomeFile = loadGenomeFile(0);
		PrintStream ps;
		try {
			ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(
					parameters.outputFileName)));
		} catch (FileNotFoundException e) {
			throw new SearchException(e);
		}

		Random random = new Random();
		int genomeLength = genomeFile.dataOffsets[1]
				- genomeFile.dataOffsets[0] - parameters.queryLength;
		int minGap = 300;
		int maxGap = 900;

		for (int i = 0; i < 10000000; ++i) {
			int gap = random.nextInt(maxGap - minGap) + minGap;
			int firstPos = random.nextInt(genomeLength - maxGap
					- parameters.queryLength)
					+ parameters.queryLength;
			if (random.nextBoolean()) {
				firstPos += genomeFile.dataOffsets[1];
			}
			int secondPos = firstPos + gap;

			ps.println(mutate(new String(genomeFile.data, firstPos,
					parameters.queryLength))
					+ "\t"
					+ mutate(new String(genomeFile.data, secondPos,
							parameters.queryLength)));
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
