package compressor.logger;

public interface Logger {
    /**
     * Writes a log message if the message's level meets the configured threshold.
     * @param level The severity/detail level of the message.
     * @param message The content of the log message.
     */
    void log(LogLevel level, String message);
}