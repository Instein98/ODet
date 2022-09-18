package edu.illinois.odex.agent.cv;

import com.google.common.collect.Sets;
import edu.illinois.odex.agent.FieldAccessClassVisitor;
import edu.illinois.odex.agent.FieldAccessMethodVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Set;

import static edu.illinois.odex.agent.Config.ASM_Version;

/**
 * @author Yicheng Ouyang
 * @Date 9/16/22
 */

public class InterceptTestStartCV extends ClassVisitor {

    private String currentSlashClassName;

    boolean shouldInst = false;

    public static Set<String> instClassPrefix = Sets.newHashSet(
            "org/junit/runner/notification/RunNotifier",  // junit4: public void fireTestStarted(final Description description)
            "junit/textui/TestRunner",  // junit3: public void testStarted(String testName),
            "org/junit/platform/runner/JUnitPlatformRunnerListener",  // junit5: public void executionStarted(TestIdentifier testIdentifier)
            "org/junit/vintage/engine/execution/RunListenerAdapter"  // junit5: public void testRunStarted(Description description)
    );

    public InterceptTestStartCV(ClassVisitor cv, String className) {
        super(ASM_Version, cv);
        currentSlashClassName = className;
        for(String prefix: instClassPrefix){
            if (currentSlashClassName.startsWith(prefix)){
                shouldInst = true;
                break;
            }
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions){
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        // exclude <init> and <clinit>
        if (name.equals("<init>") || name.equals("<clinit>")){
            return mv;
        }
        return new InterceptTestStartMV(mv, currentSlashClassName, name, descriptor, access);
    }
}
