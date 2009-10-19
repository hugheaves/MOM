/*
 * Copyright (c) 2008 Virginia Commonwealth University. All rights reserved.
 * 
 * $Log: ProgramParameters.java,v $
 * Revision 1.6  2009/10/19 17:37:03  hugh
 * Revised.
 *
 * Revision 1.5  2009-03-31 15:47:28  hugh
 * Updated for 0.2 release
 *
 * Revision 1.4  2008-08-13 19:08:46  hugh
 * Updated.
 *
 * Revision 1.3  2008-07-01 15:59:21  hugh
 * Updated.
 *
 * Revision 1.2  2008-06-10 13:48:48  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

/**
 * Handles parsing the command line parameters and stores parameter values in
 * static variables for use throughout the rest of the progam.
 * 
 * @author hugh
 * 
 */
public class ProgramParameters {
    public static final String PARAM_MIN_MATCH_LENGTH = "q";
    public static final String PARAM_QUERY_LENGTH = "s";
    public static final String PARAM_MAX_MISMATCHES = "k";
    public static final String PARAM_NUM_THREADS = "numThreads";
    public static final String PARAM_OUTPUT_FILE = "output";
    public static final String PARAM_QUERIES_FILE = "query";
    public static final String PARAM_PAIRED_QUERIES_FILE = "pairedQuery";
    public static final String PARAM_REFERENCE_FILE = "reference";
    public static final String PARAM_SEARCH_METHOD = "searchMethod";
    public static final String PARAM_KMER_LENGTH = "seedLength";
    public static final String PARAM_REFERENCE_KMER_INTERVAL = "referenceSeedInterval";
    public static final String PARAM_QUERY_KMER_INTERVAL = "querySeedInterval";
    public static final String PARAM_MAX_MATCHES_PER_QUERY = "maxMatchesPerQuery";
    public static final String PARAM_PAIRED_READ_MIN_GAP = "pairedReadMinGap";
    public static final String PARAM_PAIRED_READ_MAX_GAP = "pairedReadMaxGap";

    private static HashMap<String, ParamMetaData> mainParameters = new HashMap<String, ParamMetaData>();

    static {
        mainParameters.put(PARAM_MAX_MISMATCHES, new ParamMetaData(
                PARAM_MAX_MISMATCHES, "maximum number of mismatches", true,
                ParamMetaData.Type.INTEGER));
        mainParameters.put(PARAM_MIN_MATCH_LENGTH, new ParamMetaData(
                PARAM_MIN_MATCH_LENGTH, "minimum match length", true,
                ParamMetaData.Type.INTEGER));
        mainParameters.put(PARAM_OUTPUT_FILE, new ParamMetaData(
                PARAM_OUTPUT_FILE, "filename for output", true,
                ParamMetaData.Type.OUTPUT_FILE));
        mainParameters.put(PARAM_SEARCH_METHOD, new ParamMetaData(
                PARAM_SEARCH_METHOD, "search method", true,
                ParamMetaData.Type.STRING));
        mainParameters.put(PARAM_QUERY_LENGTH, new ParamMetaData(
                PARAM_QUERY_LENGTH, "query length", true,
                ParamMetaData.Type.INTEGER));
        mainParameters.put(PARAM_NUM_THREADS, new ParamMetaData(
                PARAM_NUM_THREADS, "number of threads", false,
                ParamMetaData.Type.INTEGER));
        mainParameters.put(PARAM_QUERY_KMER_INTERVAL, new ParamMetaData(
                PARAM_QUERY_KMER_INTERVAL, "query kmer interval", false,
                ParamMetaData.Type.INTEGER));
        mainParameters.put(PARAM_REFERENCE_KMER_INTERVAL, new ParamMetaData(
                PARAM_REFERENCE_KMER_INTERVAL, "reference kmer interval",
                false, ParamMetaData.Type.INTEGER));
        mainParameters.put(PARAM_KMER_LENGTH, new ParamMetaData(
                PARAM_KMER_LENGTH, "seed length", false,
                ParamMetaData.Type.INTEGER));
        mainParameters.put(PARAM_MAX_MATCHES_PER_QUERY, new ParamMetaData(
                PARAM_MAX_MATCHES_PER_QUERY, "max matches per query", false,
                ParamMetaData.Type.INTEGER));
        mainParameters.put(PARAM_PAIRED_READ_MAX_GAP, new ParamMetaData(
                PARAM_PAIRED_READ_MAX_GAP, "paired read maximum gap size",
                false, ParamMetaData.Type.BOOLEAN));
        mainParameters.put(PARAM_PAIRED_READ_MIN_GAP, new ParamMetaData(
                PARAM_PAIRED_READ_MIN_GAP, "paired read minimum gap size",
                false, ParamMetaData.Type.BOOLEAN));
    }

    private static ParamMetaData referenceParam = new ParamMetaData(
            PARAM_REFERENCE_FILE, "filename containing reference data", false,
            ParamMetaData.Type.INPUT_FILE);

    private static ParamMetaData queryParam = new ParamMetaData(
            PARAM_QUERIES_FILE, "filename containing query sequences", false,
            ParamMetaData.Type.INPUT_FILE);

    public static final String SEARCH_METHOD_INDEX_QUERIES = "indexQueries";
    public static final String SEARCH_METHOD_INDEX_REFERENCE = "indexReference";
    public static final String SEARCH_METHOD_PAIRED_QUERIES = "pairedQueries";

    public static int minMatchLength;
    public static int queryLength;
    public static int numThreads;
    public static int maxMismatches;
    public static List<String> queryFileNames = new ArrayList<String>();
    public static String pairedQueryFileName;
    public static List<String> referenceFileNames = new ArrayList<String>();
    public static String outputFileName;
    public static String searchMethod;
    public static int kmerLength;
    public static int referenceKmerInterval;
    public static int queryKmerInterval;
    public static int maxMatchesPerQuery;
    public static boolean pairedReads;
    public static int pairedReadMaxGap;
    public static int pairedReadMinGap;

    public static boolean loadFromCommandLine(String[] args) throws IOException {
        Properties properties = new Properties();

        for (int i = 0; i < args.length; ++i) {
            if (args[i].charAt(0) == '@') {
                FileInputStream inStream;
                try {
                    inStream = new FileInputStream(args[i].substring(1));
                } catch (FileNotFoundException e) {
                    System.out.println("Properties file "
                            + args[i].substring(1) + " not found.");
                    return false;
                }
                properties.load(inStream);
                inStream.close();
            } else {
                properties.load(new StringReader(args[i]));
            }
        }

        System.out.println("Using properties:");
        properties.store(System.out, "");

        return validateAndLoadProperties(properties);

    }

    private static boolean validateParameter(Properties properties,
            ParamMetaData metaData) {

        String paramValue = properties.getProperty(metaData.getName());
        if (paramValue == null) {
            if (metaData.isRequired()) {
                System.out.println("Parameter '" + metaData.getName()
                        + "' is missing. Must specify '"
                        + metaData.getDescription() + "'.");
                return false;
            } else {
                return true;
            }
        }

        if (metaData.getType() == ParamMetaData.Type.INPUT_FILE) {
            File file = new File(paramValue);
            if (!file.canRead()) {
                System.out.println("Input file " + paramValue + " not found.");
                return false;
            }
        } else if (metaData.getType() == ParamMetaData.Type.OUTPUT_FILE) {
            boolean fileWritable = false;
            File file = new File(paramValue);
            try {
                if (file.createNewFile()) {
                    fileWritable = true;
                } else {
                    fileWritable = file.canWrite();
                }
            } catch (IOException e) {
                fileWritable = false;
            }
            if (!fileWritable) {
                System.out.println("Unable to create file " + paramValue
                        + " for output.");
                return false;
            }
        } else if (metaData.getType() == ParamMetaData.Type.INTEGER) {
            try {
                Integer.parseInt(paramValue);
            } catch (NumberFormatException e) {
                System.out.println("Value for parameter " + metaData.getName()
                        + " must be an integer.");
                return false;
            }
        } else if (metaData.getType() == ParamMetaData.Type.BOOLEAN) {
            Boolean.parseBoolean(paramValue);
        }

        return true;
    }

    private static boolean validateAndLoadProperties(Properties properties) {

        int errorCount = 0;

        for (Iterator<Entry<String, ParamMetaData>> i = mainParameters
                .entrySet().iterator(); i.hasNext();) {
            if (!validateParameter(properties, i.next().getValue())) {
                ++errorCount;
            }
        }

        if (errorCount == 0) {
            minMatchLength = Integer.parseInt(properties
                    .getProperty(PARAM_MIN_MATCH_LENGTH));

            queryLength = Integer.parseInt(properties
                    .getProperty(PARAM_QUERY_LENGTH));

            maxMismatches = Integer.parseInt(properties
                    .getProperty(PARAM_MAX_MISMATCHES));

            outputFileName = properties.getProperty(PARAM_OUTPUT_FILE);

            searchMethod = properties.getProperty(PARAM_SEARCH_METHOD);
            if (!SEARCH_METHOD_INDEX_QUERIES.equals(searchMethod)
                    && !SEARCH_METHOD_INDEX_REFERENCE.equals(searchMethod)
                    && !SEARCH_METHOD_PAIRED_QUERIES.equals(searchMethod)) {
                System.out.println("Invalid " + PARAM_SEARCH_METHOD
                        + " parameter specified. Specify one of:");
                System.out.println(" " + SEARCH_METHOD_INDEX_QUERIES);
                System.out.println(" " + SEARCH_METHOD_INDEX_REFERENCE);
                System.out.println(" " + SEARCH_METHOD_PAIRED_QUERIES);
                ++errorCount;
            }

            if (properties.getProperty(PARAM_NUM_THREADS) != null) {
                numThreads = Integer.parseInt(properties
                        .getProperty(PARAM_NUM_THREADS));
            } else {
                numThreads = 1;
            }

            if (properties.getProperty(PARAM_KMER_LENGTH) != null) {
                kmerLength = Integer.parseInt(properties
                        .getProperty(PARAM_KMER_LENGTH));
            } else {
                kmerLength = minMatchLength / (maxMismatches + 1);

                if (kmerLength > 32) {
                    kmerLength = 32;
                }
            }

            if (properties.getProperty(PARAM_QUERY_KMER_INTERVAL) != null) {
                queryKmerInterval = Integer.parseInt(properties
                        .getProperty(PARAM_QUERY_KMER_INTERVAL));
            } else {
                queryKmerInterval = kmerLength;
            }

            if (properties.getProperty(PARAM_REFERENCE_KMER_INTERVAL) != null) {
                referenceKmerInterval = Integer.parseInt(properties
                        .getProperty(PARAM_REFERENCE_KMER_INTERVAL));
            } else {
                referenceKmerInterval = 1;
            }

            if (properties.getProperty(PARAM_MAX_MATCHES_PER_QUERY) != null) {
                maxMatchesPerQuery = Integer.parseInt(properties
                        .getProperty(PARAM_MAX_MATCHES_PER_QUERY));
            } else {
                maxMatchesPerQuery = -1;
            }

            if (properties.getProperty(PARAM_PAIRED_READ_MAX_GAP) != null) {
                pairedReadMaxGap = Integer.parseInt(properties
                        .getProperty(PARAM_PAIRED_READ_MAX_GAP));
            } else {
                pairedReadMaxGap = -1;
            }

            if (properties.getProperty(PARAM_PAIRED_READ_MIN_GAP) != null) {
                pairedReadMinGap = Integer.parseInt(properties
                        .getProperty(PARAM_PAIRED_READ_MIN_GAP));
            } else {
                pairedReadMinGap = -1;
            }

        }

        if (validateParameter(properties, queryParam)) {
            if (properties.getProperty(PARAM_QUERIES_FILE) != null)
                queryFileNames.add(properties.getProperty(PARAM_QUERIES_FILE));
        } else {
            ++errorCount;
        }

        int i = 1;
        String propName = PARAM_QUERIES_FILE + i;
        while ((properties.getProperty(propName) != null)) {
            queryParam.setName(propName);
            if (validateParameter(properties, queryParam)) {
                queryFileNames.add(properties.getProperty(propName));
            } else {
                ++errorCount;
            }
            ++i;
            propName = PARAM_QUERIES_FILE + i;
        }

        if (validateParameter(properties, referenceParam)) {
            if (properties.getProperty(PARAM_REFERENCE_FILE) != null)
                referenceFileNames.add(properties
                        .getProperty(PARAM_REFERENCE_FILE));
        } else {
            ++errorCount;
        }

        i = 1;
        propName = PARAM_REFERENCE_FILE + i;
        while ((properties.getProperty(propName) != null)) {
            referenceParam.setName(propName);
            if (validateParameter(properties, referenceParam)) {
                referenceFileNames.add(properties.getProperty(propName));
            } else {
                ++errorCount;
            }
            ++i;
            propName = PARAM_REFERENCE_FILE + i;
        }

        if (referenceFileNames.size() == 0) {
            System.out.println("Must specify at least one reference file");
            ++errorCount;
        }

        if (queryFileNames.size() == 0) {
            System.out.println("Must specify at least one query file");
            ++errorCount;
        }

        return (errorCount == 0);
    }
}
