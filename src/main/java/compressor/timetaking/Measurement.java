package compressor.timetaking;

public class Measurement {

    private String name;
    private long timeNanos;

    /**
     * Default constructor, initializes fields to default values (null and 0).
     */
    public Measurement(){}

    /**
     * Constructor used to create a timed measurement record.
     * @param name The name of the measured operation (e.g., "Setup", "Compressing").
     * @param timeNanos The duration of the operation in nanoseconds.
     */
    public Measurement(String name, long timeNanos){
        this.name = name;
        this.timeNanos = timeNanos;
    }

    /**
     * Gets the name of the measured operation.
     * @return The operation name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the duration of the measurement.
     * @return The duration in nanoseconds.
     */
    public long getTimeNanos() {
        return timeNanos;
    }

}