package compressor.logger;

public enum LogLevel {
    /**
     * No logging output.
     */
    NONE(0),
    /**
     * General operational information and status updates.
     */
    INFO(1),
    /**
     * Non-critical issues or warnings that should be noted.
     */
    WARNING(2),
    /**
     * Highly detailed, low-level messages for debugging and tracing.
     */
    DEBUG(3);

    private final int level;

    /**
     * Constructor for the LogLevel enum.
     * @param level The numerical severity/detail value.
     */
    private LogLevel(int level) {
        this.level = level;
    }

    /**
     * Gets the numerical level value, used for comparison in the logger filtering logic.
     * @return The integer level of this constant.
     */
    public int getLevel() {
        return level;
    }
}