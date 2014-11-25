package deobfuscator.comparators;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import deobfuscator.Main;
import deobfuscator.util.Log;
import deobfuscator.util.LogLevel;
import deobfuscator.util.OutputLogger;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import deobfuscator.ClassComparator;

public class BasicClassComparator implements ClassComparator {

	@Override
	public float similarity(ClassNode original, ClassNode transformed) {
		// TEMP REJECTION
		// a is a good example of something that conflicts for no apparant reasons
		// a->a is correct 1.7.10->1.8->1.8.1pre5->1.8.1 mapping
		// However other mappings like awf get really high scores, in fact awf is 50% on method matching vs 31%
		// and the methods are not that similar. Probably a sign that magic numbers need more tweaking

		//if (!original.name.equals("a")) {
		//	return 0;
		//}

		// SOLVED -> Field count was being used giving a 0% mapping for interfaces
		// aa is another example of something that refuses to be mapped
		// the rating on the actual mapping is less than 10%
		// and lots of other things get close
		// aa->ICommand for 1.7.10->MCP
		//
		/*
		if (!original.name.equals("aa")) {
			return 0f;
		}
		*/

		//
		// Early rejection step - NOT FOR US!
		//
		// It should be a somewhat safe assumption that ProGuard will not:
		// 1) Add additional fields to classes (provided classes are not being folded together)
		// 2) Add additional methods to classes (provided classes are not being folded together)
		// 3) Remove any interfaces from an obfuscated class
		// 4) Create additional inner classes
		//
		/*
		if( original.fields.size() < transformed.fields.size()				||
			original.methods.size() < transformed.methods.size()			||
			original.interfaces.size() != transformed.interfaces.size() 	||
			original.innerClasses.size() < transformed.innerClasses.size()
		){
			return Integer.MAX_VALUE;
		}
		*/

		// We can skipt interface vs class
		if (original.signature != transformed.signature) {
			System.out.println(original.signature + "->" + transformed.signature);
		}
		// We can skip things with non object supers that differ
		if (!original.superName.equals(transformed.superName)) {
			if (original.superName.equals("java/lang/Object") || transformed.superName.equals("java/lang/Object")) {
				return 0;
			}
		}
		/*
		Can't do this
		if (transformed.name.contains("$")) {
			return 0;
		}
		*/

		// The result starts at 0 and strictly increases as confidence decreases
		float result = 0;

		// Make a copy of the set of methods for use in mapping methods
		List<MethodNode> unassignedMethods = new ArrayList<MethodNode>();
		for( int i = 0; i < original.methods.size(); i++ ){
			MethodNode currentNode = (MethodNode)original.methods.get(i);

			// Lets skip empty constructors and initializers
			if (currentNode.desc.equals("()V")) {
				if (currentNode.name.equals("<init>") || currentNode.name.equals("<clinit>")) {
					continue;
				}
			}

			List<String> currentExceptions = new ArrayList<String>();
			for( int j = 0; j < currentNode.exceptions.size(); j++ ){
				currentExceptions.add((String)currentNode.exceptions.get(j));
			}
			String[] exceptions_s = new String[currentExceptions.size()];
			currentExceptions.toArray(exceptions_s);
			MethodNode cloneMethod = new MethodNode(currentNode.access,currentNode.name,currentNode.desc,currentNode.signature,exceptions_s);
			// GOTTA COPY INSTRUCTION LISTS!
			cloneMethod.instructions = currentNode.instructions;
			unassignedMethods.add(cloneMethod);
		}

		// Allocate a BasicMethodComparator
		BasicMethodComparator bmc = new BasicMethodComparator();

		HashMap<String,String> MethodMappings = new HashMap<String,String>();

		//
		// Loop over the methods and try to match them if possible
		//
		for( int j = 0; j < transformed.methods.size(); j++ ){
			MethodNode transformedMethod = (MethodNode) transformed.methods.get(j);
			// Lets skip empty constructors and initializers
			if (transformedMethod.name.equals("<init>") || transformedMethod.name.equals("<clinit>")) {
				if (transformedMethod.desc.equals("()V")) {
					continue;
				}

			}
			float bestMethodConfidence = -1.f;
			int bestMethodIndex = -1;
			for( int i = 0; i < unassignedMethods.size(); i++ ){

				if (unassignedMethods.get(i).name.equals("<init>") && !transformedMethod.name.equals("<init>")) {
					continue;
				} else if (!unassignedMethods.get(i).name.equals("<init>") && transformedMethod.name.equals("<init>")) {
					continue;
				}

				if (unassignedMethods.get(i).name.equals("<clinit>") && !transformedMethod.name.equals("<clinit>")) {
					continue;
				} else if (!unassignedMethods.get(i).name.equals("<clinit>") && transformedMethod.name.equals("<clinit>")) {
					continue;
				}

				float confidence = bmc.similarity(unassignedMethods.get(i), (MethodNode)transformed.methods.get(j));
				//System.out.println(unassignedMethods.get(i).name + "->" + transformedMethod.name + ":"+(confidence*100) + "%");
				if( confidence > bestMethodConfidence ){
					bestMethodIndex = i;
					bestMethodConfidence = confidence;
				}

			}

			if( bestMethodIndex > -1 ){
				MethodMappings.put(unassignedMethods.get(bestMethodIndex).name+unassignedMethods.get(bestMethodIndex).desc, ((MethodNode)transformed.methods.get(j)).name+((MethodNode)transformed.methods.get(j)).desc);
				unassignedMethods.remove(bestMethodIndex);
				result += bestMethodConfidence;
			}

		}

		result /= ((transformed.methods.size()>0)?transformed.methods.size():1);
		/*
		if (result >= .75) {
			System.out.println(original.name + "<->" + transformed.name);
			System.out.println("Method: " + result * 100 + "%");
		}
		*/

		// Remove 25% if method count is significantly different
		// Add 25% if its good
	//	System.out.println(original.name +"->" + transformed.name + ": " + result);
		if (Math.abs(transformed.methods.size() - original.methods.size()) >= 3 && result >= .5) {
			result -= result * .25;
		}
		else if (transformed.methods.size() == original.methods.size() && result >= .5) {
			// Oops not result *= result * .10
			result *= 1.25;
		}

		/*
		if (result > .01) {
			System.out.println(original.name + "<->" + transformed.name);
			System.out.println("Method (after size): " + result * 100 + "%");
		}
		*/
		double originalFieldCount = original.fields.size();
		double transformedFieldCount = transformed.fields.size();

		double totalFields = original.fields.size() + transformed.fields.size();
		int sharedFields = 0;
		int identicalNames = 0;
		for (FieldNode obj : (List<FieldNode>)original.fields) {
			for (FieldNode obj2 : (List<FieldNode>)transformed.fields) {
				if (org.objectweb.asm.Type.getType(obj.desc).getSort() == org.objectweb.asm.Type.getType(obj2.desc).getSort()) {
					// 10 is an object, object fields are quite commonly the same because different objects match
					if (org.objectweb.asm.Type.getType(obj.desc).getSort() != 10) {
						sharedFields += 2;
					}
					// However identically named object fields are usually the same
					// For Strings check that the types are identical
					if (obj.name.equals(obj2.name)) {
						if (obj.desc.equals("java/lang/String") || obj2.desc.equals("java/lang/String")) {
							if (obj.desc.equals(obj2.desc)) {
								identicalNames += 2;
							}
						} else {
							identicalNames += 2;
						}
					}
				}
			}
		}

		// Identical names are twice as imporant
		// 1:2
		// Heh if its an interface it has no fields...
		if (totalFields != 0) {
			double weightedFieldAverage = (sharedFields + identicalNames * 2) / totalFields;
			//System.out.println(original.name + "->" + transformed.name + ": " + weightedFieldAverage*100 +"%");
			//System.out.println(result * 100 + "%");

			result = (float) ((result + weightedFieldAverage) / 3);
			//System.out.println("result: " + result * 100 + "%");
			//System.out.println();
		}
		// Lets disable this check given the more precise check
		// If identical field count, 10% more
		// If significantly different 25% less
		/*
		if (originalFieldCount == transformedFieldCount) {
			result *= 1.10;
		} else if (Math.abs(originalFieldCount - transformedFieldCount) > 15) {
			result -= result * .50;
		}
		*/

		/*
		if (result > .01) {
			System.out.println(original.name + "<->" + transformed.name);
			System.out.println("Field: " + result * 100 + "%");
		}
		*/

		double totalInterfaces = original.interfaces.size() + transformed.interfaces.size();
		int sharedInterfaces = 0;
		for (String intf : (List<String>) original.interfaces) {
			for (String intf2 : (List<String>) transformed.interfaces) {
				if (intf.equals(intf2)) {
					//System.out.println(original.name + "->" + transformed.name);
					//System.out.println("*" + intf + "->" + intf2);
					sharedInterfaces += 2; // 2 because we will have duplicates
				}
			}
		}

		/*
		if (result > 0.10) {
			System.out.println(original.name + "<->" + transformed.name);
			System.out.println("Interface: " + result * 100 + "%");
		}
		*/

		/*if (original.name.equals(transformed.name)) {
			result += 10;
		}*/
		// Percentage seems to be the field

		if (totalInterfaces != 0) {
			// 20% for single
			// 40% for two or more
			if (sharedInterfaces/totalInterfaces != 0) {
				if (sharedInterfaces == 1) {
					result *= 1.20;
				} else if (sharedInterfaces >= 2) {
					result *= 1.40;
				}

			}
		}

		//if (transformed.name.equals("net/minecraft/command/ICommand")) {
			//System.out.println(original.name + ": " + result);
		//}

		if (result >= 0.1) {
		//	if (result >= 1.2) {
				// This will miss a lot of good matches.
			//	log(new OutputLogger(), LogLevel.INFO, result, MethodMappings, original, transformed);
		//	}
			log(Main.logger, LogLevel.INFO, result, MethodMappings, original, transformed);
		} else {
			// Lets save ourselves time
			// We should always be logging, but use debug level here so it can be ignored.
			// However 0 is not ever helpful
			/*
			if (result != 0){
				log(Main.logger,LogLevel.DEBUG, result, MethodMappings, original, transformed);
			}
			*/

		}

		// Someone divided a second time, which isn't needed
		return result;
	}

	private void log(Log log, LogLevel level, double field, HashMap<String, String> methodMappings, ClassNode original, ClassNode transformed) {
		log.log(level, original.name + "," + transformed.name + "," + field);
	}
}