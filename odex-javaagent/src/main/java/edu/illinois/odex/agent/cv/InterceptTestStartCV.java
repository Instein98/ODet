package edu.illinois.odex.agent.cv;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static edu.illinois.odex.agent.Config.ASM_Version;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author Yicheng Ouyang
 * @Date 9/16/22
 */

public class InterceptTestStartCV extends ClassVisitor {

    private String currentSlashClassName;

    public InterceptTestStartCV(ClassVisitor cv, String className) {
        super(ASM_Version, cv);
        currentSlashClassName = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions){
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        return new InterceptTestStartMV(mv, currentSlashClassName, name, descriptor, access);
    }
}

class InterceptTestStartMV extends MethodVisitor {

    private String currentSlashClassName;
    private String currentMethodName;
    private String currentMethodDesc;

    public InterceptTestStartMV(MethodVisitor mv, String currentClassName, String methodName, String desc, int access) {
        super(ASM_Version, mv);
        this.currentSlashClassName = currentClassName;
        this.currentMethodName = methodName;
        this.currentMethodDesc = desc;
    }

    @Override
    public void visitCode() {
        mv.visitCode();
//        "org/junit/runner/notification/RunNotifier",  // junit4: public void fireTestStarted(final Description description)
//        "junit/textui/TestRunner",  // junit3: public void testStarted(String testName),
//        "org/junit/platform/runner/JUnitPlatformRunnerListener",  // junit5: public void executionStarted(TestIdentifier testIdentifier)
//        "org/junit/vintage/engine/execution/RunListenerAdapter"  // junit5: public void testRunStarted(Description description)
        // Currently working for junit3 & junit4. Todo: instrument for junit5
        if ("org/junit/runner/notification/RunNotifier".equals(currentSlashClassName)
                && "fireTestStarted".equals(currentMethodName)
                && "(Lorg/junit/runner/Description;)V".equals(currentMethodDesc)){
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/junit/runner/Description", "getClassName", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn("#");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/junit/runner/Description", "getMethodName", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }

        // not working
//        else if ("org/junit/platform/launcher/core/CompositeTestExecutionListener".equals(currentSlashClassName)
//                && "executionStarted".equals(currentMethodName)
//                && "(Lorg/junit/platform/launcher/TestIdentifier;)V".equals(currentMethodDesc)){
//            LogUtils.agentInfo("Instrumenting org/junit/platform/launcher/core/CompositeTestExecutionListener");
//            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//            mv.visitVarInsn(ALOAD, 1);
//            mv.visitMethodInsn(INVOKEVIRTUAL, "org/junit/platform/launcher/TestIdentifier", "getDisplayName", "()Ljava/lang/String;", false);
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
//        } else if ("org/junit/vintage/engine/execution/RunListenerAdapter".equals(currentSlashClassName)
//                && "testStarted".equals(currentMethodName)
//                && "(Lorg/junit/runner/Description;)V".equals(currentMethodDesc)){
//            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//            mv.visitInsn(DUP);
//            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
//            mv.visitVarInsn(ALOAD, 1);
//            mv.visitMethodInsn(INVOKEVIRTUAL, "org/junit/runner/Description", "getClassName", "()Ljava/lang/String;", false);
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
//            mv.visitLdcInsn("#");
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
//            mv.visitVarInsn(ALOAD, 1);
//            mv.visitMethodInsn(INVOKEVIRTUAL, "org/junit/runner/Description", "getMethodName", "()Ljava/lang/String;", false);
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
//        }


        // instrumenting org/junit/runner/notification/RunNotifier can already work for junit 3 tests!
//        else if ("junit/textui/TestRunner".equals(currentSlashClassName)
//                && "testStarted".equals(currentMethodName)
//                && "(Ljava/lang/String;)V".equals(currentMethodDesc)){
//            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//            mv.visitVarInsn(ALOAD, 1);
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
//        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack+3, maxLocals);
    }
}
