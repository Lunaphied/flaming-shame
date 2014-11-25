package deobfuscator.util;

/**
 * Created by sjohnson on 11/24/14.
 */
public class OutputLogger implements Log {
    @Override
    public void info(String message) {
        System.out.println("[Info] " + message);
    }

    @Override
    public void log(LogLevel level, String message) {
        System.out.println("[" + formatName(level) + "] " + message);
    }

    private String formatName(LogLevel level) {
        StringBuilder builder = new StringBuilder();
        builder.append(level.name().substring(0,1).toUpperCase());
        builder.append(level.name().substring(1).toLowerCase());
        return builder.toString();
    }
}
