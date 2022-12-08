package edu.illinois.odet.agent.utils;

import org.objectweb.asm.ClassReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Yicheng Ouyang
 * @Date 9/25/22
 */

public class CommonUtils {

    public static final String STATE_RECORDER = "edu/illinois/odet/agent/app/StateRecorder";

    private static Map<String, Integer> fieldAccessFlagMap = new HashMap<>();

    public static String getFieldIdentifier(int accessFlag, String owner, String name, String desc){
        return accessFlag + "#" + owner + "#" + name + "#" + desc;
    }

    public static String slashToDotName(String slashName){
        return slashName.replace('/', '.');
    }

    public static String getDotClassNameFromTestIdentifier(String testId){
        return testId.split("[#]")[0];
    }

    public static int getFieldAccessFlag(String fieldIdWithoutFlag){
        if (fieldIdWithoutFlag == null || !fieldAccessFlagMap.containsKey(fieldIdWithoutFlag))
            return 0;
        return fieldAccessFlagMap.get(fieldIdWithoutFlag);
    }

    public static void putFieldAccessFlag(String fieldId, int access){
            fieldAccessFlagMap.put(fieldId, access);
    }

    public static Set<String> getFieldIds(){
        return fieldAccessFlagMap.keySet();
    }

    public static void printRecordedFieldsInfo(){
        for (String key: fieldAccessFlagMap.keySet()){
            System.out.println(key + ": " + fieldAccessFlagMap.get(key));
        }
    }

    public static boolean matchPrefix(String slashClassName, Set<String> prefixSet){
        if (prefixSet == null || prefixSet.size() == 0) return true;
        for (String prefix: prefixSet){
            if (slashClassName.startsWith(prefix)){
                return true;
            }
        }
        return false;
    }

    public static int getClassVersion(ClassReader cr) {
        return cr.readUnsignedShort(6);
    }
}
