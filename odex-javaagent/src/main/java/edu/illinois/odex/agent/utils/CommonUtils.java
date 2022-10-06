package edu.illinois.odex.agent.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yicheng Ouyang
 * @Date 9/25/22
 */

public class CommonUtils {

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
}
