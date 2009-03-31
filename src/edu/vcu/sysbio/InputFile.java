/*
 * $Log: InputFile.java,v $
 * Revision 1.4  2009/03/31 15:47:28  hugh
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
 * 
 * A --> adenosine
 * 
 * B --> G T C
 * 
 * C --> cytidine
 * 
 * D --> G A T
 * 
 * G --> guanine
 * 
 * H --> A C T
 * 
 * K --> G T (keto)
 * 
 * M --> A C (amino)
 * 
 * N --> A G C T (any)
 * 
 * R --> G A (purine)
 * 
 * S --> G C (strong)
 * 
 * T --> thymidine
 * 
 * U --> uridine
 * 
 * V --> G C A
 * 
 * W --> A T (weak)
 * 
 * Y --> T C (pyrimidine)
 * 
 * @author hugh
 * 
 */
public class InputFile {
    private static final int READ_BUFFER_SIZE = 65536 * 4;
    public static final byte NOMATCH_CHAR = 'X';
    public static final byte WILDCARD_CHAR = 'N';

    // basic information about file
    public String fileName;
    public int fileNum;
    public boolean reverseData;
    public boolean referenceFile;

    public static final byte[] validBaseChars = {
    // 0..15
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // 16..31
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // 32..47
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // 48..63
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // 64..79
            0, 'A', 'B', 'C', 'D', 0, 0, 'G', 'H', 0, 0, 'K', 0, 'M', 'N', 0,
            // 80..95
            0, 0, 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 0, 0, 0, 0, 0, 0,
            // 96..111
            0, 'A', 'B', 'C', 'D', 0, 0, 'G', 'H', 0, 0, 'K', 0, 'M', 'N', 0,
            // 112..127
            0, 0, 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 0, 0, 0, 0, 0, 0 };

    // base data
    public byte[] bases;
    // offsets of start of each data segments within the "data" array
    public int[] segmentStart;
    // offsets of end of each data segment within the "data" array
    public int[] segmentEnd;
    // fasta headers (if any)
    public byte[] headers;
    // offsets of the start of each fasta header within the "headers" array
    public int[] headerStart;

    // is this RNA data?
    boolean rnaData;

    // number of base segments
    public int numSegments;

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
            int fileLength;
            byte[] fileBuffer = new byte[READ_BUFFER_SIZE];
            int bufferPos = 0;
            int bytesRead = 0;

            System.out.println("Reading file " + fileName + ", first pass");

            File file = new File(fileName);
            fileLength = (int) file.length();

            InputStream is = new BufferedInputStream(new FileInputStream(file));

            // InputStream is = new FileInputStream(file);

            byte ch = 0;

            int basesPos = 0;
            int headerPos = 0;
            int numHeaders = 0;
            int bytesProcessed = 0;
            int readLength = 0;
            boolean inHeader = false;

            numSegments = 0;

            // parse (to obtain data sizes) and validate input file
            while (bytesProcessed < fileLength) {
                bytesProcessed++;
                if (bufferPos >= bytesRead) {
                    bytesRead = is.read(fileBuffer);
                    bufferPos = 0;
                }
                ch = fileBuffer[bufferPos++];

                if (!inHeader) {
                    if (validBaseChars[ch] > 0) {
                        ++basesPos;
                        if (ch == 'u' || ch == 'U') {
                            rnaData = true;
                        }
                        ++readLength;
                    } else if (ch == 13 || ch == 10) {
                        if (!referenceFile
                                && readLength != ProgramParameters.queryLength) {
                            throw new SearchException(
                                    "Invalid length query in file  ["
                                            + fileName + "] at byte "
                                            + bytesProcessed);
                        }
                        readLength = 0;
                    } else if (ch == '>') {
                        readLength =  0;
                        inHeader = true;
                        if (referenceFile && numHeaders > 0) {
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
                bases = new byte[basesPos * 2
                        + ((numSegments + 1) * ProgramParameters.queryLength)];
            } else {

                bases = new byte[basesPos
                        + ((numSegments + 1) * ProgramParameters.queryLength)];

            }
            segmentStart = new int[numSegments];
            segmentEnd = new int[numSegments];
            headers = new byte[headerPos];
            headerStart = new int[numHeaders + 1];

            // read the input file into our data structures
            basesPos = 0;
            numSegments = 0;
            headerPos = 0;
            numHeaders = 0;
            bytesProcessed = 0;
            inHeader = false;

            // pad the first part of the array with "no match" characters
            Arrays.fill(bases, 0, ProgramParameters.queryLength, NOMATCH_CHAR);
            basesPos += ProgramParameters.queryLength;
            segmentStart[0] = basesPos;

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
                    if (validBaseChars[ch] > 0) {
                        bases[basesPos++] = validBaseChars[ch];
                    } else if (ch == 13 || ch == 10) {

                    } else if (ch == '>') {
                        inHeader = true;
                        if (referenceFile && numHeaders > 0) {
                            segmentEnd[numSegments] = basesPos;
                            Arrays.fill(bases, basesPos, basesPos
                                    + ProgramParameters.queryLength,
                                    NOMATCH_CHAR);
                            basesPos += ProgramParameters.queryLength;
                            segmentStart[++numSegments] = basesPos;
                            if (reverseData) {
                                basesPos = reverseReference(numSegments++,
                                        basesPos);
                                segmentStart[numSegments] = basesPos;
                            }
                        }
                    }
                } else {
                    if (ch != 10 && ch != 13) {
                        headers[headerPos++] = ch;
                    } else {
                        inHeader = false;
                        headerStart[++numHeaders] = headerPos;
                    }
                }
            }

            is.close();

            segmentEnd[numSegments] = basesPos;
            Arrays.fill(bases, basesPos, basesPos
                    + ProgramParameters.queryLength, NOMATCH_CHAR);
            basesPos += ProgramParameters.queryLength;
            if (reverseData) {
                segmentStart[++numSegments] = basesPos;
                basesPos = reverseReference(numSegments, basesPos);
            }

            ++numSegments;

        } catch (FileNotFoundException e) {
            throw new SearchException(e);
        } catch (IOException e) {
            throw new SearchException(e);
        }
    }

    private int reverseReference(int numSegments, int dataPos) {
        for (int readPos = segmentEnd[numSegments - 1] - 1; readPos >= segmentStart[numSegments - 1]; --readPos) {
            byte ch = bases[readPos];
            switch (ch) {
            case 'A':
                if (rnaData) {
                    bases[dataPos++] = 'U';
                } else {
                    bases[dataPos++] = 'T';
                }
                break;
            case 'T':
                bases[dataPos++] = 'A';
                break;
            case 'G':
                bases[dataPos++] = 'C';
                break;
            case 'C':
                bases[dataPos++] = 'G';
                break;
            default:
                bases[dataPos++] = ch;
                break;
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
        System.out
                .println("======================== DUMP =======================");
        System.out.println("Filename = " + this.fileName);
        System.out.println("File number = " + this.fileNum);
        System.out
                .println("Segment array length = " + this.segmentStart.length);
        System.out.println("Num Segments = " + this.numSegments);
        for (int i = 0; i < this.segmentStart.length; ++i) {
            System.out
                    .println("Segment # " + i + " start = " + segmentStart[i]);
            System.out.println("Segment # " + i + " end = " + segmentEnd[i]);
            System.out.println("Segment Data: ["
                    + new String(bases, segmentStart[i], segmentEnd[i]
                            - segmentStart[i]) + "]");

        }
        System.out.println("Num headers = " + (headerStart.length - 1));
        for (int i = 0; i < headerStart.length - 1; ++i) {
            System.out.println("Header # "
                    + i
                    + " = "
                    + new String(headers, headerStart[i], headerStart[i + 1]
                            - headerStart[i]));
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
