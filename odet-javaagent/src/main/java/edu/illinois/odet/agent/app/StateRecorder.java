package edu.illinois.odet.agent.app;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;
import edu.illinois.odet.agent.utils.LogUtils;

import java.util.HashMap;
import java.util.Map;

import static edu.illinois.odet.agent.utils.CommonUtils.getDotClassNameFromTestIdentifier;
import static edu.illinois.odet.agent.utils.CommonUtils.getFieldAccessFlag;
import static edu.illinois.odet.agent.utils.CommonUtils.getFieldIdentifier;
import static edu.illinois.odet.agent.utils.CommonUtils.slashToDotName;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author Yicheng Ouyang
 * @Date 9/22/22
 */

public class StateRecorder {

    public static XStream xs = new XStream();

    private static String currentTestIdentifier = null;

    //                       accessedFields  fieldValue
    private static Map<String, Map<String, Object>> testAccessedFieldsMap = new HashMap<>();

    static {
        xs.addPermission(AnyTypePermission.ANY);
    }

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

    public static void stateAccess(int opcode, String owner, String name, String descriptor, byte value){
        stateAccess(opcode, owner, name, descriptor, (Object) value);
    }
    public static void stateAccess(int opcode, String owner, String name, String descriptor, boolean value){
        stateAccess(opcode, owner, name, descriptor, (Object) value);
    }
    public static void stateAccess(int opcode, String owner, String name, String descriptor, char value){
        stateAccess(opcode, owner, name, descriptor, (Object) value);
    }
    public static void stateAccess(int opcode, String owner, String name, String descriptor, short value){
        stateAccess(opcode, owner, name, descriptor, (Object) value);
    }
    public static void stateAccess(int opcode, String owner, String name, String descriptor, int value){
        stateAccess(opcode, owner, name, descriptor, (Object) value);
    }
    public static void stateAccess(int opcode, String owner, String name, String descriptor, long value){
        stateAccess(opcode, owner, name, descriptor, (Object) value);
    }
    public static void stateAccess(int opcode, String owner, String name, String descriptor, float value){
        stateAccess(opcode, owner, name, descriptor, (Object) value);
    }
    public static void stateAccess(int opcode, String owner, String name, String descriptor, double value){
        stateAccess(opcode, owner, name, descriptor, (Object) value);
    }

    public static void stateAccess(int opcode, String owner, String name, String descriptor, Object value){
        if (currentTestIdentifier == null)
            return;
        String fieldIdentifier = getFieldIdentifier(owner, name, descriptor);
        if (needRecordField(fieldIdentifier)){
            if (!testAccessedFieldsMap.containsKey(currentTestIdentifier)) {
                testAccessedFieldsMap.put(currentTestIdentifier, new HashMap<>());
            }
            // if the field is already recorded as dependent, and its value is also recorded,
            // then return since we only want to record its value before the first access.
            else if (testAccessedFieldsMap.get(currentTestIdentifier).containsKey(fieldIdentifier)){
                return;
            }
            Map<String, Object> fieldValueMap = testAccessedFieldsMap.get(currentTestIdentifier);

            // make a deep copy of the value, otherwise only the reference is recorded (can not detect value change)
            value = xs.fromXML(xs.toXML(value));

            fieldValueMap.put(fieldIdentifier, value);
            LogUtils.agentInfo("[Odet] Recorded Field Access: " + fieldIdentifier);
            System.out.println("[Odet] Recorded Field Access: " + fieldIdentifier);
        }
    }

    public static void checkFieldState(String owner, String name, String descriptor, byte value){
        checkFieldState(owner, name, descriptor, (Object) value);
    }
    public static void checkFieldState(String owner, String name, String descriptor, short value){
        checkFieldState(owner, name, descriptor, (Object) value);
    }
    public static void checkFieldState(String owner, String name, String descriptor, char value){
        checkFieldState(owner, name, descriptor, (Object) value);
    }
    public static void checkFieldState(String owner, String name, String descriptor, int value){
        checkFieldState(owner, name, descriptor, (Object) value);
    }
    public static void checkFieldState(String owner, String name, String descriptor, long value){
        checkFieldState(owner, name, descriptor, (Object) value);
    }
    public static void checkFieldState(String owner, String name, String descriptor, float value){
        checkFieldState(owner, name, descriptor, (Object) value);
    }
    public static void checkFieldState(String owner, String name, String descriptor, double value){
        checkFieldState(owner, name, descriptor, (Object) value);
    }
    public static void checkFieldState(String owner, String name, String descriptor, boolean value){
        checkFieldState(owner, name, descriptor, (Object) value);
    }

    public static void checkFieldState(String owner, String name, String descriptor, Object value){
        if (currentTestIdentifier == null){
            LogUtils.agentErr("[ERROR] currentTestIdentifier == null when try to check field state");
            return;
        }
        String fieldIdentifier = getFieldIdentifier(owner, name, descriptor);
        if (!testAccessedFieldsMap.containsKey(currentTestIdentifier) || !testAccessedFieldsMap.get(currentTestIdentifier).containsKey(fieldIdentifier)){
//            printTestAccessedFieldsMap();
//            System.out.println("testAccessedFieldsMap.containsKey(currentTestIdentifier): " + testAccessedFieldsMap.containsKey(currentTestIdentifier));
//            System.out.println("testAccessedFieldsMap.get(currentTestIdentifier).containsKey(fieldIdentifier): " + testAccessedFieldsMap.get(currentTestIdentifier).containsKey(fieldIdentifier));
//            System.out.println("skip field checking: " + fieldIdentifier);
            return;
        }
        Object originalValue = testAccessedFieldsMap.get(currentTestIdentifier).get(fieldIdentifier);

        if ((originalValue == null && value != null) || (originalValue != null && !originalValue.equals(value))){
            LogUtils.agentInfo(String.format("[IMPORTANT] Value of %s changed after test %s execution!", fieldIdentifier, currentTestIdentifier));
            System.out.println(String.format("[IMPORTANT] Value of %s changed after test %s execution!", fieldIdentifier, currentTestIdentifier));
        } else {
//            System.out.println(String.format("[IMPORTANT] Value of %s does not change after test %s execution!", fieldIdentifier, currentTestIdentifier));
        }
    }

    public static boolean needCheckField(String owner, String name, String descriptor){
        if (currentTestIdentifier == null){
            LogUtils.agentErr("[ERROR] currentTestIdentifier == null when try to check field state");
            return false;
        }
        String fieldIdentifier = getFieldIdentifier(owner, name, descriptor);
        if (testAccessedFieldsMap.get(currentTestIdentifier).containsKey(fieldIdentifier)){
            return true;
        }
        return false;
    }

    private static void printTestAccessedFieldsMap(){
        for (String key: testAccessedFieldsMap.keySet()){
            System.out.println("Test: " + key);
            Map<String, Object> recordedMap = testAccessedFieldsMap.get(key);
            for (String fid: recordedMap.keySet()){
                System.out.println(String.format("    %s: %s", fid, recordedMap.get(fid).toString()));
            }
        }
    }
}
