package compressor.logger;

public class ConsoleLogger implements Logger {

    private final LogLevel level;

    /**
     * Initializes the ConsoleLogger with the maximum logging detail level allowed.
     * @param level The configured LogLevel (e.g., INFO, DEBUG).
     */
    public ConsoleLogger(LogLevel level) {
        this.level = level;
    }

    /**
     * Logs a message if the message's severity level is equal to or less than
     * the logger's configured level.
     * * NOTE: The System.out.println format logic was corrected.
     * @param msglevel The severity of the message being logged.
     * @param message The actual log message content.
     */
    @Override
    public void log(LogLevel msglevel, String message) {
        // Check if the message's level is active (level number <= configured level number)
        if (msglevel.getLevel() <= level.getLevel()) {
            System.out.printf("[%s] %s\n", msglevel.name(), message);
        }
    }
}