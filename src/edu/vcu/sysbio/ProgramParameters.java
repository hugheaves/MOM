/*
 * Copyright (c) 2007 Virginia Commonwealth University. All rights reserved.
 * 
 * $Log: ProgramParameters.java,v $
 * Revision 1.1  2008/05/08 18:50:08  hugh
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

public class ProgramParameters {
	public static final String PARAM_MIN_MATCH_LENGTH = "q";
	public static final String PARAM_QUERY_SIZE = "s";
	public static final String PARAM_MAX_MISMATCHES = "k";
	public static final String PARAM_NUM_THREADS = "numThreads";
	public static final String PARAM_OUTPUT_FILE = "output";
	public static final String PARAM_QUERIES_FILE = "query";
	public static final String PARAM_GENOME_FILE = "genome";
	public static final String PARAM_QUERY_KMER_INTERVAL = "queryKmerInterval";
	public static final String PARAM_SEARCH_METHOD = "searchMethod";

	public static final int SEARCH_METHOD_GENOME = 1;
	public static final int SEARCH_METHOD_QUERIES = 2;
	
	private static HashMap<String, ParamMetaData> mainParameters = new HashMap();

	static {
		mainParameters.put(PARAM_MAX_MISMATCHES, new ParamMetaData(
				PARAM_MAX_MISMATCHES, "maximum number of mismatches", "2",
				ParamMetaData.Type.INTEGER));
		mainParameters.put(PARAM_QUERIES_FILE, new ParamMetaData(
				PARAM_QUERIES_FILE, "filename containing queries", null,
				ParamMetaData.Type.INPUT_FILE));
		mainParameters.put(PARAM_MIN_MATCH_LENGTH, new ParamMetaData(
				PARAM_MIN_MATCH_LENGTH, "minimum match size", "36",
				ParamMetaData.Type.INTEGER));
		mainParameters.put(PARAM_OUTPUT_FILE, new ParamMetaData(
				PARAM_OUTPUT_FILE, "filename for output", null,
				ParamMetaData.Type.OUTPUT_FILE));
		mainParameters.put(PARAM_NUM_THREADS, new ParamMetaData(
				PARAM_NUM_THREADS, "number of threads", "1",
				ParamMetaData.Type.INTEGER));
		mainParameters.put(PARAM_QUERY_KMER_INTERVAL, new ParamMetaData(
				PARAM_QUERY_KMER_INTERVAL, "query kmer interval", null,
				ParamMetaData.Type.INTEGER));
		mainParameters.put(PARAM_SEARCH_METHOD, new ParamMetaData(
				PARAM_SEARCH_METHOD, "search method", null,
				ParamMetaData.Type.INTEGER));
		mainParameters.put(PARAM_QUERY_SIZE, new ParamMetaData(
				PARAM_QUERY_SIZE, "query size", null,
				ParamMetaData.Type.INTEGER));

	}

	private static ParamMetaData genomeParam = new ParamMetaData(
			PARAM_GENOME_FILE, "filename containing genome data", null,
			ParamMetaData.Type.INPUT_FILE);

	public int minMatchLength;
	public int queryLength;
	public int numThreads;
	public int maxMismatches;
	public String queryFileName;
	public List<String> genomeFileNames = new ArrayList();
	public String outputFileName;
	public int queryKmerInterval;
	public int searchMethod;

	public boolean loadFromCommandLine(String[] args) throws IOException {
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

	private boolean validateParameter(Properties properties,
			ParamMetaData metaData) {

		String paramValue = properties.getProperty(metaData.getName());
		if (paramValue == null) {
			if (metaData.getDefaultValue() == null) {
				System.out.println("Parameter '" + metaData.getName()
						+ "' is missing. Must specify '"
						+ metaData.getDescription() + "'.");
			} else {
				paramValue = metaData.getDefaultValue();
			}
		}

		if (metaData.getType() == ParamMetaData.Type.INPUT_FILE) {
			File file = new File(paramValue);
			if (!file.canRead()) {
				System.out.println("Query file " + paramValue + " not found.");
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
				return false;
			}
		}

		return true;
	}

	private boolean validateAndLoadProperties(Properties properties) {

		boolean haveErrors = false;

		for (Iterator<Entry<String, ParamMetaData>> i = mainParameters
				.entrySet().iterator(); i.hasNext();) {
			if (!validateParameter(properties, i.next().getValue())) {
				haveErrors = true;
			}
		}

		if (!haveErrors) {
			minMatchLength = Integer.parseInt(properties
					.getProperty(PARAM_MIN_MATCH_LENGTH));

			queryLength = Integer.parseInt(properties
					.getProperty(PARAM_QUERY_SIZE));

			numThreads = Integer.parseInt(properties
					.getProperty(PARAM_NUM_THREADS));

			maxMismatches = Integer.parseInt(properties
					.getProperty(PARAM_MAX_MISMATCHES));

			queryKmerInterval = Integer.parseInt(properties
					.getProperty(PARAM_QUERY_KMER_INTERVAL));

			queryFileName = properties.getProperty(PARAM_QUERIES_FILE);

			outputFileName = properties.getProperty(PARAM_OUTPUT_FILE);
			
			searchMethod = Integer.parseInt(properties
					.getProperty(PARAM_SEARCH_METHOD));
		}

		if (validateParameter(properties, genomeParam)) {
			genomeFileNames.add(properties.getProperty(PARAM_GENOME_FILE));
		} else {
			int i = 1;
			String propName = PARAM_GENOME_FILE + i;
			while ((properties.getProperty(propName) != null)) {
				genomeParam.setName(propName);
				if (validateParameter(properties, genomeParam)) {
					genomeFileNames.add(properties.getProperty(propName));
				} else {
					haveErrors = true;
				}
				++i;
			}
		}

		if (genomeFileNames.size() == 0) {
			haveErrors = true;
		}

		return !haveErrors;
	}
}
