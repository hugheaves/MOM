/*
 * $Log: InputFile.java,v $
 * Revision 1.5  2009-10-19 17:37:03  hugh
 * Revised.
 *
 * Revision 1.4  2009-03-31 15:47:28  hugh
 * Updated for 0.2 release
 *
 * Revision 1.3  2008-08-13 19:08:46  hugh
 * Updated.
 *
 * Revision 1.2  2008-07-01 15:59:22  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
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

/**
 * Parses and stores metadata for an input file. Handles raw and fasta file
 * formats.
 * 
 * Base data is stored one array, and fasta headsers (if any) are stored in
 * another array.
 * 
 * Supports the following base encoding characters from the standard NCBI fasta
 * format:
 * 
 * @author hugh
 * 
 */
public class InputFile {
    private static final int READ_BUFFER_SIZE = 65536 * 4;
    public static final byte NOMATCH_CHAR = 'X';
    public static final byte WILDCARD_CHAR = 'N';
    public static final int MAX_HEADER_SIZE = 65536;

    // basic information about file
    public String fileName;
    public int fileNum;
    public boolean reverseData;
    public boolean referenceFile;

    // size of base data
    public int basesSize;
    // base data
    public byte[] bases;
    // number of base data segments
    public int numSegments;
    // offsets of start of each data segments within the "data" array
    public int[] segmentStart;
    // offsets of end of each data segment within the "data" array
    public int[] segmentEnd;

    // number of fasta headers (if any)
    public int numHeaders;
    // size of fasta headers in bytes (if any)
    public int headersSize;
    // fasta headers (if any)
    public byte[] headers;
    // offsets of the start of each fasta header within the "headers" array
    public int[] headerStart;

    // is this RNA data?
    boolean rnaData;

    /**
     * @param fileName
     * @param reverse
     * @param referenceFile
     * @param queryLength
     */
    public InputFile(String fileName, int fileNum, boolean reverseData,
            boolean referenceFile) {
        this.fileName = fileName;
        this.fileNum = fileNum;
        this.reverseData = reverseData;
        this.referenceFile = referenceFile;
    }

    public void loadFile() {
        try {
            parseFile();
            readFile();
        } catch (FileNotFoundException e) {
            throw new SearchException(e);
        } catch (IOException e) {
            throw new SearchException(e);
        }
    }

    public void reloadFile() {
        try {
            if (basesSize == 0) {
                throw new SearchException("File not loaded");
            }
            readFile();
        } catch (FileNotFoundException e) {
            throw new SearchException(e);
        } catch (IOException e) {
            throw new SearchException(e);
        }
    }

    public void parseFile() throws IOException {

        System.out.println("Reading file " + fileName + ", parsing pass");

        byte[] fileBuffer = new byte[READ_BUFFER_SIZE];
        int bufferPos = 0;
        int bytesRead = 0;

        File file = new File(fileName);
        int fileLength = (int) file.length();
        InputStream is = new BufferedInputStream(new FileInputStream(file));

        byte ch = 0;
        int basesPos = 0;
        int headerPos = 0;
        int bytesProcessed = 0;
        int readLength = 0;
        int headerLength = 0;
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
                if (BaseConstants.validBases[ch] > 0) {
                    ++basesPos;
                    if (ch == 'u' || ch == 'U') {
                        rnaData = true;
                    }
                    ++readLength;
                } else if (ch == 13 || ch == 10) {
                    if (!referenceFile
                            && readLength != ProgramParameters.queryLength) {
                        throw new SearchException(
                                "Invalid length query in file  [" + fileName
                                        + "] at byte " + bytesProcessed);
                    }
                    readLength = 0;
                } else if (ch == '>') {
                    readLength = 0;
                    inHeader = true;
                    headerLength = 0;

                    if (referenceFile && numHeaders > 0) {
                        ++numSegments;
                        if (reverseData) {
                            ++numSegments;
                        }
                    }
                } else {
                    throw new SearchException(
                            "Invalid character '" + (char) ch + "' in file ["
                                    + fileName + "] at byte " + bytesProcessed);
                }
            } else {
                if (ch != 10 && ch != 13) {
                    ++headerLength;
                    if (headerLength <= MAX_HEADER_SIZE) {
                        ++headerPos;
                    } else if (headerLength == MAX_HEADER_SIZE + 1) {
                        System.out.println("Warning: Truncating Fasta header #"
                                + numHeaders + " in file " + fileName
                                + " to length " + MAX_HEADER_SIZE);
                    }
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

        // calculate space for the data
        if (reverseData) {
            basesSize = basesPos * 2
                    + ((numSegments + 1) * ProgramParameters.queryLength);
        } else {
            basesSize = basesPos
                    + ((numSegments + 1) * ProgramParameters.queryLength);
        }

        headersSize = headerPos;

    }

    private void readFile() throws FileNotFoundException, IOException {

        System.out.println("Reading file " + fileName + ", data load pass");

        byte ch;
        int basesPos = 0;
        int bufferPos = 0;
        int bytesRead = 0;
        int headerPos = 0;
        int bytesProcessed = 0;
        int headerCount = 0;
        int segmentCount = 0;
        boolean inHeader = false;
        int headerLength = 0;
        byte[] fileBuffer = new byte[READ_BUFFER_SIZE];

        File file = new File(fileName);
        int fileLength = (int) file.length();
        InputStream is = new BufferedInputStream(new FileInputStream(file));

        // allocate space for file data
        segmentStart = new int[numSegments];
        segmentEnd = new int[numSegments];
        headers = new byte[headersSize];
        headerStart = new int[numHeaders + 1];
        bases = new byte[basesSize];

        // pad the first part of the array with "no match" characters
        Arrays.fill(bases, 0, ProgramParameters.queryLength, NOMATCH_CHAR);
        basesPos += ProgramParameters.queryLength;
        segmentStart[0] = basesPos;

        is = new BufferedInputStream(new FileInputStream(file));

        while (bytesProcessed < fileLength) {
            bytesProcessed++;
            if (bufferPos >= bytesRead) {
                bytesRead = is.read(fileBuffer);
                bufferPos = 0;
            }
            ch = fileBuffer[bufferPos++];

            if (!inHeader) {
                if (BaseConstants.validBases[ch] > 0) {
                    bases[basesPos++] = BaseConstants.validBases[ch];
                } else if (ch == 13 || ch == 10) {
                    // do nothing
                } else if (ch == '>') {
                    inHeader = true;
                    headerLength = 0;
                    if (referenceFile && headerCount > 0) {
                        segmentEnd[segmentCount] = basesPos;
                        Arrays.fill(bases, basesPos,
                                basesPos + ProgramParameters.queryLength,
                                NOMATCH_CHAR);
                        basesPos += ProgramParameters.queryLength;
                        segmentStart[++segmentCount] = basesPos;
                        if (reverseData) {
                            basesPos = reverseReference(segmentCount++,
                                    basesPos);
                            segmentStart[segmentCount] = basesPos;
                        }
                    }
                }
            } else {
                if (ch != 10 && ch != 13) {
                    ++headerLength;
                    if (headerLength <= MAX_HEADER_SIZE) {
                        headers[headerPos++] = ch;
                    }
                } else {
                    inHeader = false;
                    headerStart[++headerCount] = headerPos;
                }
            }
        }

        is.close();

        segmentEnd[segmentCount] = basesPos;
        Arrays.fill(bases, basesPos, basesPos + ProgramParameters.queryLength,
                NOMATCH_CHAR);
        basesPos += ProgramParameters.queryLength;
        if (reverseData) {
            segmentStart[++segmentCount] = basesPos;
            basesPos = reverseReference(segmentCount, basesPos);
        }

        ++segmentCount;
    }

    private int reverseReference(int numSegments, int dataPos) {
        for (int readPos = segmentEnd[numSegments - 1]
                - 1; readPos >= segmentStart[numSegments - 1]; --readPos) {
            byte ch = bases[readPos];
            if (ch == 'A') {

                if (rnaData) {
                    bases[dataPos++] = 'U';
                } else {
                    bases[dataPos++] = 'T';
                }

            } else {
                bases[dataPos++] = BaseConstants.complementaryBases[ch];
            }
        }

        segmentEnd[numSegments] = dataPos;
        Arrays.fill(bases, dataPos, dataPos + ProgramParameters.queryLength,
                NOMATCH_CHAR);
        dataPos += ProgramParameters.queryLength;

        return dataPos;

    }

    public void clearData() {
        bases = null;
    }

    private void debugDump() {
        System.out.println(
                "======================== DUMP =======================");
        System.out.println("Filename = " + this.fileName);
        System.out.println("File number = " + this.fileNum);
        System.out
                .println("Segment array length = " + this.segmentStart.length);
        System.out.println("Num Segments = " + this.numSegments);
        for (int i = 0; i < this.segmentStart.length; ++i) {
            System.out
                    .println("Segment # " + i + " start = " + segmentStart[i]);
            System.out.println("Segment # " + i + " end = " + segmentEnd[i]);
            System.out
                    .println(
                            "Segment Data: ["
                                    + new String(bases, segmentStart[i],
                                            segmentEnd[i] - segmentStart[i])
                                    + "]");

        }
        System.out.println("Num headers = " + (headerStart.length - 1));
        for (int i = 0; i < headerStart.length - 1; ++i) {
            System.out.println("Header # " + i + " = " + new String(headers,
                    headerStart[i], headerStart[i + 1] - headerStart[i]));
        }

    }

    public static void main(String[] args) {
        ProgramParameters.queryLength = 10;
        InputFile inputFile = new InputFile("test.fa", 0, true, true);
        inputFile.loadFile();
        inputFile.debugDump();
        inputFile = new InputFile("test.fa", 0, false, true);
        inputFile.loadFile();
        inputFile.debugDump();
        inputFile = new InputFile("test2.fa", 0, true, true);
        inputFile.loadFile();
        inputFile.debugDump();
        inputFile = new InputFile("test2.fa", 0, false, true);
        inputFile.loadFile();
        inputFile.debugDump();
        inputFile = new InputFile("test3.fa", 0, true, false);
        inputFile.loadFile();
        inputFile.debugDump();
        inputFile = new InputFile("test3.fa", 0, false, false);
        inputFile.loadFile();
        inputFile.debugDump();
    }

}
