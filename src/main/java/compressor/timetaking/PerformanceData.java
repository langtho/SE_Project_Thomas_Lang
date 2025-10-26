package compressor.timetaking;
import java.util.ArrayList;
import java.util.List;

public class PerformanceData {

    private String compressionType; // E.g., "NonSpanning", "Overflow", etc.
    private String functionType; // E.g., "Compress", "Decompress", "get"
    private String arraySize; // Custom label for array size (used in JSON output)
    private String valueSize; // Custom label for value size (used in JSON output)
    private int uncompressedArraySize; // The original size of the data array
    private int compressedArraySize; // The resulting size of the data array (or source array size for 'get')
    private List<Measurement> parts; // List of individual timed segments (e.g., Setup, Compressing)
    private long fulldurationNanos; // Sum of all individual measurement times

    /**
     * Initializes a new PerformanceData object.
     * @param compressionType The name of the specific compression algorithm being timed.
     */
    public PerformanceData(String compressionType){
        this.compressionType = compressionType;
        this.parts = new ArrayList<>();
    }

    /**
     * Adds a new Measurement record to the list of timed segments.
     * @param name The name of the segment (e.g., "Setup").
     * @param timeNanos The duration of the segment in nanoseconds.
     */
    public void addMeasurement(String name, long timeNanos){
        this.parts.add(new Measurement(name, timeNanos));
    }

    /**
     * Calculates the total duration of the operation by summing all individual measurements.
     */
    public void calculateFullDuration(){
        long sum = 0;
        for(Measurement measurement : this.parts){
            sum +=measurement.getTimeNanos();
        }
        this.fulldurationNanos = sum;
    }

    // --- Standard Getters and Setters ---

    public String getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }

    public String getFunctionType() {
        return functionType;
    }

    public void setFunctionType(String functionType) {
        this.functionType = functionType;
    }

    public int getUncompressedArraySize() {
        return uncompressedArraySize;
    }

    public void setUncompressedArraySize(int uncompressedArraySize) {
        this.uncompressedArraySize = uncompressedArraySize;
    }

    public int getCompressedArraySize() {
        return compressedArraySize;
    }

    public void setCompressedArraySize(int compressedArraySize) {
        this.compressedArraySize = compressedArraySize;
    }

    public List<Measurement> getParts() {
        return parts;
    }

    public void setParts(List<Measurement> parts) {
        this.parts = parts;
    }

    public long getFullDurationNanos() {
        return fulldurationNanos;
    }

    public void setFullDurationNanos(long fullDurationNanos) {
        this.fulldurationNanos = fullDurationNanos;
    }

    public void setArraySize(String arraySize) {
        this.arraySize = arraySize;
    }

    public String getArraySize() {
        return arraySize;
    }

    public void setValueSize(String valueSize) {
        this.valueSize = valueSize;
    }

    public String getValueSize() {
        return valueSize;
    }
}