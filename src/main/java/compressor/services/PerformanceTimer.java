package compressor.services;
import java.io.FileWriter;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class PerformanceTimer {
    private long startTime;
    private final ObjectMapper mapper;
    private final String filePath;
    private final PerformanceData currentTimetaking;

    public PerformanceTimer(String filePath,String compressionType){
        this.filePath = filePath;
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.currentTimetaking = new PerformanceData(compressionType);
    }

    public void start() {
        this.startTime = System.nanoTime();
    }

    public void stop(String partName) {
        long endTime = System.nanoTime();
        long durationNanos = endTime - this.startTime;
        this.currentTimetaking.addMeasurement(partName, durationNanos);
        this.startTime = System.nanoTime();
    }

    public void saveToJson(String functionName,int uncompressed_array_size, int compressed_array_size) {
        this.currentTimetaking.setFunctionType(functionName);
        this.currentTimetaking.setUncompressedArraySize(uncompressed_array_size);
        this.currentTimetaking.setCompressedArraySize(compressed_array_size);
        this.currentTimetaking.calculateFullDuration();



        try(FileWriter writer = new FileWriter(filePath,true)){
            String jsonLine = mapper.writeValueAsString(this.currentTimetaking);
            writer.write(jsonLine+"\n");
        } catch (IOException e) {
            System.err.println("Error writing performance data to file");
            e.printStackTrace();
        }
    }
}
