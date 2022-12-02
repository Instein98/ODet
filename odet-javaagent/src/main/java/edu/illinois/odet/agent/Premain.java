package edu.illinois.odet.agent;

import com.google.common.collect.Sets;
import edu.illinois.odet.agent.detect.DetectTransformer;
import edu.illinois.odet.agent.record.RecordTransformer;
import edu.illinois.odet.agent.utils.CommonUtils;
import edu.illinois.odet.agent.utils.LogUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.io.File;
import java.io.FileInputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static edu.illinois.odet.agent.utils.CommonUtils.getFieldIdentifier;
import static edu.illinois.odet.agent.utils.CommonUtils.putFieldAccessFlag;

public class Premain {

    public static Set<String> prefixWhiteList = new HashSet<>();
    public static Set<String> thirdPartyPrefixWhiteList = Sets.newHashSet(
            "org/junit/runner/notification/RunNotifier"
    );

    private static String mode = "record";
    private static String detectConfigFilePath = null;

    public static void premain(String options, Instrumentation ins) {
        LogUtils.agentInfo("******** Premain Start ********\n");
        parseArgs(options);
        if ("record".equals(mode)){
            try{
                Enumeration<URL> roots = Premain.class.getClassLoader().getResources("");
                while(roots.hasMoreElements()){
                    URL url = roots.nextElement();
                    String path = url.getPath();
                    System.out.println(path);
                    try{
                        try (Stream<Path> stream = Files.walk(Paths.get(path))) {
                            stream.filter(Files::isRegularFile).forEach(Premain::recordFields);
                        }
                    } catch (Throwable x){
//                        x.printStackTrace();
                        continue;
                    }
                }
            } catch (Throwable t){
                t.printStackTrace();
            }
//            printRecordedFieldsInfo();  // debug
            ins.addTransformer(new RecordTransformer());
        } else if ("detect".equals(mode)){
            ins.addTransformer(new DetectTransformer(detectConfigFilePath));
        }
    }

    private static void recordFields(Path path){
        boolean match = false;
        if (!path.toString().endsWith(".class"))
            return;
        for (String prefix: prefixWhiteList){
            if (path.toString().contains(prefix)){
//                System.out.println(path.toString() + " is matched");  // debug
                match = true;
            }
        }
        if (!match){
//            System.out.println(path.toString() + " is not matched");  // debug
            return;
        }
        File target = path.toFile();
//        System.out.println("Loading " + path);  // debug
        try(FileInputStream fis = new FileInputStream(target)){
            ClassReader cr = new ClassReader(fis);
            ClassNode cn = new ClassNode();
            // make sure the class name satisfy the instPrefix
            if (!CommonUtils.matchPrefix(cr.getClassName(), prefixWhiteList)) return;
            cr.accept(cn, ClassReader.EXPAND_FRAMES);
            if (cn.fields == null) return;
            for (FieldNode fn: cn.fields){
                String fieldIdentifier = getFieldIdentifier(cn.name, fn.name, fn.desc);
                putFieldAccessFlag(fieldIdentifier, fn.access);
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    public static void parseArgs(String args){
        if (args == null || args.equals("")){
            return;
        }
        for (String argPair: args.split(";")){
            String[] kv = argPair.split("=");
            String key = kv[0];
            String value = kv[1];
            // should be slash class name prefix
            if (key.equals("instPrefix")){
                for (String prefix: value.split(",")){
                    prefixWhiteList.add(prefix);
                }
            } else if (key.equals("mode")){
                if ("detect".equals(value)){
                    mode = "detect";
                } else {
                    mode = "record";
                }
            } else if (key.equals("detectConfig")){
                detectConfigFilePath = value;
            }
        }
    }
}
