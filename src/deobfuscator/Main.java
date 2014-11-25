package deobfuscator;

import java.io.FileInputStream;
import java.io.IOException;

import deobfuscator.comparators.BasicClassComparator;
import deobfuscator.comparators.FieldCount;
import deobfuscator.comparators.LinearComparator;
import deobfuscator.comparators.MethodCount;
import deobfuscator.util.Logger;

public class Main {
	public static Logger logger;

	public static void printUsage(String[] args) {

		System.out
				.println("Usage: "
						+ " <Annotated Jar> <Obfuscated Jar> <Mapping File> [Unobfuscated Files to Consider]\n"
						+ "\n"
						+ "<Annotated Jar> - a path to a jar file containing unobfuscated Java *.class files\n"
						+ "<Obfuscated Jar> - a path to a jar file containing obfuscated Java *.class files to match against the unobfuscated *.class files\n"
						+ "<Mapping File> - the mapping file generated from proguard to test for correctness\n"
						+ "[Unobfuscated Files to Consider] - a list of files in the <Annotated Jar> to consider. If not specified, all files will be considered\n");
	}

	public static void main(String[] args) {
		logger = new Logger("logs/" + args[2]);
		// ------------------------------------------------------------------------------------------------
		// Show usage and exit if too few command line parameters are given
		// ------------------------------------------------------------------------------------------------
		if (args.length < 3) {
			printUsage(args);
			return;
		}

		String annotatedJarFile = args[0];

		String obfuscatedJarFile = args[1];
/*

		ClassMapping groundTruth;
		try {
			FileInputStream mappingFile = new FileInputStream(args[2]);
			groundTruth = ClassMapping.loadFromProguardMapping(mappingFile);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		String[] libraryClasses = null;
		if (args.length >= 3) {
			libraryClasses = args[2].split(" ");
		}
*/

		/*
		LinearComparator metric = new LinearComparator();

		These comparators need work, probably use a std deviation thingy or just hardcode distance to be -dist/10+1
		metric.add(new FieldCount(), 1);
		metric.add(new MethodCount(), 1);
		*/

		BasicClassComparator metric = new BasicClassComparator();
		ClassMapping deobfuscation;
		try {
			deobfuscation = Deobfuscator.deobfuscate(annotatedJarFile,
					obfuscatedJarFile, metric);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		//float matchPercentage = groundTruth.getMatchPercent(deobfuscation);

		//logger.info("Match percentage: " + matchPercentage);
		//System.out.println("Match percentage: " + matchPercentage);
		logger.close();
	}
}
