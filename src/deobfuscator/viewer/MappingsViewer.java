package deobfuscator.viewer;


import java.io.*;
import java.util.*;

public class MappingsViewer {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: LogViewer <log file> <class name>");
            return;
        } else if (args.length < 2) {
            System.out.print("Enter a class name: ");
            Scanner scanner = new Scanner(System.in);
            args = new String[] {args[0], scanner.nextLine()};
        }

        File logFile = new File(args[0]);
        if (!logFile.exists()) {
            System.out.println("Log file must exist!");
            return;
        }

        String className = args[1];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            String line;
            int linenum = 0;

            List<Mapping> unsortedMappings = new ArrayList<Mapping>();
            while ((line = reader.readLine()) != null) {
                // No linenum cap for now
                if (line.length() >= 1 && line.charAt(0) == '2' && (true || linenum < 200)) {
                    //  System.out.println("[Info] " + line.substring(1));
                    String[] mappingParts = line.substring(1).split(",");
                    if (mappingParts[0].equals(className)) {
                        Mapping mapping = new Mapping(mappingParts[0], mappingParts[1], Double.parseDouble(mappingParts[2]));
                        unsortedMappings.add(mapping);
                        linenum++;
                    }
                }
            }

            Mapping max;

            List<Mapping> sortedAAA = new ArrayList<Mapping>();
            while (unsortedMappings.size() > 0) {
                max = unsortedMappings.get(0);
                for (Mapping mapping : unsortedMappings) {
                    if (max.percentage < mapping.percentage) {
                        max = mapping;
                    }
                }
                // System.out.println(max.transformed);
                // This is a very odd step that I don't know why I have to do
                while (unsortedMappings.contains(max)) {
                    unsortedMappings.remove(max);
                }
                sortedAAA.add(max);
            }
            /*
            for (int i = 0; i < mappings.length; i++) {
                System.out.println(mappings[i].original + "->" + mappings[i].transformed + ": " + mappings[i].percentage*100);
            }*/
            for (Mapping mapping : sortedAAA) {
                System.out.println(mapping.original + "->" + mapping.transformed + ": " + mapping.percentage*100);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
