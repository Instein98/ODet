package edu.illinois.odex.agent.app;

import edu.illinois.odex.agent.utils.LogUtils;

import java.util.HashMap;
import java.util.Map;

import static edu.illinois.odex.agent.utils.CommonUtils.getDotClassNameFromTestIdentifier;
import static edu.illinois.odex.agent.utils.CommonUtils.getFieldAccessFlag;
import static edu.illinois.odex.agent.utils.CommonUtils.getFieldIdentifier;
import static edu.illinois.odex.agent.utils.CommonUtils.slashToDotName;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author Yicheng Ouyang
 * @Date 9/22/22
 */

public class StateRecorder {

    private static String currentTestIdentifier = null;

    //                       accessedFields  fieldValue
    private static Map<String, Map<String, Object>> testAccessedFieldsMap = new HashMap<>();

    // testIdentifier: fullyQualifiedClassName#testMethod
    public static void junitTestStart(String testIdentifier){
//        System.out.println("junit test start: " + testIdentifier);
        currentTestIdentifier = testIdentifier;
    }

    public static void junitTestFinish(String testIdentifier){
//        System.out.println("junit test finish: " + testIdentifier);
        currentTestIdentifier = null;
    }

    // Only record public static fields and fields in the current test methods
    public static boolean needRecordField(String fieldIdentifier){
        if (currentTestIdentifier == null)
            return false;
        String[] tmp = fieldIdentifier.split("[#]");
        String fieldOwner = slashToDotName(tmp[0]);
        int access = getFieldAccessFlag(fieldIdentifier);
        return fieldOwner.equals(getDotClassNameFromTestIdentifier(currentTestIdentifier))
                || ((access & ACC_PUBLIC) != 0 && (access & ACC_STATIC) != 0);
    }

    public static void stateAccess(int opcode, String owner, String name, String descriptor, Object value){
        if (currentTestIdentifier == null)
            return;
//        String fieldType = null;  // "STATIC" or "FIELD"
//        if (opcode == GETSTATIC || opcode == PUTSTATIC){
//            fieldType = "STATIC";  // GETSTATIC or PUTSTATIC
//        } else if (opcode == GETFIELD || opcode == PUTFIELD){
//            fieldType = "FIELD";  // GETFIELD or PUTFIELD
//        } else {
//            LogUtils.agentInfo("Unknown opcode: " + opcode);
//            return;
//        }
        String fieldIdentifier = getFieldIdentifier(owner, name, descriptor);
//        if (!fieldAccessFlagMap.containsKey(fieldIdentifier)){
//            LogUtils.agentInfo("Field " + fieldIdentifier + " not found!!!");
//            return;
//        }
        if (needRecordField(fieldIdentifier)){
            LogUtils.agentInfo("Recorded Field Access: " + fieldIdentifier);
            System.out.println("Recorded Field Access: " + fieldIdentifier);
            if (!testAccessedFieldsMap.containsKey(fieldIdentifier)){
                Map<String, Object> fieldValueMap = new HashMap<>();
                fieldValueMap.put(fieldIdentifier, value);
                testAccessedFieldsMap.put(fieldIdentifier, fieldValueMap);
            } else {
                Map<String, Object> fieldValueMap = testAccessedFieldsMap.get(fieldIdentifier);
                // do nothing if the field state is already recorded in the test execution
                if (!fieldValueMap.containsKey(fieldIdentifier)){
                    fieldValueMap.put(fieldIdentifier, value);
                }
            }
        }
    }
}
