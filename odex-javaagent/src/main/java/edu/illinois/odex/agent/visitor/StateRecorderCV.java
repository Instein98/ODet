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
            mv = new InterceptJunitTestEventMV(mv, currentSlashClassName, name, descriptor, access);
        }
        return mv;
    }
}

class StateAccessRecorderMV extends MethodVisitor {

    private String currentSlashClassName;
    private String currentMethodName;
    private String currentMethodDesc;

    public StateAccessRecorderMV(int api, MethodVisitor methodVisitor) {
        super(api, methodVisitor);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor){
        if (opcode == GETSTATIC) {

        }
        mv.visitFieldInsn(opcode, owner, name, descriptor);
    }
}