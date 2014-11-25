package deobfuscator.util;

public interface Log {
    void info(String message);
    void log(LogLevel level, String message);
}
