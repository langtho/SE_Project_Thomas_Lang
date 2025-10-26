package compressor.services;

import compressor.models.BitPacker;
import compressor.models.BitPackerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.stream.Collectors;
import java.util.Arrays; // Import fuer Arrays.stream

public class APIController {

    // --- Class Fields ---
    private File sourceFile;
    private File destinationFile;
    private String compressionType;
    private String method;
    private Integer getIndex = null;
    private String loggingTypeArgument = null;
    private File performanceLogFile = null; // Changed type to File for consistency
    private BitPacker bitPacker;

    // Regular Expression to split the text by any whitespace (spaces, tabs, newlines)
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    // Use explicit public modifier
    public APIController(ArrayList<String> args) {

        // --- 1. Check and Assign Positional Arguments ---
        if (args.size() < 4) {
            throw new IllegalArgumentException("At least 4 arguments (Type, Method, Source, Dest) are required.");
        }

        this.compressionType = args.get(0);
        this.method = args.get(1);
        this.sourceFile = new File(args.get(2));
        this.destinationFile = new File(args.get(3));

        int currentArgIndex = 4; // Start parsing optional arguments from Index 4

        // --- 2. Special Case: 'get' Method and Detail Level ---
        if (this.method.equalsIgnoreCase("get")) {

            // Check if the 5th argument (Index 4) is present and is not a flag
            if (args.size() <= currentArgIndex || args.get(currentArgIndex).startsWith("--")) {
                throw new IllegalArgumentException("'get' method requires a detail level (int) as the 5th argument.");
            }

            String getIndexStr = args.get(currentArgIndex);

            // Use try-catch logic to validate the number
            try {
                this.getIndex = Integer.parseInt(getIndexStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("The detail level must be a valid integer: " + getIndexStr);
            }

            currentArgIndex++; // Move to the 6th argument (Index 5), where flags begin
        }

        // --- 3. Optional Flag Parsing (from currentArgIndex onwards) ---
        while (currentArgIndex < args.size()) {
            String currentArg = args.get(currentArgIndex);

            if (currentArg.equals("--logging")) {
                // Expect a value as the next argument
                if (currentArgIndex + 1 >= args.size() || args.get(currentArgIndex + 1).startsWith("--")) {
                    throw new IllegalArgumentException("The flag '--logging' requires a value (e.g., verbose) as the next argument.");
                }
                this.loggingTypeArgument = args.get(currentArgIndex + 1);
                currentArgIndex += 2; // Skip both the flag and its value

            } else if (currentArg.equals("--performancelogging")) {
                // Expect a file path as the next argument
                if (currentArgIndex + 1 >= args.size() || args.get(currentArgIndex + 1).startsWith("--")) {
                    throw new IllegalArgumentException("The flag '--performancelogging' requires a file path as the next argument.");
                }
                // Store the path as a File object
                this.performanceLogFile = new File(args.get(currentArgIndex + 1));
                currentArgIndex += 2; // Skip both the flag and its value

            } else {
                // Warn about or ignore unknown arguments
                System.err.println("WARNING: Unknown or misplaced argument ignored: " + currentArg);
                currentArgIndex++;
            }
        }

        // --- 4. BitPacker Creation (Factory Method) ---
        // Assume BitPackerFactory has a static 'createBitPacker' method.
        try {
            // NOTE: The signature must match your Factory's method
            this.bitPacker = BitPackerFactory.createBitPacker(this.compressionType, this.performanceLogFile);
        } catch (Exception e) {
            // Wrap factory exceptions for better context
            throw new RuntimeException("Error creating BitPacker: " + e.getMessage(), e);
        }

        System.out.println("APIController initialized. Logging Type Argument: " + this.loggingTypeArgument);
    }

    // --- Core Execution Method ---
    public void run() throws IOException {
        int[] resultData=null;
        int getresult=-1;
        // Extrahieren der Quelldaten einmal
        int[] sourceData = extractIntArray(sourceFile);

        switch (this.method.toLowerCase()) {
            case "compress":
                // Annahme: bitPacker.compress akzeptiert int[] und File (log)
                resultData = bitPacker.compress(sourceData, "custom","custom");
                break;

            case "decompress":
                // Annahme: bitPacker.decompress akzeptiert int[] (komprimierte Daten) und File (log)
                resultData = bitPacker.decompress(sourceData, "custom","custom");
                break;
            case "get":
                // Annahme: bitPacker.get akzeptiert Index, int[] und File (log)
                if (this.getIndex == null) {
                    throw new IllegalStateException("Get index was not properly set during initialization.");
                }
                getresult = bitPacker.get(this.getIndex, sourceData, "custom","custom");
                break;
            default:
                throw new IllegalArgumentException("Unknown method: " + this.method);
        }

        // Schreibe das Ergebnis in die Zieldatei
        if(resultData==null) {
            System.out.println("The value at Index "+getIndex+" is: "+getresult);
            resultData=new int[]{getresult};
        }
        writeIntArray(resultData, destinationFile);
    }

    // --- Utility Methods ---

    /**
     * Extracts an array of integers from a source file.
     * Assumes the file contains numbers separated by whitespace.
     */
    public int[] extractIntArray(File sourceFile) throws IOException, NumberFormatException {

        System.out.println("Reading integers from file: " + sourceFile.getAbsolutePath());

        // Read the entire file content into a single string.
        String content = Files.readString(Paths.get(sourceFile.getPath()));

        if (content.trim().isEmpty()) {
            return new int[0];
        }

        // CORRECTED: Use Pattern to split the content string into number strings
        String[] numberStrings = WHITESPACE_PATTERN.split(content.trim());

        // Use Java Streams to efficiently map and filter
        return Arrays.stream(numberStrings)
                .filter(s -> !s.trim().isEmpty()) // Filter out any empty strings resulting from splitting
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    /**
     * Writes an array of integers to a destination file, with each integer on a new line.
     */
    public void writeIntArray(int[] dataArray, File destinationFile) throws IOException {

        System.out.println("Writing " + dataArray.length + " integers to file: " + destinationFile.getAbsolutePath());

        // Use try-with-resources to ensure the BufferedWriter is always closed.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile))) {

            for (int value : dataArray) {
                // Convert the integer to a String and write to file
                writer.write(String.valueOf(value));
                writer.newLine(); // Write a newline character
            }

            System.out.println("✅ Successfully wrote integer array to file.");

        } catch (IOException e) {
            // Re-throw the IOException with a more detailed message
            throw new IOException("Failed to write array to file: " + destinationFile.getAbsolutePath(), e);
        }
    }

    // --- Getter Methods ---

    public BitPacker getBitPacker() {
        return bitPacker;
    }

    public String getMethod() {
        return method;
    }

    public Integer getDetailLevel() {
        return getIndex;
    }
}