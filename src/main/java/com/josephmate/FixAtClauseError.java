package com.josephmate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FixAtClauseError {

    /**
     * [checkstyle] [ERROR] [filepath]:[lineNumber]: Javadoc at-clause @param should be preceded with an empty line. [JavadocParagraph]
     * \[checkstyle\] \[ERROR\] (.*):(\d+): Javadoc at-clause @\w+ should be preceded with an empty line. \[JavadocParagraph\]
     */
    private static final Pattern regex = Pattern.compile(
            "\\[checkstyle\\] \\[ERROR\\] (.*):(\\d+): Javadoc at-clause @\\w+ should be preceded with an empty line. \\[JavadocParagraph\\]");


    private static final class Violation {
        public final String path;
        public final long lineNumber;

        private Violation(String path, long lineNumber) {
            this.path = path;
            this.lineNumber = lineNumber;
        }
    }

    /**
     * Reads through the build output file, looking for checkstyle issues related to
     * not placing a newline before an at-clause like '@param'.
     *
     * @param args args[0] must contain the path to the build file.
     * @throws IOException when unable to read the build file.
     */
    public static void main(String[] args) throws IOException {
        try (Stream<String> lines = Files.lines(FileSystems.getDefault().getPath(args[0]))) {
            List<Violation> violationsToProcess =
                    lines.map(regex::matcher)
                         .filter(Matcher::matches)
                         .map(matcher -> new Violation(matcher.group(1), Long.parseLong(matcher.group(2))))
                         .collect(Collectors.toList());

            // The build file output the violations from top of the file to the bottom of the file
            // if we process the file in the reverse order, we will not affect the positions of the
            // rest of the violations. If we processed it in the same order, then we would need to
            // do some sort of book keeping to remember the lines inserted to track violations
            // moving down as a result of fixing an earlier violation.
            Collections.reverse(violationsToProcess);

            System.out.println("Fixing " + violationsToProcess.size() + " at-clause violations");

            for(Violation violationToProcess : violationsToProcess) {
                processViolation(violationToProcess);
            }
        }
    }

    private static void processViolation(Violation violation) throws IOException {
        try {
            List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(violation.path), StandardCharsets.UTF_8);

            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(violation.path), StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.write("\n");
                }
            }
        } catch(IOException e) {
            throw new IOException("could not process file " + violation.path, e);
        }
    }

}
