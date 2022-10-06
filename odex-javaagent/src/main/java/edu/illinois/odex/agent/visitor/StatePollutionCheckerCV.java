package edu.illinois.odex.agent.visitor;


import edu.illinois.odex.agent.utils.LogUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static edu.illinois.odex.agent.Config.ASM_Version;
import static org.objectweb.asm.Opcodes.*;

public class StatePollutionCheckerCV extends ClassVisitor {
    private String slashClassName;
    private ClassLoader loader;
    private boolean isJUnit3TestClass;
    // Todo: support parameterized test classes
    private boolean isParameterizedTestClass;
    private int classVersion;

    StatePollutionCheckerCV(ClassVisitor classVisitor, String className, ClassLoader loader, int classVersion) {
        super(ASM_Version, classVisitor);
        this.slashClassName = className;
        this.loader = loader;
        this.classVersion = classVersion;
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

//    @Override
//    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
//        if (visible && descriptor.equals("Lorg/junit/runner/RunWith;")){
//            AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
//            return new CoverageAnnotationVisitor(av);
//        } else {
//            return super.visitAnnotation(descriptor, visible);
//        }
//    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
//        log("Visiting method " + slashClassName + "#" + name, null);
        return new StatePollutionCheckerMV(mv, slashClassName, name, descriptor, isJUnit3TestClass, isParameterizedTestClass, this.classVersion);
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


class StatePollutionCheckerMV extends MethodVisitor {

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

    // insert a try catch block for the whole test method to capture the exception thrown
    private Label tryStart;
    private Label tryEndCatchStart;

    public StatePollutionCheckerMV(MethodVisitor methodVisitor, String className, String methodName,
                                   String desc, boolean isJUnit3TestClass, boolean isParameterizedTestClass, int classVersion) {
        super(ASM_Version, methodVisitor);
        this.slashClassName = className;
        this.methodName = methodName;
        this.methodDesc = desc;
        this.isJUnit3TestClass = isJUnit3TestClass;
        this.isParameterizedTestClass = isParameterizedTestClass;
        this.hasNoParameters = desc.contains("()");
        this.classVersion = classVersion;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (descriptor.equals("Lorg/junit/Test;") || descriptor.equals("Lorg/junit/jupiter/api/Test;")){
            this.hasTestAnnotation = true;
        } else if (descriptor.equals("Lorg/junit/jupiter/params/ParameterizedTest;")){
            this.hasPrameterizedTestAnnotation = true;
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
        if (isTestMethod){
//            super.visitLdcInsn(this.slashClassName);
//            super.visitLdcInsn(this.methodName);
//            super.visitMethodInsn(INVOKESTATIC, "", "testStart", "(Ljava/lang/String;Ljava/lang/String;)V", false);
            tryStart = new Label();
            super.visitLabel(tryStart);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        // reporting the test end event when return normally (add big try-catch block to handle exception throwing)
        if (isTestMethod && opcode >= IRETURN && opcode <= RETURN){
//            super.visitLdcInsn(this.slashClassName);
//            super.visitLdcInsn(this.methodName);
//            super.visitMethodInsn(INVOKESTATIC, "", "testEnd", "(Ljava/lang/String;Ljava/lang/String;)V", false);
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (isTestMethod){
            tryEndCatchStart = new Label();
            super.visitTryCatchBlock(tryStart, tryEndCatchStart, tryEndCatchStart, "java/lang/Throwable");
            super.visitLabel(tryEndCatchStart);
            // if the class is compiled with Java 6 or higher, stack map frames are required
//            log(String.format("Class version of %s: %d", slashClassName, classVersion), null);
            if (this.classVersion >= 50){
                super.visitFrame(F_FULL, 0, null, 1, new Object[]{"java/lang/Throwable"});
            }
            // report test method end
//            super.visitLdcInsn(this.slashClassName);
//            super.visitLdcInsn(this.methodName);
//            super.visitMethodInsn(INVOKESTATIC, "",
//                    "testEnd", "(Ljava/lang/String;Ljava/lang/String;)V", false);
            // rethrow the caught exception
            mv.visitInsn(ATHROW);
        }
        super.visitMaxs(maxStack+4, maxLocals);
    }
}