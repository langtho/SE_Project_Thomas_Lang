package compressor.logger;

public enum LogLevel {
    NONE(0),
    INFO(1),
    WARNING(2),
    DEBUG(3);

    private final int level;

    private LogLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
