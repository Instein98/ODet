package edu.illinois.odet.agent.app;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;
import edu.illinois.odet.agent.utils.FileUtils;
import edu.illinois.odet.agent.utils.LogUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // testAccessedFieldsMap only stores tests having pollution
    //                       accessedFields  fieldValue
    private static Map<String, Map<String, Object>> testAccessedFieldsMap = new HashMap<>();

    private static Map<String, Map<String, Object>> testPollutedFieldsMap = new HashMap<>();

//    private static String WOKR_DIR_PATH = System.getProperty("user.home") + "/.odet/";
    private static String WOKR_DIR_PATH = System.getProperty("user.dir") + "/odet/";
    private static String OUTPUT_POLLUTION_INFO_PATH = WOKR_DIR_PATH + "/pollutionInfo.json";
    private static String OUTPUT_ACCESS_INFO_PATH = WOKR_DIR_PATH + "/accessInfo.json";

    private static String WOKR_DIR_SERIALIZATION_PATH = WOKR_DIR_PATH + "/objects/";

    private static int objId = 1;

    private static void dumpInfo(){
        // dump pollution information and serialize
        JSONObject pollutionJsonObj = new JSONObject();
        for (String testId: testPollutedFieldsMap.keySet()){
            Map<String, Object> pollutedFieldMap = testPollutedFieldsMap.get(testId);
            JSONObject fieldSerializationJsonObj = new JSONObject();
            for (String fieldId: pollutedFieldMap.keySet()){
                Object value = pollutedFieldMap.get(fieldId);
                String serializationPath = serializeObject(value);
                fieldSerializationJsonObj.put(fieldId, serializationPath);
            }
            pollutionJsonObj.put(testId, fieldSerializationJsonObj);
        }
        FileUtils.clear(OUTPUT_POLLUTION_INFO_PATH);
        FileUtils.write(OUTPUT_POLLUTION_INFO_PATH, pollutionJsonObj.toJSONString());
        // dump access information
        JSONObject accessJsonObj = new JSONObject();
        for (String testId: testAccessedFieldsMap.keySet()){
            Map<String, Object> accessedFieldMap = testAccessedFieldsMap.get(testId);
            JSONArray accessedArr = new JSONArray();
            for (String fieldAccessed: accessedFieldMap.keySet()){
                accessedArr.add(fieldAccessed);
            }
            accessJsonObj.put(testId, accessedArr);
        }
        FileUtils.clear(OUTPUT_ACCESS_INFO_PATH);
        FileUtils.write(OUTPUT_ACCESS_INFO_PATH, accessJsonObj.toJSONString());
    }

    private static String serializeObject(Object value){
        String s = xs.toXML(value);
        System.out.println("Before serializaiton: " + value.toString());
        System.out.println("After serializaiton: " + s);
        String serializationPath = String.format(WOKR_DIR_SERIALIZATION_PATH + "/%d.xml", objId);
        FileUtils.clear(serializationPath);
        FileUtils.write(serializationPath, s);
        objId++;
        return serializationPath;
    }

    static {
        xs.addPermission(AnyTypePermission.ANY);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            printMap(testPollutedFieldsMap);
            dumpInfo();
        }));
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
            // Todo (optimization): use array to make it faster, because such case may be a lot
            else if (testAccessedFieldsMap.get(currentTestIdentifier).containsKey(fieldIdentifier)){
                return;
            }
            Map<String, Object> fieldValueMap = testAccessedFieldsMap.get(currentTestIdentifier);

//            if (value.getClass().getName().contains("Person")){
//                System.out.println("Access: " + value.toString());
//            }

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
        // fieldIdentifier example: org/example/Test#name#Lorg/example/Name;
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
            LogUtils.agentInfo(String.format("[Odet] Value of %s changed after test %s execution!",
                    fieldIdentifier.substring(0, fieldIdentifier.lastIndexOf("#")), currentTestIdentifier));
            System.out.println(String.format("[Odet] Value of %s changed after test %s execution!",
                    fieldIdentifier.substring(0, fieldIdentifier.lastIndexOf("#")), currentTestIdentifier));
            if (!testPollutedFieldsMap.containsKey(currentTestIdentifier)){
                testPollutedFieldsMap.put(currentTestIdentifier, new HashMap<>());
            }
            // otherwise the value may change after later test execution
            value = xs.fromXML(xs.toXML(value));
            testPollutedFieldsMap.get(currentTestIdentifier).put(fieldIdentifier, value);
        } else {
//            if (value.getClass().getName().contains("Person")){
//                System.out.println("Before: " + originalValue.toString());
//                System.out.println("After: " + value.toString());
//            }
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

    private static void printMap(Map<String, Map<String, Object>> map){
        for (String key: map.keySet()){
            System.out.println("Test: " + key);
            Map<String, Object> recordedMap = map.get(key);
            for (String fid: recordedMap.keySet()){
                System.out.println(String.format("    %s: %s", fid, recordedMap.get(fid).toString()));
            }
        }
    }

    public static boolean isTestPollutingField(String testId, String simplifiedFieldId){
        if (testPollutedFieldsMap.containsKey(testId)){
            Map<String, Object> fieldMap = testPollutedFieldsMap.get(testId);
            boolean matched = false;
            for (String key: fieldMap.keySet()){
                String[] tmp = key.split("[#]");
                String owner = tmp[0];
                String fieldName = tmp[1];
                String desc = tmp[2];
                String sid1 = owner + '#' + fieldName;
                String sid2 = owner.replace("/", ".") + '#' + fieldName;
//                System.out.println("simplifiedFieldId: " + simplifiedFieldId);
//                System.out.println("sid1: " + sid1);
//                System.out.println("sid2: " + sid2);
                if (sid1.equals(simplifiedFieldId) || sid2.equals(simplifiedFieldId)){
                    matched = true;
                    break;
                }
            }
            return matched;
        }
        return false;
    }

    private static Map<String, List> assertPollutionMap = new HashMap<>();
    private static Map<String, List> assertNoPollutionMap = new HashMap<>();

    public static void recordAssertPollutionInvocation(String testId, String fieldId){
        if (!assertPollutionMap.containsKey(testId)){
            assertPollutionMap.put(testId, new ArrayList());
        }
        assertPollutionMap.get(testId).add(fieldId);
    }
    public static void recordAssertNoPollutionInvocation(String testId, String fieldId){
        if (!assertNoPollutionMap.containsKey(testId)){
            assertNoPollutionMap.put(testId, new ArrayList());
        }
//        System.out.println(String.format("assertNoPollutionMap.put(%s, %s)", testId, fieldId));
        assertNoPollutionMap.get(testId).add(fieldId);
//        System.out.println("assertNoPollutionMap.size(): " + assertNoPollutionMap.size());
    }

    public static void assertPollution(){
        for (Map.Entry<String, List> entry: assertPollutionMap.entrySet()){
            List<String> fieldList = entry.getValue();
            for (String fid: fieldList) {
                assertTestPollutesField(entry.getKey(), fid);
            }
        }
        assertPollutionMap.clear();
    }

    public static void assertNoPollution(){
        for (Map.Entry<String, List> entry: assertNoPollutionMap.entrySet()){
            List<String> fieldList = entry.getValue();
            for (String fid: fieldList) {
                assertTestNotPollutesField(entry.getKey(), fid);
            }
        }
        assertNoPollutionMap.clear();
    }

    private static void assertTestPollutesField(String testId, String simplifiedFieldId){
//        printMap(testPollutedFieldsMap);
        if (!StateRecorder.isTestPollutingField(testId, simplifiedFieldId)){
            throw new RuntimeException(String.format("%s is not polluted by %s!", simplifiedFieldId, testId));
        } else {
            System.out.printf("[PASS] %s is polluted by %s%n", simplifiedFieldId, testId);
        }
    }

    private static void assertTestNotPollutesField(String testId, String simplifiedFieldId){
        if (StateRecorder.isTestPollutingField(testId, simplifiedFieldId)){
            throw new RuntimeException(String.format("%s is polluted by %s!", simplifiedFieldId, testId));
        } else {
            System.out.printf("[PASS] %s is not polluted by %s%n", simplifiedFieldId, testId);
        }
    }
}
