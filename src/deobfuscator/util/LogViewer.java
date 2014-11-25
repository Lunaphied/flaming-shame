package deobfuscator.util;


import java.io.*;

public class LogViewer {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: LogViewer <log file>");
            return;
        }

        File logFile = new File(args[0]);
        if (!logFile.exists()) {
            System.out.println("Log file must exist!");
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() >= 1 && line.charAt(0) == '2') {
                    System.out.println("[Info] " + line.substring(1));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
