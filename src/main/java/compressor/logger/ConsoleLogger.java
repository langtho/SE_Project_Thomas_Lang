package compressor.logger;

public class ConsoleLogger implements Logger{
    private final LogLevel level;

    public ConsoleLogger(LogLevel level) {
        this.level = level;
    }

    @Override
    public void log(LogLevel msglevel, String message){
        if(msglevel.getLevel() <= level.getLevel()){
            System.out.println("[%s] %s\n" + msglevel.name()+  message);
        }
    }
}