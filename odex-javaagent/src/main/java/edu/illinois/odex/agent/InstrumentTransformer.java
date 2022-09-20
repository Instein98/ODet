package edu.illinois.odex.agent;

import com.google.common.collect.Sets;
import edu.illinois.odex.agent.cv.InterceptTestStartCV;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Set;

import static edu.illinois.odex.agent.Premain.thirdPartyPrefixWhiteList;

public class InstrumentTransformer implements ClassFileTransformer {

    private String transformerName = "InstrumentTransformer";

    Set<String> PREFIX_BLACK_LIST = Sets.newHashSet(
            "edu/illinois/odex",
            "java",
            "sun",
            "com/sun",
            "javax",
            "jdk"
    );

    public static Set<String> junitInstPrefixes = Sets.newHashSet(
            "org/junit/runner/notification/RunNotifier"  // junit4: public void fireTestStarted(final Description description)
//            "junit/textui/TestRunner",  // junit3: public void testStarted(String testName),
//            "org/junit/platform/runner/JUnitPlatformRunnerListener",  // junit5: public void executionStarted(TestIdentifier testIdentifier)
//            "org/junit/vintage/engine/execution/RunListenerAdapter",  // junit5: public void testRunStarted(Description description)
//            "org/junit/platform/launcher/core/CompositeTestExecutionListener"
    );

    private Set<String> PREFIX_WHITE_LIST = Premain.prefixWhiteList;

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws
            IllegalClassFormatException {
        byte[] result = classfileBuffer;

        if (className == null){
            return result;
        }

        if (PREFIX_WHITE_LIST == null || PREFIX_WHITE_LIST.size() == 0) {
            if (matchPrefix(className, PREFIX_BLACK_LIST)) return result;
        } else {
            if (!matchPrefix(className, PREFIX_WHITE_LIST) && !matchPrefix(className, thirdPartyPrefixWhiteList))
                return result;
        }

        try{
            ClassReader cr = new ClassReader(result);
            ClassWriter cw = new ClassWriter(cr, 0);
            ClassVisitor cv = new FieldAccessClassVisitor(cw, className);
            if (matchPrefix(className, junitInstPrefixes)){
                cv = new InterceptTestStartCV(cv, className);
            }

            cr.accept(cv, ClassReader.EXPAND_FRAMES);

            result = cw.toByteArray();

//            FileUtils.write(Config.workingDirectory() + "/" + transformerName + "/"
//                    + className.replace('/', '.') + ".class", result);

        } catch (Throwable t){
            LogUtils.agentErr(t);
//            t.printStackTrace();
        }

        return result;
    }

    private boolean matchPrefix(String slashClassName, Set<String> prefixSet){
        for (String prefix: prefixSet){
            if (slashClassName.startsWith(prefix)){
                return true;
            }
        }
        return false;
    }
}
