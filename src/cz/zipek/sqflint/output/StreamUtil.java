package cz.zipek.sqflint.output;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class StreamUtil {
    /**
     * Creates input stream (with UTF-8 encoding) from input string.
     * 
     * @param input
     * @return
     */
    public static InputStream stringToStream(String input) {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates string from input stream (with UTF-8 encoding).
     * 
     * @param input
     * @return
     */
    public static String streamToString(InputStream input) {
        // StringBuilder textBuilder = new StringBuilder();
        // Reader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        // int c = 0;
        // try {
        //     while ((c = reader.read()) != -1) {
        //         textBuilder.append((char) c);
        //     }
        // } catch (IOException e) {} // will probably never occur
        // return textBuilder.toString();
        return new BufferedReader(new InputStreamReader(input)).lines().collect(Collectors.joining("\n"));
	}

}