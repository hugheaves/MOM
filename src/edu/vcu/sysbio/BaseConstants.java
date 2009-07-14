package edu.vcu.sysbio;

public class BaseConstants {

    public static byte[] validBases = new byte[256];

    private static void addValidBase(char c) {
        validBases[Character.toLowerCase(c)] = (byte) Character.toUpperCase(c);
        validBases[Character.toUpperCase(c)] = (byte) Character.toUpperCase(c);
    }

    static {
        addValidBase('A');
        addValidBase('B');
        addValidBase('C');
        addValidBase('D');
        addValidBase('G');
        addValidBase('H');
        addValidBase('K');
        addValidBase('M');
        addValidBase('N');
        addValidBase('R');
        addValidBase('S');
        addValidBase('T');
        addValidBase('U');
        addValidBase('V');
        addValidBase('W');
        addValidBase('X');
        addValidBase('Y');
        validBases['.'] = 'N'; // for Illumina sequences
    }

    public static boolean[] baseMatches = new boolean[256 * 256];

    private static void addMatch(char c, char d) {
        BaseConstants.baseMatches[(c << 8) + d] = true;
        BaseConstants.baseMatches[(d << 8) + c] = true;
    }

    static {
        addMatch('A', 'A');
        addMatch('A', 'N');

        addMatch('C', 'C');
        addMatch('C', 'N');

        addMatch('G', 'G');
        addMatch('G', 'N');

        addMatch('T', 'T');
        addMatch('T', 'N');

        addMatch('U', 'U');
        addMatch('U', 'N');

        addMatch('W', 'W');
        addMatch('W', 'A');
        addMatch('W', 'T');
        addMatch('W', 'N');

        addMatch('S', 'S');
        addMatch('S', 'C');
        addMatch('S', 'G');
        addMatch('S', 'N');

        addMatch('M', 'M');
        addMatch('M', 'A');
        addMatch('M', 'C');
        addMatch('M', 'N');

        addMatch('K', 'K');
        addMatch('K', 'G');
        addMatch('K', 'T');
        addMatch('K', 'N');

        addMatch('R', 'R');
        addMatch('R', 'A');
        addMatch('R', 'G');
        addMatch('R', 'N');

        addMatch('Y', 'Y');
        addMatch('Y', 'C');
        addMatch('Y', 'T');
        addMatch('Y', 'N');

        addMatch('B', 'B');
        addMatch('B', 'C');
        addMatch('B', 'G');
        addMatch('B', 'T');
        addMatch('B', 'N');

        addMatch('D', 'D');
        addMatch('D', 'A');
        addMatch('D', 'G');
        addMatch('D', 'T');
        addMatch('D', 'N');

        addMatch('H', 'H');
        addMatch('H', 'A');
        addMatch('H', 'C');
        addMatch('H', 'T');
        addMatch('H', 'N');

        addMatch('V', 'V');
        addMatch('V', 'A');
        addMatch('V', 'C');
        addMatch('V', 'G');
        addMatch('V', 'N');

        addMatch('N', 'N');
    }

    public static byte[] complementaryBases = new byte[256];

    private static void addComplement(char c, char d) {
        complementaryBases[c] = (byte) d;
        complementaryBases[d] = (byte) c;
    }

    static {
        addComplement('A', 'T');
        addComplement('B', 'V');
        addComplement('C', 'G');
        addComplement('H', 'D');
        addComplement('K', 'M');
        addComplement('R', 'Y');
        addComplement('S', 'S');
        addComplement('W', 'W');
        addComplement('N', 'N');
        addComplement('X', 'X');
        complementaryBases['U'] = 'A';
    }

}
