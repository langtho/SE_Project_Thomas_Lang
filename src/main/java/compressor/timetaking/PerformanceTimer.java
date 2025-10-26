package compressor.timetaking;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class PerformanceTimer {

    // --- Singleton Management ---
    private static PerformanceTimer instance;

    // --- Instance Fields ---
    private long startTime; // Stores the start time of the current measured segment in nanoseconds
    private final ObjectMapper mapper; // Jackson object mapper for JSON serialization
    private final File file; // The target file to write performance JSON data to
    private PerformanceData currentTimetaking; // Holds data (measurements, metadata) for the current operation

    /**
     * Private constructor for the Singleton pattern. Initializes the timer instance.
     * @param filePath The file where performance data should be saved (appended).
     * @param compressionType The name of the compression algorithm being timed (e.g., "Spanning").
     */
    public PerformanceTimer(File filePath, String compressionType){
        this.file = filePath;
        // Initialize Jackson mapper and disable pretty printing (indentation)
        this.mapper = new ObjectMapper();
        this.mapper.disable(SerializationFeature.INDENT_OUTPUT);
        // Start a new PerformanceData object for the current run
        this.currentTimetaking = new PerformanceData(compressionType);
    }

    /**
     * Gets the Singleton instance of the PerformanceTimer.
     * Creates a new instance if one does not exist.
     * * NOTE: This pattern relies on external code managing the file and type carefully.
     * @param file The file path for the log.
     * @param compressionType The type of compression being timed.
     * @return The single instance of PerformanceTimer.
     */
    public static PerformanceTimer getInstance(File file, String compressionType){
        if(instance == null){
            instance = new PerformanceTimer(file, compressionType);
        }
        return instance;
    }

    /**
     * Marks the starting point of a new measurement segment.
     */
    public void start() {
        this.startTime = System.nanoTime();
    }

    /**
     * Marks the end of the current measurement segment and logs the duration.
     * Sets the end time, calculates duration, adds the measurement to the list,
     * and resets the startTime for the next segment.
     * @param partName The name of the segment that just finished (e.g., "Setup").
     */
    public void stop(String partName) {
        long endTime = System.nanoTime();
        long durationNanos = endTime - this.startTime;
        this.currentTimetaking.addMeasurement(partName, durationNanos);
        // Reset startTime immediately for continuous measurement
        this.startTime = System.nanoTime();
    }

    /**
     * Finalizes the current PerformanceData record, calculates the full duration,
     * saves the data as a JSON line, and resets for the next operation.
     * @param functionName The type of operation (e.g., "Compress", "get").
     * @param uncompressed_array_size The size of the uncompressed data.
     * @param compressed_array_size The size of the resulting data.
     * @param sizeLabel Custom label for array size (for JSON output).
     * @param valueLabel Custom label for value size (for JSON output).
     */
    public void saveToJson(String functionName, int uncompressed_array_size, int compressed_array_size, String sizeLabel, String valueLabel) {
        // Set final metadata
        this.currentTimetaking.setFunctionType(functionName);
        this.currentTimetaking.setUncompressedArraySize(uncompressed_array_size);
        this.currentTimetaking.setCompressedArraySize(compressed_array_size);
        this.currentTimetaking.setArraySize(sizeLabel);
        this.currentTimetaking.setValueSize(valueLabel);

        // Calculate the sum of all parts
        this.currentTimetaking.calculateFullDuration();


        // Write the finalized record to the file
        try(FileWriter writer = new FileWriter(file, true)){ // Use 'true' for appending
            String jsonLine = mapper.writeValueAsString(this.currentTimetaking);
            writer.write(jsonLine + "\n");
        } catch (IOException e) {
            System.err.println("Error writing performance data to file: " + e.getMessage());
            e.printStackTrace();
        }

        // Reset currentTimetaking for the next run, preserving the compression type
        this.currentTimetaking = new PerformanceData(this.currentTimetaking.getCompressionType());
    }
}