package compressor.services;

import compressor.logger.Logger;
import compressor.logger.LoggerFactory;
import compressor.models.BitPacker;
import compressor.models.BitPackerFactory;
import compressor.logger.LogLevel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Arrays;

public class APIController {

    // --- Class Fields ---
    private File sourceFile;
    private File destinationFile;
    private String compressionType;
    private String method;
    private Integer getIndex = null; // Index requested for 'get' method
    private String loggingTypeArgument = null; // Value passed after the --logging flag
    private File performanceLogFile = null; // File path passed after the --performancelogging flag
    private BitPacker bitPacker;
    private Logger logger; // The logger instance

    // Pattern to split text by any whitespace
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    public APIController(ArrayList<String> args) {

        // 1. Assign Obligatory Positional Arguments
        if (args.size() < 4) {
            throw new IllegalArgumentException("At least 4 arguments (Type, Method, Source, Dest) are required.");
        }

        this.compressionType = args.get(0);
        this.method = args.get(1);
        this.sourceFile = new File(args.get(2));
        this.destinationFile = new File(args.get(3));

        int currentArgIndex = 4; // Start index for optional arguments

        // 2. Handle 'get' Method Specific Argument (Detail Level)
        if (this.method.equalsIgnoreCase("get")) {
            // Check if the 5th argument is present and is not a flag
            if (args.size() <= currentArgIndex || args.get(currentArgIndex).startsWith("--")) {
                throw new IllegalArgumentException("'get' method requires a detail level (int) as the 5th argument.");
            }

            try {
                this.getIndex = Integer.parseInt(args.get(currentArgIndex));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("The detail level must be a valid integer: " + args.get(currentArgIndex));
            }

            currentArgIndex++; // Move past the detail level argument
        }

        // 3. Process Optional Flags (--logging and --performancelogging)
        while (currentArgIndex < args.size()) {
            String currentArg = args.get(currentArgIndex);

            if (currentArg.equals("--logging")) {
                // Process the value associated with the --logging flag
                if (currentArgIndex + 1 >= args.size() || args.get(currentArgIndex + 1).startsWith("--")) {
                    throw new IllegalArgumentException("The flag '--logging' requires a value (e.g., verbose) as the next argument.");
                }
                this.loggingTypeArgument = args.get(currentArgIndex + 1);
                currentArgIndex += 2; // Skip both the flag and its value

            } else if (currentArg.equals("--performancelogging")) {
                // Process the file path associated with the --performancelogging flag
                if (currentArgIndex + 1 >= args.size() || args.get(currentArgIndex + 1).startsWith("--")) {
                    throw new IllegalArgumentException("The flag '--performancelogging' requires a file path as the next argument.");
                }
                this.performanceLogFile = new File(args.get(currentArgIndex + 1));
                currentArgIndex += 2; // Skip both the flag and its value

            } else {
                // Warn about or ignore unknown arguments
                System.err.println("WARNING: Unknown or misplaced argument ignored: " + currentArg);
                currentArgIndex++;
            }
        }

        // 4. Create Logger and BitPacker instances
        this.logger = LoggerFactory.createLogger(
                this.loggingTypeArgument != null ? this.loggingTypeArgument : "NONE"
        );
        this.logger.log(LogLevel.INFO, "APIController initialization started.");

        try {
            // Inject the Logger into the BitPacker Factory
            this.bitPacker = BitPackerFactory.createBitPacker(this.compressionType, this.performanceLogFile, this.logger);
            this.logger.log(LogLevel.INFO, "BitPacker created successfully.");
        } catch (Exception e) {
            this.logger.log(LogLevel.WARNING, "Error creating BitPacker: " + e.getMessage());
            throw new RuntimeException("Error creating BitPacker: " + e.getMessage(), e);
        }

        this.logger.log(LogLevel.INFO, "APIController initialized successfully. Method: " + this.method + ", Type: " + this.compressionType);
    }

    // --- Core Execution Method ---
    public void run() throws IOException {
        int[] resultData = null;
        int getResult = -1;

        this.logger.log(LogLevel.INFO, "Starting execution of method: " + this.method.toUpperCase());

        int[] sourceData = extractIntArray(sourceFile);

        switch (this.method.toLowerCase()) {
            case "compress":
                this.logger.log(LogLevel.DEBUG, "Calling BitPacker compress for " + sourceData.length + " items.");
                resultData = bitPacker.compress(sourceData,"custom","custom");
                this.logger.log(LogLevel.INFO, "Compression finished.");
                break;

            case "decompress":
                this.logger.log(LogLevel.DEBUG, "Calling BitPacker decompress for " + sourceData.length + " items.");
                resultData = bitPacker.decompress(sourceData,"custom","custom");
                this.logger.log(LogLevel.INFO, "Decompression finished.");
                break;

            case "get":
                this.logger.log(LogLevel.DEBUG, "Calling BitPacker get for index: " + this.getIndex);
                if (this.getIndex == null) {
                    this.logger.log(LogLevel.WARNING, "Get index was not set.");
                    throw new IllegalStateException("Get index was not properly set during initialization.");
                }
                getResult = bitPacker.get(this.getIndex, sourceData,"custom","custom");
                this.logger.log(LogLevel.INFO, "Get operation finished. Value: " + getResult);

                // Wrap single int result into an array for file writing
                resultData = new int[]{getResult};
                break;

            default:
                this.logger.log(LogLevel.WARNING, "Unknown method encountered: " + this.method);
                throw new IllegalArgumentException("Unknown method: " + this.method);
        }

        // Write the final result data to the destination file
        writeIntArray(resultData, destinationFile);
        this.logger.log(LogLevel.INFO, "Operation finished. Result written to " + destinationFile.getName());
    }

    // --- Utility Methods ---

    /** Extracts an array of integers from a source file (whitespace separated). */
    public static int[] extractIntArray(File sourceFile) throws IOException, NumberFormatException {

        System.out.println("Reading integers from file: " + sourceFile.getAbsolutePath());

        String content = Files.readString(Paths.get(sourceFile.getPath()));

        if (content.trim().isEmpty()) {
            return new int[0];
        }

        String[] numberStrings = WHITESPACE_PATTERN.split(content.trim());

        // Use Streams to filter empty strings and convert valid ones to integers
        return Arrays.stream(numberStrings)
                .filter(s -> !s.trim().isEmpty())
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    /** Writes an array of integers to a destination file, with each integer on a new line. */
    public static void writeIntArray(int[] dataArray, File destinationFile) throws IOException {

        System.out.println("Writing " + dataArray.length + " integers to file: " + destinationFile.getAbsolutePath());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile))) {

            for (int value : dataArray) {
                writer.write(String.valueOf(value));
                writer.newLine(); // Write a newline character for separation
            }

            System.out.println("✅ Successfully wrote integer array to file.");

        } catch (IOException e) {
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