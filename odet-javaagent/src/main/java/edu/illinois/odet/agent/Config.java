package edu.illinois.odet.agent;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.objectweb.asm.Opcodes.ASM9;

public class Config {

    private static String agentName = "odetAgent";
    public static int ASM_Version = ASM9;

    public static String ODET_TMP_DIR = System.getProperty("user.dir") + "/odet/";

//    static {
//        System.err.println(Paths.get(workingDirectory()).toAbsolutePath());
//    }

    public static synchronized String version() {
        String version = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date());
        return version;
    }

    public static String workingDirectory() {
//        return System.getProperty("user.home") + "/agentLogs/" + agentName + "/" + version() + "/";
        return System.getProperty("user.home") + "/agentLogs/" + agentName + "/";  // for debug convenience
    }
}
