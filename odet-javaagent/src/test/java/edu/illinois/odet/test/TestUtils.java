package edu.illinois.odet.test;

/**
 * @author Yicheng Ouyang
 * @Date 10/23/22
 */

public class TestUtils {

    public static void assertTestPollutesField(String testId, String fieldId){
        throw new RuntimeException("Odet javaagent is not attached!");
        // This method invocation will be handled by the odet javaagent:
        // (1) The current invocation will be redirect to the invocation of StateRecorder#assertTestPollutesField
        // (2) The invocation will be moved to the point after odet javaagent checking pollution
    }

    public static void assertTestNotPollutesField(String testId, String fieldId){
        throw new RuntimeException("Odet javaagent is not attached!");
        // This method invocation will be handled by the odet javaagent:
        // (1) The current invocation will be redirect to the invocation of StateRecorder#assertTestPollutesField
        // (2) The invocation will be moved to the point after odet javaagent checking pollution
    }

}
