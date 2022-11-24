package edu.illinois.odet.agent.detect.visitor;

import edu.illinois.odet.agent.Config;
import edu.illinois.odet.agent.utils.LogUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Yicheng Ouyang
 * @Date 11/16/22
 */

public class StateResetCV extends ClassVisitor {
    private String slashClassName;
    private ClassLoader loader;
    private boolean isJUnit3TestClass;
    // Todo: support parameterized test classes
    private boolean isParameterizedTestClass;
    private int classVersion;

    // "Lorg/junit/Before;" or "Lorg/junit/jupiter/api/BeforeEach;"
    // or null if the class has no methods with annotation @Before or @BeforeEach
    private String beforeEachAnnotation;

    public StateResetCV(ClassVisitor classVisitor, String className, ClassLoader loader, int classVersion, String beforeEachAnnotation) {
        super(Config.ASM_Version, classVisitor);
        this.slashClassName = className;
        this.loader = loader;
        this.classVersion = classVersion;
        this.beforeEachAnnotation = beforeEachAnnotation;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superSlashName, String[] interfaces) {
        String originalSuperName = superSlashName;
        // check if this class is a subclass of TestCase.class
        try{
            while (superSlashName != null){
                if (superSlashName.equals("junit/framework/TestCase")){
                    this.isJUnit3TestClass = true;
                    break;
                }else{
                    InputStream is;
                    if (this.loader != null){
                        is = this.loader.getResourceAsStream(superSlashName + ".class");
                    } else {
                        is = ClassLoader.getSystemResourceAsStream(superSlashName + ".class");
                    }
                    byte[] superBytes = loadByteCode(is);
                    ClassReader parentCr = new ClassReader(superBytes);
                    superSlashName = parentCr.getSuperName();
                }
            }
//            log(String.format("%s is Junit 3 test class? %s", slashClassName, isJUnit3TestClass?"true":"false"));
        } catch (Exception e) {
            LogUtils.agentErr("[ERROR] ClassLoader can not get resource: " + superSlashName + ".class");
            LogUtils.agentErr(e);
        }
        super.visit(version, access, name, signature, originalSuperName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new StateResetMV(mv, slashClassName, name, descriptor, isJUnit3TestClass, isParameterizedTestClass, this.classVersion, beforeEachAnnotation);
    }

    private byte[] loadByteCode(InputStream inStream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(1000);
        byte[] b = new byte[1000];
        while(inStream.read(b) != -1) {
            outStream.write(b, 0, b.length);
        }
        inStream.close();
        outStream.close();
        return outStream.toByteArray();
    }
}


class StateResetMV extends MethodVisitor {

    private String slashClassName;
    private String methodName;
    private String methodDesc;
    private boolean isJUnit3TestClass;
    private boolean hasTestAnnotation;
    private boolean isParameterizedTestClass;
    private boolean hasPrameterizedTestAnnotation;
    private boolean hasNoParameters;
    private boolean isTestMethod;
    private int classVersion;

    // If @Before/@BeforeEach methods exist, need to reset state at the beginning of these methods
    // Otherwise, reset the state at the beginning of the test method
    private String beforeEachAnnotation;
    private boolean isBeforeEachMethod = false;

    public StateResetMV(MethodVisitor methodVisitor, String className, String methodName,
                        String desc, boolean isJUnit3TestClass, boolean isParameterizedTestClass,
                        int classVersion, String beforeEachAnnotation) {
        super(Config.ASM_Version, methodVisitor);
        this.slashClassName = className;
        this.methodName = methodName;
        this.methodDesc = desc;
        this.isJUnit3TestClass = isJUnit3TestClass;
        this.isParameterizedTestClass = isParameterizedTestClass;
        this.hasNoParameters = desc.contains("()");
        this.classVersion = classVersion;
        this.beforeEachAnnotation = beforeEachAnnotation;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (descriptor.equals("Lorg/junit/Test;") || descriptor.equals("Lorg/junit/jupiter/api/Test;")){
            this.hasTestAnnotation = true;
        } else if (descriptor.equals("Lorg/junit/jupiter/params/ParameterizedTest;")){
            this.hasPrameterizedTestAnnotation = true;
        } else if (descriptor.equals(beforeEachAnnotation)){
            isBeforeEachMethod = true;
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        this.isTestMethod = ((isJUnit3TestClass && methodName.startsWith("test") && hasNoParameters)
                || hasTestAnnotation
                || hasPrameterizedTestAnnotation);
//        log(String.format("%s is test method? %s", slashClassName+"#"+methodName, isTestMethod?"yes":"no"), null);
    }
}
