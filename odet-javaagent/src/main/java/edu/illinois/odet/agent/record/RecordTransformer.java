package edu.illinois.odet.agent.record;

import com.google.common.collect.Sets;
import edu.illinois.odet.agent.Premain;
import edu.illinois.odet.agent.record.visitor.InterceptJunitTestEventCV;
import edu.illinois.odet.agent.record.visitor.StatePollutionCheckerCV;
import edu.illinois.odet.agent.record.visitor.StateRecorderCV;
import edu.illinois.odet.agent.utils.CommonUtils;
import edu.illinois.odet.agent.utils.LogUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Set;

import static edu.illinois.odet.agent.utils.CommonUtils.getClassVersion;

public class RecordTransformer implements ClassFileTransformer {

    private String transformerName = "RecordTransformer";

    Set<String> PREFIX_BLACK_LIST = Sets.newHashSet(
            "edu/illinois/odet/agent",
            "java",
            "sun",
            "com/sun",
            "javax",
            "jdk"
    );

    public static Set<String> junitInstPrefixes = Sets.newHashSet(
            "org/junit/runner/notification/RunNotifier"
    );

    private Set<String> PREFIX_WHITE_LIST = Premain.prefixWhiteList;

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws
            IllegalClassFormatException {
        byte[] result = classfileBuffer;

        if (className == null){
            return result;
        }

        if (PREFIX_WHITE_LIST == null || PREFIX_WHITE_LIST.size() == 0) {
            if (CommonUtils.matchPrefix(className, PREFIX_BLACK_LIST)) return result;
        } else {
            if (!CommonUtils.matchPrefix(className, PREFIX_WHITE_LIST) && !CommonUtils.matchPrefix(className, Premain.thirdPartyPrefixWhiteList))
                return result;
        }

        try{
            ClassReader cr = new ClassReader(result);
            ClassWriter cw = new ClassWriter(cr, 0);
            ClassVisitor cv = cw;
            if (CommonUtils.matchPrefix(className, junitInstPrefixes)){
                cv = new InterceptJunitTestEventCV(cv, className);
                cr.accept(cv, 0);
            } else {
                // check the class information before pass it to visitors
                ClassNode cn = new ClassNode();
                cr.accept(cn, 0);
                String afterEachAnnotation = getClassAfterEachMethodAnnotation(cn);

                cv = new StatePollutionCheckerCV(cv, className, loader, getClassVersion(cr), afterEachAnnotation);
                cv = new StateRecorderCV(cv, className);
//                cv = new FieldCV(cv, className);  // should be the last one
                cn.accept(cv);
            }

            result = cw.toByteArray();

//            FileUtils.write(Config.workingDirectory() + "/" + transformerName + "/"
//                    + className.replace('/', '.') + ".class", result);

        } catch (Throwable t){
            LogUtils.agentErr(t);
            t.printStackTrace();
        }

        return result;
    }

    private String getClassAfterEachMethodAnnotation(ClassNode cn){
        String junit4AfterEachDesc = "Lorg/junit/After;";
        String junit5AfterEachDesc = "Lorg/junit/jupiter/api/AfterEach;";

        for (MethodNode mn: cn.methods){
            if (mn == null || mn.visibleAnnotations == null) continue;
            for (AnnotationNode an: mn.visibleAnnotations){
                if (junit4AfterEachDesc.equals(an.desc)){
                    return junit4AfterEachDesc;
                } else if (junit5AfterEachDesc.equals(an.desc)){
                    return junit5AfterEachDesc;
                }
            }
        }
        return null;
    }
}
