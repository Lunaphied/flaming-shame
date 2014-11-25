package deobfuscator.util;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public enum LogLevel {
    FINE(0),
    DEBUG(1),
    INFO(2),
    WARNING(3),
    ERROR(4),
    CRITICAL(5);

    public int level;

    private LogLevel(int level) {
        this.level = level;
    }

}
