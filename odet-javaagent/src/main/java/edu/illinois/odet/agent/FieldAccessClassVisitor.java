package edu.illinois.odet.agent;

import org.objectweb.asm.*;

import static edu.illinois.odet.agent.Config.ASM_Version;

public class FieldAccessClassVisitor extends ClassVisitor {
    private String currentSlashClassName;
    public FieldAccessClassVisitor(ClassVisitor cv, String className) {
        super(ASM_Version, cv);
        currentSlashClassName = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions){
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
//        // exclude <init> and <clinit>
//        if (name.equals("<init>") || name.equals("<clinit>")){
//            return mv;
//        }
//        return new FieldAccessMethodVisitor(mv, currentSlashClassName, name, descriptor, access);
        return mv;
    }
}

class FieldAccessMethodVisitor extends MethodVisitor {
    private String currentSlashClassName;
    private String currentMethodName;
    private String currentMethodDesc;

    public FieldAccessMethodVisitor(MethodVisitor mv, String currentClassName, String methodName, String desc, int access) {
        super(ASM_Version, mv);
        this.currentSlashClassName = currentClassName;
        this.currentMethodName = methodName;
        this.currentMethodDesc = desc;
    }

    @Override
    public void visitCode() {
        mv.visitCode();
    }
}
