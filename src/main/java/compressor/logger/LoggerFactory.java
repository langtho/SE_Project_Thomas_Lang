package compressor.logger;

public class LoggerFactory {
    public static Logger createLogger(String arglevel) {
        LogLevel desiredLevel = LogLevel.WARNING;
        if(arglevel!=null){
            try {
                desiredLevel = LogLevel.valueOf(arglevel.toUpperCase());
            }catch (IllegalArgumentException e){
                System.err.println("Invalid log level: "+arglevel);
            }
        }
        return new ConsoleLogger(desiredLevel);
    }

}
