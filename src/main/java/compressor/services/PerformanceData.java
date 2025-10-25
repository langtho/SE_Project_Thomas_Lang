package compressor.services;
import java.util.ArrayList;
import java.util.List;

public class PerformanceData {
    private String compressionType;
    private String functionType;
    private String arraySize;
    private String valueSize;
    private int uncompressedArraySize;
    private int compressedArraySize;
    private List<Measurement> parts;
    private long fulldurationNanos;

    public PerformanceData(String compressionType){
        this.compressionType = compressionType;
        this.parts = new ArrayList<>();
    }

    public void addMeasurement(String name, long timeNanos){
        this.parts.add(new Measurement(name, timeNanos));
    }

    public void calculateFullDuration(){
        long sum = 0;
        for(Measurement measurement : this.parts){
            sum +=measurement.getTimeNanos();
        }
        this.fulldurationNanos = sum;
    }

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

    public void setArraySize(String arraySize) {this.arraySize=arraySize;}
    public String getArraySize() {return arraySize;}

    public void setValueSize(String valueSize) {this.valueSize=valueSize;}
    public String getValueSize() {return valueSize;}
}
