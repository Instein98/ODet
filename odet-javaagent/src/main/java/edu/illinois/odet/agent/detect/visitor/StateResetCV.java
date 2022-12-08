package edu.illinois.odet.agent.detect.visitor;

import edu.illinois.odet.agent.Config;
import edu.illinois.odet.agent.utils.LogUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_VOLATILE;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.SWAP;

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
    private String parentSlashName;

    // "Lorg/junit/Before;" or "Lorg/junit/jupiter/api/BeforeEach;"
    // or null if the class has no methods with annotation @Before or @BeforeEach
    private String beforeEachAnnotation;

    private HashMap<String, String> stateToResetMap;

    public StateResetCV(ClassVisitor classVisitor, String className, ClassLoader loader, int classVersion,
                        String beforeEachAnnotation, HashMap<String, String> stateToResetMap) {
        super(Config.ASM_Version, classVisitor);
        this.slashClassName = className;
        this.loader = loader;
        this.classVersion = classVersion;
        this.beforeEachAnnotation = beforeEachAnnotation;
        this.stateToResetMap = stateToResetMap;
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

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new StateResetMV(mv, slashClassName, name, descriptor, isJUnit3TestClass, isParameterizedTestClass,
                this.classVersion, beforeEachAnnotation, stateToResetMap, parentSlashName);
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
    private String parentSlashName;

    // If @Before/@BeforeEach methods exist, need to reset state at the beginning of these methods
    // Otherwise, reset the state at the beginning of the test method
    private String beforeEachAnnotation;
    private boolean isBeforeEachMethod = false;

    private HashMap<String, String> stateToResetMap;

    public StateResetMV(MethodVisitor methodVisitor, String className, String methodName,
                        String desc, boolean isJUnit3TestClass, boolean isParameterizedTestClass, int classVersion,
                        String beforeEachAnnotation, HashMap<String, String> stateToResetMap, String parentSlashName) {
        super(Config.ASM_Version, methodVisitor);
        this.slashClassName = className;
        this.methodName = methodName;
        this.methodDesc = desc;
        this.isJUnit3TestClass = isJUnit3TestClass;
        this.isParameterizedTestClass = isParameterizedTestClass;
        this.hasNoParameters = desc.contains("()");
        this.classVersion = classVersion;
        this.beforeEachAnnotation = beforeEachAnnotation;
        this.stateToResetMap = stateToResetMap;
        this.parentSlashName = parentSlashName;
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
        if ((beforeEachAnnotation == null && isTestMethod) || (beforeEachAnnotation != null && isBeforeEachMethod)){
            // reset the states
            for (String fieldId: stateToResetMap.keySet()) {
                String[] tmp = fieldId.split("#");
                int accessFlag = Integer.parseInt(tmp[0]);
                String fieldOwner = tmp[1];
                String fieldName = tmp[2];
                String fieldDesc = tmp[3];
                String serializationPath = stateToResetMap.get(fieldId);

                if (fieldOwner.equals(slashClassName)
                        || (accessFlag & ACC_STATIC) != 0 && ((accessFlag & ACC_PUBLIC) != 0
                        || (accessFlag & ACC_PROTECTED) != 0 && (fieldOwner.equals(parentSlashName))
                        || fieldOwner.startsWith(slashClassName + '$'))) {
                    super.visitLdcInsn(serializationPath);
                    super.visitMethodInsn(INVOKESTATIC, "edu/illinois/odet/agent/app/StateResetter", "deserialize",
                            "(Ljava/lang/String;)Ljava/lang/Object;", false);
                    super.visitTypeInsn(CHECKCAST, Type.getType(fieldDesc).getInternalName());
                    super.visitFieldInsn(PUTSTATIC, fieldOwner, fieldName, fieldDesc);

                } else if ((accessFlag & ACC_STATIC) != 0) {
                    super.visitLdcInsn(fieldOwner.replace("/", "."));
                    super.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
                    super.visitLdcInsn(fieldName);
                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
                    super.visitInsn(DUP);
                    super.visitLdcInsn(true);
                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);

                    if ((accessFlag & ACC_FINAL) != 0 || (accessFlag & ACC_VOLATILE) != 0) {
                        // change the field access flag (e.g., final) to allow assignment, see https://stackoverflow.com/a/3301720/11495796
                        super.visitInsn(DUP);  // fieldToSet
                        super.visitLdcInsn("java.lang.reflect.Field");
                        super.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
                        super.visitLdcInsn("modifiers");
                        super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
                        super.visitInsn(DUP);
                        super.visitLdcInsn(true);
                        super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);  // fieldToSet, modifiersField
                        super.visitInsn(SWAP);
                        super.visitLdcInsn(accessFlag & ~ACC_FINAL & ~ACC_VOLATILE);  // modifiersField, fieldToSet, newAcc
                        super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setInt", "(Ljava/lang/Object;I)V", false);  // fieldToSet, modifiersField
                    }

                    super.visitInsn(ACONST_NULL);
                    // deserialize the object
                    super.visitLdcInsn(serializationPath);
                    super.visitMethodInsn(INVOKESTATIC, "edu/illinois/odet/agent/app/StateResetter", "deserialize",
                            "(Ljava/lang/String;)Ljava/lang/Object;", false);
                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
                }
            }
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack+5, maxLocals);
    }
}
