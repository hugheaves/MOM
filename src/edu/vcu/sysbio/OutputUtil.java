package edu.vcu.sysbio;

import java.nio.CharBuffer;

public class OutputUtil {

    public static int getQueryId(InputFile queriesFile, char[] charBuf,
            int queryNum) {
        if (queriesFile.headers.length == 0) {
            CharBuffer buffer = CharBuffer.wrap(charBuf);
            buffer.append(queriesFile.fileName);
            buffer.append(":");
            buffer.append(Integer.toString(queryNum));
            return buffer.position();
           
        } else {
            int len = queriesFile.headerStart[queryNum]
                    - queriesFile.headerStart[queryNum - 1];
            bytesToChars(queriesFile.headers, charBuf,
                    queriesFile.headerStart[queryNum - 1], len);
            return (len);
        }
    }

    public static char[] bytesToChars(byte[] byteBuf, char charBuf[],
            int offset, int length) {
        int byteBufPos = offset;
        for (int i = 0; i < length; ++i) {
            charBuf[i] = (char) byteBuf[byteBufPos++];
        }
        return charBuf;
    }
}