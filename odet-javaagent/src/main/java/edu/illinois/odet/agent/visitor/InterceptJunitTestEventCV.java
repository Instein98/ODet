package edu.illinois.odet.agent.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static edu.illinois.odet.agent.Config.ASM_Version;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author Yicheng Ouyang
 * @Date 9/16/22
 */

public class InterceptJunitTestEventCV extends ClassVisitor {

    private String currentSlashClassName;

    public InterceptJunitTestEventCV(ClassVisitor cv, String className) {
        super(ASM_Version, cv);
        currentSlashClassName = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions){
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        return new InterceptJunitTestEventMV(mv, currentSlashClassName, name, descriptor, access);
    }
}

class InterceptJunitTestEventMV extends MethodVisitor {

    private String currentSlashClassName;
    private String currentMethodName;
    private String currentMethodDesc;

    public InterceptJunitTestEventMV(MethodVisitor mv, String currentClassName, String methodName, String desc, int access) {
        super(ASM_Version, mv);
        this.currentSlashClassName = currentClassName;
        this.currentMethodName = methodName;
        this.currentMethodDesc = desc;
    }

    @Override
    public void visitCode() {
        mv.visitCode();
        // Currently working for junit3 & junit4. Todo: instrument for junit5
        if ("org/junit/runner/notification/RunNotifier".equals(currentSlashClassName)
                && "fireTestStarted".equals(currentMethodName)
                && "(Lorg/junit/runner/Description;)V".equals(currentMethodDesc)){
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
            mv.visitMethodInsn(INVOKESTATIC, "edu/illinois/odet/agent/app/StateRecorder", "junitTestStart", "(Ljava/lang/String;)V", false);
        } else if ("org/junit/runner/notification/RunNotifier".equals(currentSlashClassName)
                && "fireTestFinished".equals(currentMethodName)
                && "(Lorg/junit/runner/Description;)V".equals(currentMethodDesc)){
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
            mv.visitMethodInsn(INVOKESTATIC, "edu/illinois/odet/agent/app/StateRecorder", "junitTestFinish", "(Ljava/lang/String;)V", false);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack+3, maxLocals);
    }
}
