package edu.illinois.odex.agent.app;

import edu.illinois.odex.agent.LogUtils;

import java.util.HashMap;
import java.util.Map;

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
        System.out.println("junit test start: " + testIdentifier);
        currentTestIdentifier = testIdentifier;
    }

    public static void junitTestFinish(String testIdentifier){
        System.out.println("junit test finish: " + testIdentifier);
        currentTestIdentifier = null;
    }

    public static void stateAccess(int opcode, String owner, String name, String descriptor, Object value){
        if (currentTestIdentifier == null)
            return;
        String fieldType = null;  // "STATIC" or "FIELD"
        if (opcode == 178 || opcode == 179){
            fieldType = "STATIC";  // GETSTATIC or PUTSTATIC
        } else if (opcode == 180 || opcode == 181){
            fieldType = "FIELD";  // GETFIELD or PUTFIELD
        } else {
            LogUtils.agentInfo("Unknown opcode: " + opcode);
            return;
        }
        String fieldIdentifier = fieldType + "#" + owner + "#" + name + "#" + descriptor;
        if (!testAccessedFieldsMap.containsKey(fieldIdentifier)){
            Map<String, Object> fieldValueMap = new HashMap<>();
            fieldValueMap.put(fieldIdentifier, value);
            testAccessedFieldsMap.put(fieldIdentifier, fieldValueMap);
        } else {
            Map<String, Object> fieldValueMap = testAccessedFieldsMap.get(fieldIdentifier);
            // do nothing if the field state is already recorded
            if (!fieldValueMap.containsKey(fieldIdentifier)){
                fieldValueMap.put(fieldIdentifier, value);
            }
        }
    }
}
