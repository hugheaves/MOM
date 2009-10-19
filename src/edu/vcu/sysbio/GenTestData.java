package edu.vcu.sysbio;

import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

public class GenTestData {
    public static final int numReferences = 10;
    public static final int referenceSize = 10000;
    public static final int numReads = 100000;
    public static final int readSize = 40;
    public static final double SNPfreq = 0.015;
    public static final String referenceFileName = "data/test2_reference.fa";
    public static final String queryFileName = "data/test2_query.fa";

    public static char reference[] = new char[referenceSize];

    public static void main(String[] args) throws IOException {
        PrintStream refFile = new PrintStream(referenceFileName);
        PrintStream queryFile = new PrintStream(queryFileName);

        for (int refNum = 0; refNum < numReferences; ++refNum) {

            System.out.println("Generating sequence data for ref # " + refNum
                    + "...");
            for (int i = 0; i < referenceSize; ++i) {
                reference[i] = getRandomChar(' ');
            }
            System.out.println("Writing sequence data...");

            refFile.println(">Reference Sequence " + refNum + " - "
                    + DateFormat.getDateInstance().format(new Date()));

            for (int i = 0; i < referenceSize; ++i) {
                refFile.print(reference[i]);
                if ((i + 1) % 60 == 0)
                    refFile.println();
            }

            refFile.println();
            
            System.out.println("Generating reads...");

            char[] read = new char[readSize];
            for (int i = 0; i < numReads; ++i) {
                queryFile.print(">Ref " + refNum + ", Read " + i + " - ");

                int readPos = (int) ((double) (referenceSize - readSize + 1) * Math
                        .random());
                for (int j = 0; j < readSize; ++j) {
                    if (Math.random() < SNPfreq) {
                        read[j] = getRandomChar(reference[readPos + j]);
                        queryFile.print(j + ":" + reference[readPos + j] + "-"
                                + read[j] + " ");
                    } else {
                        read[j] = reference[readPos + j];
                    }

                }
                queryFile.println();
                queryFile.println(new String(read));
            }

        }
        queryFile.close();

        refFile.close();
        System.out.println("Done");
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
