package edu.vcu.sysbio;

import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

public class GenTestData {
    public static final int NUM_REFERENCES = 20;
    public static final int REFERENCE_SIZE = 10000000;
    public static final int REFERENCE_VARIANCE = 5000000;
    public static final int NUM_READS = 10000000;
    public static final int READ_SIZE = 40;
    public static final int MAX_GAP_SIZE = 1500;
    public static final int MIN_GAP_SIZE = 500;
    public static final int LARGE_GAP_SIZE = 10000;
    public static final double LARGE_GAP_FREQ = 0.1;
    public static final double RANDOM_CONTIG_FREQ = 0.05;
    public static final double SNP_FREQ = 0.015;
    public static final String referenceFileName = "data/big_test_reference.fa";
    public static final String queryFileName1 = "data/big_test_query_1.fa";
    public static final String queryFileName2 = "data/big_test_query_2.fa";

    public static char reference[][] = new char[NUM_REFERENCES][];

    public static void main(String[] args) throws IOException {
        PrintStream refFile = new PrintStream(referenceFileName);

        for (int refNum = 0; refNum < NUM_REFERENCES; ++refNum) {
            int referenceSize = (int) (REFERENCE_SIZE
                    + (2 * REFERENCE_VARIANCE * Math.random())
                    - REFERENCE_VARIANCE);
            reference[refNum] = new char[referenceSize];
        }

        for (int refNum = 0; refNum < NUM_REFERENCES; ++refNum) {

            System.out.println(
                    "Generating sequence data for ref # " + refNum + "...");
            for (int i = 0; i < reference[refNum].length; ++i) {
                reference[refNum][i] = getRandomChar(' ');
            }
            System.out.println("Writing sequence data...");

            refFile.println(">Reference Sequence " + refNum + " | Bases "
                    + reference[refNum].length + " | Generated "
                    + DateFormat.getDateTimeInstance().format(new Date()) + "");

            for (int i = 0; i < reference[refNum].length; ++i) {
                refFile.print(reference[refNum][i]);
                if ((i + 1) % 60 == 0)
                    refFile.println();
            }

            refFile.println();
        }

        refFile.close();

        PrintStream queryFile[] = new PrintStream[2];

        queryFile[0] = new PrintStream(queryFileName1);
        queryFile[1] = new PrintStream(queryFileName2);

        System.out.println("Generating reads");

        char[] read = new char[READ_SIZE];
        int[] readPos = new int[2];
        int[] refPos = new int[2];

        int gapSize;
        int forward = 0;

        for (int i = 0; i < NUM_READS; ++i) {

            refPos[0] = refPos[1] = (int) (NUM_REFERENCES * Math.random());
            if (Math.random() > RANDOM_CONTIG_FREQ) {

                readPos[0] = (int) ((double) (reference[refPos[0]].length
                        - READ_SIZE + 1) * Math.random());
                gapSize = (int) (Math.random() * (MAX_GAP_SIZE - MIN_GAP_SIZE))
                        + MIN_GAP_SIZE;
                if (Math.random() < LARGE_GAP_FREQ) {
                    gapSize += Math.random() * LARGE_GAP_SIZE;
                }
                if (Math.random() >= 0.5) {
                    gapSize = -gapSize;
                }
                readPos[1] = readPos[0] + gapSize;

                if (readPos[1] < 0) {
                    readPos[1] = 0;
                } else if (readPos[1] > reference[refPos[1]].length
                        - READ_SIZE) {
                    readPos[1] = reference[refPos[1]].length - READ_SIZE;
                }

                gapSize = readPos[1] - readPos[0];

            } else {
                refPos[1] = (int) (NUM_REFERENCES * Math.random());
                readPos[0] = (int) ((double) (reference[refPos[0]].length
                        - READ_SIZE + 1) * Math.random());
                readPos[1] = (int) ((double) (reference[refPos[1]].length
                        - READ_SIZE + 1) * Math.random());
                gapSize = 999999;
            }

            for (int j = 0; j < 2; ++j) {
                if (Math.random() < 0.5) {
                    forward = 1;
                } else {
                    forward = 0;
                }

                queryFile[j].print(">Read " + i + " | Reference Sequence "
                        + refPos[j] + " | Position " + (readPos[j] + 1)
                        + " | Direction " + (forward == 1 ? 'F' : 'R')
                        + " | Gap " + gapSize + " | SNP ");

                for (int k = 0; k < READ_SIZE; ++k) {
                    if (Math.random() < SNP_FREQ) {
                        read[k] = getRandomChar(
                                reference[refPos[j]][readPos[j] + k]);
                        queryFile[j].print(
                                k + ":" + reference[refPos[j]][readPos[j] + k]
                                        + "-" + read[k] + " ");
                    } else {
                        read[k] = reference[refPos[j]][readPos[j] + k];
                    }

                }
                if (forward == 0) {
                    read = reverse(read);
                }
                queryFile[j].println();
                queryFile[j].println(new String(read));
            }
        }

        queryFile[0].close();
        queryFile[1].close();

        System.out.println("Done");
    }

    private static char[] reverse(char[] read) {
        char[] revRead = read.clone();
        for (int i = 0; i < read.length; ++i) {
            revRead[revRead.length - i - 1] = read[i];
        }
        for (int i = 0; i < revRead.length; ++i) {
            if (revRead[i] == 'A') {
                revRead[i] = 'T';
            } else if (revRead[i] == 'T') {
                revRead[i] = 'A';
            } else if (revRead[i] == 'G') {
                revRead[i] = 'C';
            } else if (revRead[i] == 'C') {
                revRead[i] = 'G';
            }
        }
        return revRead;
    }

    public static char getRandomChar(char exclude) {
        char ch;
        do {
            int val = (int) (Math.random() * 4);
            switch (val) {
            case 0:
                ch = 'A';
                break;
            case 1:
                ch = 'T';
                break;
            case 2:
                ch = 'G';
                break;
            case 3:
                ch = 'C';
                break;
            default:
                ch = 'A';
                break;
            }
        } while (ch == exclude);
        return ch;
    }
}
