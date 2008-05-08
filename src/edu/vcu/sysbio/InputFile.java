/*
 * $Log: InputFile.java,v $
 * Revision 1.1  2008/05/08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class InputFile {
	private static final int READ_BUFFER_SIZE = 65536 * 4;
	public static final byte NOMATCH_CHAR = 'X';
	public static final byte WILDCARD_CHAR = 'N';

	// basic information about file
	public String fileName;
	public int fileNum;
	public boolean reverseData;
	public boolean genomeFile;
	public int queryLength;

	public byte[] data;
	public int[] dataOffsets;
	public byte[] headers;
	public int[] headerOffsets;

	/**
	 * @param fileName
	 * @param reverse
	 * @param genomeFile
	 * @param queryLength
	 */
	public InputFile(String fileName, int fileNum, boolean reverseData,
			boolean genomeFile, int queryLength) {
		this.fileName = fileName;
		this.fileNum = fileNum;
		this.reverseData = reverseData;
		this.genomeFile = genomeFile;
		this.queryLength = queryLength;
	}

	public void loadFile() throws SearchException {
		try {
			int fileLength;
			byte[] fileBuffer = new byte[READ_BUFFER_SIZE];
			int bufferPos = 0;
			int bytesRead = 0;

			File file = new File(fileName);
			fileLength = (int) file.length();

			System.out.println("Reading file " + fileName + ", first pass");
			InputStream is = new BufferedInputStream(new FileInputStream(file));

//			InputStream is = new FileInputStream(file);

			byte ch = 0;

			int dataPos = 0;
			int numSegments = 0;
			int headerPos = 0;
			int numHeaders = 0;
			int bytesProcessed = 0;
			boolean inHeader = false;

			// parse (to obtain data sizes) and validate input file
			while (bytesProcessed < fileLength) {
				bytesProcessed++;
				if (bufferPos >= bytesRead) {
					bytesRead = is.read(fileBuffer);
					bufferPos = 0;
				}
				ch = fileBuffer[bufferPos++];

				if (!inHeader) {
					if (ch == 'A' || ch == 'G' || ch == 'C' || ch == 'T'
							|| ch == WILDCARD_CHAR) {
						++dataPos;
					} else if (ch == 13 || ch == 10) {
						if (!genomeFile && dataPos % queryLength != 0) {
							throw new SearchException(
									"Invalid length query in file  ["
											+ fileName + "] at byte "
											+ bytesProcessed);
						}
					} else if (ch == '>') {
						inHeader = true;
						if (genomeFile && numHeaders > 0) {
							++numSegments;
							if (reverseData) {
								++numSegments;
							}
						}
					} else {
						throw new SearchException("Invalid character '"
								+ (char) ch + "' in file [" + fileName
								+ "] at byte " + bytesProcessed);
					}
				} else {
					if (ch != 10 && ch != 13) {
						++headerPos;
					} else {
						inHeader = false;
						++numHeaders;
					}
				}
			}
			is.close();

			++numSegments;
			if (reverseData) {
				++numSegments;
			}

			// allocate space for the data
			if (reverseData) {
				data = new byte[dataPos * 2 + ((numSegments + 1) * queryLength)];
			} else {

				data = new byte[dataPos + ((numSegments + 1) * queryLength)];

			}
			dataOffsets = new int[numSegments + 1];
			headers = new byte[headerPos];
			headerOffsets = new int[numHeaders + 1];

			// read the input file into our data structures
			dataPos = 0;
			numSegments = 0;
			headerPos = 0;
			numHeaders = 0;
			bytesProcessed = 0;
			inHeader = false;

			Arrays.fill(data, dataPos, dataPos + queryLength, NOMATCH_CHAR);
			dataPos += queryLength;

			System.out.println("Reading file " + fileName + ", second pass");
			is = new BufferedInputStream(new FileInputStream(file));

			while (bytesProcessed < fileLength) {
				bytesProcessed++;
				if (bufferPos >= bytesRead) {
					bytesRead = is.read(fileBuffer);
					bufferPos = 0;
				}
				ch = fileBuffer[bufferPos++];

				if (!inHeader) {
					if (ch == 'A' || ch == 'G' || ch == 'C' || ch == 'T'
							|| ch == WILDCARD_CHAR) {
						data[dataPos++] = ch;
					} else if (ch == 13 || ch == 10) {

					} else if (ch == '>') {
						inHeader = true;
						if (genomeFile && numHeaders > 0) {
							dataOffsets[++numSegments] = dataPos;
							Arrays.fill(data, dataPos, dataPos + queryLength,
									NOMATCH_CHAR);
							dataPos += queryLength;
							if (reverseData) {
								dataPos = reverseGenome(numSegments++, dataPos);

							}
						}
					}
				} else {
					if (ch != 10 && ch != 13) {
						headers[headerPos++] = ch;
					} else {
						inHeader = false;
						headerOffsets[++numHeaders] = headerPos;
					}
				}
			}

			is.close();

			dataOffsets[++numSegments] = dataPos;
			Arrays.fill(data, dataPos, dataPos + queryLength, NOMATCH_CHAR);
			dataPos += queryLength;
			if (reverseData) {
				dataPos = reverseGenome(numSegments++, dataPos);
			}

		} catch (FileNotFoundException e) {
			throw new SearchException(e);
		} catch (IOException e) {
			throw new SearchException(e);
		}
	}

	private int reverseGenome(int numSegments, int dataPos) {
		for (int readPos = dataOffsets[numSegments] - 1; readPos >= dataOffsets[numSegments - 1]
				+ queryLength; --readPos) {
			byte ch = data[readPos];
			switch (ch) {
			case 'A':
				data[dataPos++] = 'T';
				break;
			case 'T':
				data[dataPos++] = 'A';
				break;
			case 'G':
				data[dataPos++] = 'C';
				break;
			case 'C':
				data[dataPos++] = 'G';
				break;
			default:
				data[dataPos++] = ch;
				break;
			}
		}

		dataOffsets[numSegments + 1] = dataPos;
		Arrays.fill(data, dataPos, dataPos + queryLength, NOMATCH_CHAR);
		dataPos += queryLength;

		return dataPos;

	}

	public void clearData() {
		data = null;
	}
}
