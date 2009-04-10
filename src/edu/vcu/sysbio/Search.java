/*
 * 
 * $Log: Search.java,v $
 * Revision 1.7  2009/04/10 17:49:39  hugh
 * Minor bug fixes.
 *
 * Revision 1.6  2009-03-31 15:47:28  hugh
 * Updated for 0.2 release
 *
 * Revision 1.5  2008-09-27 17:08:38  hugh
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

/**
 * The main "control" class for the entire program. This is where the main()
 * method resides. Mainly decides which search method to use and calls the
 * appropriate functions to execute the search.
 * 
 * @author hugh
 * 
 */
public class Search {
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

        tasks = KmerUtil.buildTasks(queriesFile, queriesIndex);
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
            tasks = KmerUtil.buildTasks(referenceFile, referenceIndex);
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

        results = new Match[queriesFile.bases.length
                / ProgramParameters.queryLength + 1];

        for (int i = 0; i < ProgramParameters.referenceFileNames.size(); ++i) {
            InputFile referenceFile = loadReferenceFile(i);
            referenceFiles.add(referenceFile);
            KmerIndex referenceIndex = indexReferenceFile(referenceFile);

            checkedPositions = null;
            checkedPositions = new int[referenceFile.segmentEnd[1]
                    - referenceFile.segmentStart[0]];

            TimerEvent.EVENT_SEARCH_QUERIES.start();

            QueriesAligner aligner = new QueriesAligner(queriesFile,
                    referenceIndex, results, checkedPositions);
            QueriesAligner.resetSegmentCount();

            try {
                executor.invokeAll(KmerUtil.buildTasks(queriesFile, aligner));
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

        results = new Match[queriesFile.bases.length
                / ProgramParameters.queryLength + 1];
        LongSet hits = LongSets.synchronize(new LongOpenHashSet());

        for (int i = 0; i < ProgramParameters.referenceFileNames.size(); ++i) {
            InputFile referenceFile = loadReferenceFile(i);

            TimerEvent.EVENT_SEARCH_REFERENCE.start();
            referenceFiles.add(referenceFile);

            ReferenceAligner.resetSegmentCount();
            ReferenceAligner aligner = new ReferenceAligner(referenceFile,
                    queriesIndex, results, hits);

            Collection<Callable<Object>> tasks = KmerUtil.buildTasks(
                    referenceFile, aligner);

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

    public static void main(String[] args) {
        System.out.println("Maximum Oligonucleotide Mapping - Version 0.2");
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
}
