package deobfuscator.comparators;

import java.util.*;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import deobfuscator.MethodComparator;

public class BasicMethodComparator extends MethodComparator {
	
	private static final float METHOD_PARAMETER_CONFIDENCE_PADDING = 0.f;

	@Override
	public float similarity(MethodNode a, MethodNode b) {
		// Get the return type and argument types and frequencies of the unobfuscated method
		HashMap<Type, Integer> A_MethodParameters = GetMethodSignatureStatistics(a);
		Type A_ReturnType = Type.getReturnType(a.desc);
		
		HashMap<Type, Integer> B_MethodParameters = GetMethodSignatureStatistics(b);
		Type B_ReturnType = Type.getReturnType(b.desc);

		// Special method names
		if (!a.name.equals(b.name)) {
			if (a.name.equals("values") || b.name.equals("values") ||
					a.name.equals("valueOf") || b.name.equals("valueOf") ||
					a.name.equals("toString") || b.name.equals("toString") ||
					a.name.equals("<clinit>") || b.name.equals("<clinit>") ||
					a.name.equals("<init>") || b.name.equals("<init>")) {
				return 0f;
			}
		}

		// If the return types are different, then we know that this is not a potential match,
		// not return type per sya, just return category, not objects
		// So it makes sense
		if( A_ReturnType.getSort() != B_ReturnType.getSort() ){
			return 0.f;
		}

		// Special class names
		if (!A_ReturnType.getClassName().equals(B_ReturnType.getClassName())) {
			if (A_ReturnType.getClassName().equals("java.lang.String") || B_ReturnType.getClassName().equals("java.lang.String")) {
				return 0f;
				//System.out.println(A_ReturnType.getClassName() + "=/=" + B_ReturnType.getClassName());
			}
		}
		// A more sane conclusion, different argument counts are different methods
		if (A_MethodParameters.size() != B_MethodParameters.size()) {
			return 0.f;
		}

		//System.out.println(a.name + "<->" + b.name);
		//System.out.println(Enum.class.isAssignableFrom(A_ReturnType.getClass()));

		// Compute a "method confidence" that the two methods being compared are the same, starting at 1.0 and decreasing
		// as the number of dissimilarities increases
		float MethodConfidence = 1.f;

		// TODO: Simplify and make this just check for same parameters
		// possibly allow differing parameters
		// Build a list of all parameter types
		Set<Type> AllParameterTypes = new TreeSet<Type>(new Comparator<Type>(){
			@Override
			/*
			public int compare(Type o1, Type o2) {
				return o1.getDescriptor().compareTo(o2.getDescriptor());
			}
			*/
			public int compare(Type o1, Type o2) {
				return Integer.compare(o1.getSort(),o2.getSort());
			}
		});
		AllParameterTypes.addAll(A_MethodParameters.keySet());
		AllParameterTypes.addAll(B_MethodParameters.keySet());
		
		// Iterate over the parameters, multiplying through by the ratio of the number of same-type parameters
		// in A and B
		for( Type ParameterType : AllParameterTypes ){
			if( A_MethodParameters.containsKey(ParameterType) && B_MethodParameters.containsKey(ParameterType) ){
				float A_Ct = (float)A_MethodParameters.get(ParameterType)+METHOD_PARAMETER_CONFIDENCE_PADDING;
				float B_Ct = (float)B_MethodParameters.get(ParameterType)+METHOD_PARAMETER_CONFIDENCE_PADDING;
				MethodConfidence *= (A_Ct>=B_Ct)?(B_Ct/A_Ct):(A_Ct/B_Ct);
			}/* else if( A_MethodParameters.containsKey(ParameterType) ) {
				MethodConfidence *= (METHOD_PARAMETER_CONFIDENCE_PADDING/(A_MethodParameters.get(ParameterType)+METHOD_PARAMETER_CONFIDENCE_PADDING));
			} else {
				MethodConfidence *= (METHOD_PARAMETER_CONFIDENCE_PADDING/(B_MethodParameters.get(ParameterType)+METHOD_PARAMETER_CONFIDENCE_PADDING));
			}*/
		}

		// This is the INSN instruction count check
		// a very important reassurance check, however occasionally it may need to be disabled when huge updates happen
		if (MethodConfidence >= .7) {
			double instructionDiff = Math.abs(a.instructions.size() - b.instructions.size());
			/*
			System.out.println();
			System.out.println(a.name + "->" + b.name);
			System.out.println(MethodConfidence * 100 + "%");
			*/
			if (instructionDiff== 0) {
				// High score plus identical instruction count? Almost certainly identical.
				//System.out.println("Insn count identical!");
				return 1;
			} else if (instructionDiff<= 10) {
				//System.out.println("Insn count within 10!");
				// No negative bonuses
				if (MethodConfidence < .95) {
					return .95f;
				}
			} else if (instructionDiff<= 30) {
				//System.out.println("Insn count within 30!");
				if (MethodConfidence < .85) {
					return .85f;
				}
			} else if (instructionDiff >= 50 && instructionDiff < 100) {
				//System.out.println("Insn count between 50 and 100");
				// Fairly unlikely
				return .7f;
			} else if (instructionDiff >= 100) {
				//System.out.println("Insn count greater than 100");
				// 100 new opcodes? Possible but unlikely
				return .4f;
			}
		}

		return MethodConfidence;
			
	}

}
