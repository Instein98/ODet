package edu.illinois.odex.agent.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

import static edu.illinois.odex.agent.Config.ASM_Version;
import static edu.illinois.odex.agent.utils.CommonUtils.getFieldIdentifier;
import static edu.illinois.odex.agent.utils.CommonUtils.putFieldAccessFlag;

/**
 * @author Yicheng Ouyang
 * @Date 9/26/22
 */

public class FieldCV extends ClassVisitor {

    private String currentSlashClassName;

    public FieldCV(ClassVisitor cv, String className) {
        super(ASM_Version, cv);
        currentSlashClassName = className;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        String fieldIdentifier = getFieldIdentifier(currentSlashClassName, name, descriptor);
//        System.out.println("fieldIdentifier: " + fieldIdentifier);
        putFieldAccessFlag(fieldIdentifier, access);
        return cv.visitField(access, name, descriptor, signature, value);
    }
}