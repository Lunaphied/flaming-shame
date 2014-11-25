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
import org.objectweb.asm.tree.MethodNode;

import deobfuscator.ClassComparator;

public class BasicClassComparator implements ClassComparator {

	@Override
	public float similarity(ClassNode original, ClassNode transformed) {
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
			unassignedMethods.add(new MethodNode(currentNode.access,currentNode.name,currentNode.desc,currentNode.signature,exceptions_s));
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

		// If identical field count, 10% more
		// If significantly different 25% less
		if (originalFieldCount == transformedFieldCount) {
			result *= 1.10;
		} else if (Math.abs(originalFieldCount - transformedFieldCount) > 15) {
			result -= result * .50;
		}

		/*
		if (result > .01) {
			System.out.println(original.name + "<->" + transformed.name);
			System.out.println("Field: " + result * 100 + "%");
		}
		*/

		// 10% bonus for some interface magic
		double totalInterfaces = original.interfaces.size() + transformed.interfaces.size();
		int sharedInterfaces = 0;
		for (Object obj : original.interfaces) {
			for (Object obj2 : transformed.interfaces) {
				if (obj.getClass().getName().equals(obj2.getClass().getName())) {
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
			// 10% for single
			// 20% for two or more
			if (sharedInterfaces/totalInterfaces != 0) {
				if (sharedInterfaces == 1) {
					result *= 1.10;
				} else if (sharedInterfaces >= 2) {
					result *= 1.20;
				}

			}
		}

		if (transformed.name.equals("net/minecraft/command/ICommand")) {
			//System.out.println(original.name + ": " + result);
		}

		if (result >= 1.1) {
			log(new OutputLogger(),LogLevel.INFO, result, MethodMappings, original, transformed);
			log(Main.logger, LogLevel.INFO, result, MethodMappings, original, transformed);
		} else {
			// We should always be logging, but use debug level here so it can be ignored.
			// However 0 is not ever helpful
			if (result != 0){
				log(Main.logger,LogLevel.DEBUG, result, MethodMappings, original, transformed);
			}

		}

		// Someone divided a second time, which isn't needed
		return result;
	}

	private void log(Log log, LogLevel level, double field, HashMap<String, String> methodMappings, ClassNode original, ClassNode transformed) {
		log.log(level, original.name + "," + transformed.name + "," + field);
	}
}