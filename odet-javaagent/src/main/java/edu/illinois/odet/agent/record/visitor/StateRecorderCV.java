package edu.illinois.odet.agent.record.visitor;

import edu.illinois.odet.agent.Config;
import edu.illinois.odet.agent.utils.CommonUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author Yicheng Ouyang
 * @Date 9/23/22
 */

public class StateRecorderCV extends ClassVisitor {

    private String currentSlashClassName;

    public StateRecorderCV(ClassVisitor cv, String className) {
        super(Config.ASM_Version, cv);
        currentSlashClassName = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions){
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        // exclude <clinit> because it happens during loading instead of test execution
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
        super(Config.ASM_Version, methodVisitor);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor){
        // Todo: support GETFIELD and PUTFIELD
        if (opcode == GETSTATIC || opcode == PUTSTATIC) {
            int accessFlag = CommonUtils.getFieldAccessFlag(String.format("%s#%s#%s", owner, name, descriptor));
            mv.visitLdcInsn(accessFlag);
            mv.visitLdcInsn(owner);
            mv.visitLdcInsn(name);
            mv.visitLdcInsn(descriptor);
            mv.visitFieldInsn(GETSTATIC, owner, name, descriptor);
            if (descriptor.equals("C") || descriptor.equals("S") || descriptor.equals("I") || descriptor.equals("J") || descriptor.equals("F") || descriptor.equals("D") || descriptor.equals("Z") || descriptor.equals("B")){
                mv.visitMethodInsn(INVOKESTATIC, "edu/illinois/odet/agent/app/StateRecorder", "stateAccess",
                        "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;" + descriptor + ")V", false);
            } else {
                mv.visitMethodInsn(INVOKESTATIC, "edu/illinois/odet/agent/app/StateRecorder", "stateAccess",
                        "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V", false);
            }

        }
        mv.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack + 5, maxLocals);
    }
}