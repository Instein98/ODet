package edu.illinois.odex.agent.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Yicheng Ouyang
 * @Date 9/25/22
 */

public class CommonUtils {

    public static final String STATE_RECORDER = "edu/illinois/odex/agent/app/StateRecorder";

    private static Map<String, Integer> fieldAccessFlagMap = new HashMap<>();

    public static String getFieldIdentifier(String owner, String name, String desc){
        return owner + "#" + name + "#" + desc;
    }

    public static String slashToDotName(String slashName){
        return slashName.replace('/', '.');
    }

    public static String getDotClassNameFromTestIdentifier(String testId){
        return testId.split("[#]")[0];
    }

    public static int getFieldAccessFlag(String fieldId){
        if (fieldId == null || !fieldAccessFlagMap.containsKey(fieldId))
            return 0;
        return fieldAccessFlagMap.get(fieldId);
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
}
