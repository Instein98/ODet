package edu.illinois.odet.agent.record.visitor;

import edu.illinois.odet.agent.Config;
import edu.illinois.odet.agent.utils.CommonUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

/**
 * @author Yicheng Ouyang
 * @Date 9/26/22
 */

public class FieldCV extends ClassVisitor {

    private String currentSlashClassName;

    public FieldCV(ClassVisitor cv, String className) {
        super(Config.ASM_Version, cv);
        currentSlashClassName = className;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        String fieldIdentifier = CommonUtils.getFieldIdentifier(currentSlashClassName, name, descriptor);
//        System.out.println("fieldIdentifier: " + fieldIdentifier);
        CommonUtils.putFieldAccessFlag(fieldIdentifier, access);
        return cv.visitField(access, name, descriptor, signature, value);
    }
}