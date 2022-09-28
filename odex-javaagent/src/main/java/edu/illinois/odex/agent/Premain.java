package edu.illinois.odex.agent;

import com.google.common.collect.Sets;
import edu.illinois.odex.agent.utils.LogUtils;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;

public class Premain {

    public static Set<String> prefixWhiteList = new HashSet<>();
    public static Set<String> thirdPartyPrefixWhiteList = Sets.newHashSet(
            "org/junit/runner/notification/RunNotifier"  // junit4: public void fireTestStarted(final Description description)
//            "junit/textui/TestRunner",  // junit3: public void testStarted(String testName),
//            "org/junit/platform/runner/JUnitPlatformRunnerListener",  // junit5: public void executionStarted(TestIdentifier testIdentifier)
//            "org/junit/vintage/engine/execution/RunListenerAdapter",  // junit5: public void testRunStarted(Description description)
//            "org/junit/platform/launcher/core/CompositeTestExecutionListener"
    );

    public static void premain(String options, Instrumentation ins) {
        LogUtils.agentInfo("******** Premain Start ********\n");
        parseArgs(options);
        ins.addTransformer(new InstrumentTransformer());
    }

    public static void parseArgs(String args){
        if (args == null || args.equals("")){
            return;
        }
        for (String argPair: args.split(";")){
            String[] kv = argPair.split("=");
            String key = kv[0];
            String value = kv[1];
            if (key.equals("instPrefix")){
                for (String prefix: value.split(",")){
                    prefixWhiteList.add(prefix);
                }
            }
        }
    }
}
