package edu.illinois.odex.agent.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static edu.illinois.odex.agent.Config.ASM_Version;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author Yicheng Ouyang
 * @Date 9/23/22
 */

public class StateRecorderCV extends ClassVisitor {

    private String currentSlashClassName;

    public StateRecorderCV(ClassVisitor cv, String className) {
        super(ASM_Version, cv);
        currentSlashClassName = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions){
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        if (!"<clinit>".equals(name)){
            mv = new StateAccessRecorderMV(mv);
        }
        return mv;
    }
}

class StateAccessRecorderMV extends MethodVisitor {

    private String currentSlashClassName;
    private String currentMethodName;
    private String currentMethodDesc;

    public StateAccessRecorderMV(MethodVisitor methodVisitor) {
        super(ASM_Version, methodVisitor);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor){
        // Todo: support GETFIELD and PUTFIELD
        if (opcode == GETSTATIC || opcode == PUTSTATIC) {
            mv.visitLdcInsn(opcode);
            mv.visitLdcInsn(owner);
            mv.visitLdcInsn(name);
            mv.visitLdcInsn(descriptor);
            mv.visitFieldInsn(GETSTATIC, owner, name, descriptor);
            mv.visitMethodInsn(INVOKESTATIC, "edu/illinois/odex/agent/app/StateRecorder", "stateAccess",
                    "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V", false);
        }
        mv.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack + 5, maxLocals);
    }
}