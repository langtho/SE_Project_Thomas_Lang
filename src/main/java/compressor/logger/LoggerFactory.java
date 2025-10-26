package compressor.logger;

public class LoggerFactory {

    /**
     * Creates and returns a Logger instance based on the user-provided level string.
     * This method embodies the simple Factory Method pattern.
     * * @param argLevel The string value from the command-line argument (e.g., "INFO", "DEBUG").
     * @return The configured ConsoleLogger instance. Defaults to LogLevel.WARNING if input is invalid or null.
     */
    public static Logger createLogger(String argLevel) {

        LogLevel desiredLevel = LogLevel.WARNING; // Default fallback level

        if (argLevel != null) {
            try {
                // Attempt to convert the case-insensitive input string to the LogLevel enum
                desiredLevel = LogLevel.valueOf(argLevel.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Handle cases where the input string does not match any LogLevel constant
                System.err.println("Invalid log level: " + argLevel + ". Defaulting to WARNING.");
            }
        }

        // Return the concrete logger implementation configured with the determined level
        return new ConsoleLogger(desiredLevel);
    }
}