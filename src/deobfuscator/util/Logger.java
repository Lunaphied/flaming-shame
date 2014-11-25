package deobfuscator.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by sjohnson on 11/24/14.
 */
public class Logger implements Log {
    private File file;
    private BufferedWriter writer;

    public Logger(String filename) {
        file = new File(filename);
        System.out.println(file.getPath());
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            writer = new BufferedWriter(new FileWriter(file, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void log(LogLevel level, String message) {
        try {
            addLine(level.level + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addLine(String line) throws IOException {
        writer.write(line);
        writer.newLine();
        writer.flush();
    }
}
