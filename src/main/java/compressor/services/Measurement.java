package compressor.services;

public class Measurement {
    private String name;
    private long timeNanos;

    public Measurement(){}

    public Measurement(String name, long timeNanos){
        this.name = name;
        this.timeNanos = timeNanos;
    }

    public String getName() {
        return name;
    }

    public long getTimeNanos() {
        return timeNanos;
    }

}
