package edu.illinois.odet.agent.detect;

import com.google.common.collect.Sets;
import edu.illinois.odet.agent.Config;
import edu.illinois.odet.agent.Premain;
import edu.illinois.odet.agent.detect.visitor.StateResetCV;
import edu.illinois.odet.agent.utils.CommonUtils;
import edu.illinois.odet.agent.utils.FileUtils;
import edu.illinois.odet.agent.utils.LogUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.FileReader;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Set;

import static edu.illinois.odet.agent.Config.ODET_TMP_DIR;
import static edu.illinois.odet.agent.utils.CommonUtils.getClassVersion;

/**
 * @author Yicheng Ouyang
 * @Date 11/15/22
 */

public class DetectTransformer implements ClassFileTransformer {

    private String transformerName = "DetectTransformer";

    Set<String> PREFIX_BLACK_LIST = Sets.newHashSet(
            "edu/illinois/odet/agent",
            "java", "sun", "com/sun", "javax", "jdk");

    private Set<String> PREFIX_WHITE_LIST = Premain.prefixWhiteList;

    private HashMap<String, String> stateToResetMap = new HashMap<>();

    public DetectTransformer(String configFilePath){
        try(FileReader fr = new FileReader(configFilePath)){
            JSONParser jsonParser = new JSONParser();
            JSONObject obj = (JSONObject) jsonParser.parse(fr);
            JSONObject statesObj = (JSONObject) obj.get("states");
            for (Object fieldId: statesObj.keySet()){
                stateToResetMap.put((String) fieldId, (String) statesObj.get(fieldId));
//                System.out.println((String) fieldId);
//                System.out.println((String) statesObj.get(fieldId));
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

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
            if (!CommonUtils.matchPrefix(className, PREFIX_WHITE_LIST)) return result;
        }

        try{
            ClassReader cr = new ClassReader(result);
            ClassWriter cw = new ClassWriter(cr, 0);

            // check the beforeEachAnnotation before pass it to visitors
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);

            String beforeEachAnnotation = getClassAfterEachMethodAnnotation(cn);
            ClassVisitor cv = new StateResetCV(cw, className, loader, getClassVersion(cr), beforeEachAnnotation, stateToResetMap);
            cn.accept(cv);

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
        String junit4BeforeEachDesc = "Lorg/junit/Before;";
        String junit5BeforeEachDesc = "Lorg/junit/jupiter/api/BeforeEach;";

        for (MethodNode mn: cn.methods){
            if (mn == null || mn.visibleAnnotations == null) continue;
            for (AnnotationNode an: mn.visibleAnnotations){
                if (junit4BeforeEachDesc.equals(an.desc)){
                    return junit4BeforeEachDesc;
                } else if (junit5BeforeEachDesc.equals(an.desc)){
                    return junit5BeforeEachDesc;
                }
            }
        }
        return null;
    }
}

