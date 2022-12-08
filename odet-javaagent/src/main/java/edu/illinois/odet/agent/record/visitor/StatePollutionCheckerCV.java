package edu.illinois.odet.agent.record.visitor;


import edu.illinois.odet.agent.Config;
import edu.illinois.odet.agent.utils.CommonUtils;
import edu.illinois.odet.agent.utils.LogUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static edu.illinois.odet.agent.utils.CommonUtils.STATE_RECORDER;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author Yicheng Ouyang
 * @Date 9/29/22
 */

public class StatePollutionCheckerCV extends ClassVisitor {
    private String slashClassName;
    private ClassLoader loader;
    private boolean isJUnit3TestClass;
    // Todo: support parameterized test classes
    private boolean isParameterizedTestClass;
    private int classVersion;
    private String parentSlashName;

    // "Lorg/junit/After;" or "Lorg/junit/jupiter/api/AfterEach;"
    // or null if the class has no methods with annotation @After or @AfterEach
    private String afterEachAnnotation;

    public StatePollutionCheckerCV(ClassVisitor classVisitor, String className, ClassLoader loader, int classVersion, String afterEachAnnotation) {
        super(Config.ASM_Version, classVisitor);
        this.slashClassName = className;
        this.loader = loader;
        this.classVersion = classVersion;
        this.afterEachAnnotation = afterEachAnnotation;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superSlashName, String[] interfaces) {
        this.parentSlashName = superSlashName;
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
        return new StatePollutionCheckerMV(mv, slashClassName, name, descriptor, isJUnit3TestClass,
                isParameterizedTestClass, this.classVersion, afterEachAnnotation, this.parentSlashName);
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
    private String parentSlashName;

    // If @After/@AfterEach methods exist, need to check state at the end of these methods
    // Otherwise, check state before the test method return
    private String afterEachAnnotation;
    private boolean isAfterEachMethod = false;

    // insert a try catch block for the whole test method to capture the exception thrown
    private Label tryStart;
    private Label tryEndCatchStart;

    public StatePollutionCheckerMV(MethodVisitor methodVisitor, String className, String methodName,
                                   String desc, boolean isJUnit3TestClass, boolean isParameterizedTestClass,
                                   int classVersion, String afterEachAnnotation, String parentSlashName) {
        super(Config.ASM_Version, methodVisitor);
        this.slashClassName = className;
        this.methodName = methodName;
        this.methodDesc = desc;
        this.isJUnit3TestClass = isJUnit3TestClass;
        this.isParameterizedTestClass = isParameterizedTestClass;
        this.hasNoParameters = desc.contains("()");
        this.classVersion = classVersion;
        this.afterEachAnnotation = afterEachAnnotation;
        this.parentSlashName  = parentSlashName;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (descriptor.equals("Lorg/junit/Test;") || descriptor.equals("Lorg/junit/jupiter/api/Test;")){
            this.hasTestAnnotation = true;
        } else if (descriptor.equals("Lorg/junit/jupiter/params/ParameterizedTest;")){
            this.hasPrameterizedTestAnnotation = true;
        } else if (descriptor.equals(afterEachAnnotation)){
            isAfterEachMethod = true;
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
            // this is where the test method exit, check states if no @After/@AfterEach methods
            if (afterEachAnnotation == null){
                instCheckStatesCode();
            }
        } else if (isAfterEachMethod && opcode >= IRETURN && opcode <= RETURN){
            instCheckStatesCode();
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        // handle edu.illinois.odet.test.TestUtils#assertTestPollutesField
        if (opcode==INVOKESTATIC && owner.equals("edu/illinois/odet/test/TestUtils")
                && name.equals("assertTestPollutesField") && descriptor.equals("(Ljava/lang/String;Ljava/lang/String;)V")) {
            super.visitMethodInsn(INVOKESTATIC, STATE_RECORDER, "recordAssertPollutionInvocation", descriptor, false);
        } else if (opcode==INVOKESTATIC && owner.equals("edu/illinois/odet/test/TestUtils")
                && name.equals("assertTestNotPollutesField") && descriptor.equals("(Ljava/lang/String;Ljava/lang/String;)V")){
            super.visitMethodInsn(INVOKESTATIC, STATE_RECORDER, "recordAssertNoPollutionInvocation", descriptor, false);
        } else {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
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
            // this is where the test method exit, check states if no @After/@AfterEach methods
            if (afterEachAnnotation == null){
                instCheckStatesCode();
            }
            // rethrow the caught exception
            mv.visitInsn(ATHROW);
        }
        super.visitMaxs(maxStack+6, maxLocals);
    }

    private void instCheckStatesCode(){
        for (String fieldId: CommonUtils.getFieldIds()){
            String[] tmp = fieldId.split("#");
            String fieldOwner = tmp[0];
            String fieldName = tmp[1];
            String fieldDesc = tmp[2];
            int accessFlag = CommonUtils.getFieldAccessFlag(fieldId);

            if (fieldOwner.equals(slashClassName)
                    || (accessFlag & ACC_STATIC) != 0 && ((accessFlag & ACC_PUBLIC) != 0
                    || (accessFlag & ACC_PROTECTED) != 0 && (fieldOwner.equals(parentSlashName))
                    || fieldOwner.startsWith(slashClassName+'$'))){
//                LogUtils.agentInfo(fieldDesc);
                // Todo: only ldc a single string to reduce the inserted code
                super.visitLdcInsn(accessFlag);
                super.visitLdcInsn(fieldOwner);
                super.visitLdcInsn(fieldName);
                super.visitLdcInsn(fieldDesc);
                super.visitFieldInsn(GETSTATIC, fieldOwner, fieldName, fieldDesc);  // Todo: check if this is necessary first and then access the field to be more efficient
                if (fieldDesc.equals("C") || fieldDesc.equals("S") || fieldDesc.equals("I") || fieldDesc.equals("J") || fieldDesc.equals("F") || fieldDesc.equals("D") || fieldDesc.equals("Z") || fieldDesc.equals("B")){
                    super.visitMethodInsn(INVOKESTATIC, STATE_RECORDER, "checkFieldState", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;" + fieldDesc + ")V", false);
                } else {
                    super.visitMethodInsn(INVOKESTATIC, STATE_RECORDER, "checkFieldState", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V", false);
                }
            }
            // For "final" and "volatile", will throw java.lang.IllegalAccessException: class xxx cannot access a member of class yyy with modifiers "private static volatile"
            else if ((accessFlag & ACC_STATIC) != 0) {
                super.visitLdcInsn(accessFlag);
                super.visitLdcInsn(fieldOwner);
                super.visitLdcInsn(fieldName);
                super.visitLdcInsn(fieldDesc);
                super.visitLdcInsn(fieldOwner.replace("/", "."));
                super.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
                super.visitLdcInsn(fieldName);
                super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
                super.visitInsn(DUP);
                super.visitLdcInsn(true);
                super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);
                super.visitInsn(ACONST_NULL);
                super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
//                if (fieldDesc.equals("C")){
//                    super.visitTypeInsn(CHECKCAST, "java/lang/Character");
//                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
//                } else if (fieldDesc.equals("S")){
//                    super.visitTypeInsn(CHECKCAST, "java/lang/Short");
//                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
//                } else if (fieldDesc.equals("I")){
//                    super.visitTypeInsn(CHECKCAST, "java/lang/Integer");
//                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
//                } else if (fieldDesc.equals("J")){
//                    super.visitTypeInsn(CHECKCAST, "java/lang/Long");
//                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
//                } else if (fieldDesc.equals("F")){
//                    super.visitTypeInsn(CHECKCAST, "java/lang/Float");
//                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
//                } else if (fieldDesc.equals("D")){
//                    super.visitTypeInsn(CHECKCAST, "java/lang/Double");
//                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
//                } else if (fieldDesc.equals("Z")){
//                    super.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
//                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
//                } else if (fieldDesc.equals("B")){
//                    super.visitTypeInsn(CHECKCAST, "java/lang/Byte");
//                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
//                } else {
//                    super.visitTypeInsn(CHECKCAST, Type.getType(fieldDesc).getInternalName());
//                }
//                if (fieldDesc.equals("C") || fieldDesc.equals("S") || fieldDesc.equals("I") || fieldDesc.equals("J") || fieldDesc.equals("F") || fieldDesc.equals("D") || fieldDesc.equals("Z") || fieldDesc.equals("B")){
//                    super.visitMethodInsn(INVOKESTATIC, STATE_RECORDER, "checkFieldState", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;" + fieldDesc + ")V", false);
//                } else {
                    super.visitMethodInsn(INVOKESTATIC, STATE_RECORDER, "checkFieldState", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V", false);
//                }
            }
        }
        super.visitMethodInsn(INVOKESTATIC, STATE_RECORDER, "assertPollution", "()V", false);
        super.visitMethodInsn(INVOKESTATIC, STATE_RECORDER, "assertNoPollution", "()V", false);
    }
}