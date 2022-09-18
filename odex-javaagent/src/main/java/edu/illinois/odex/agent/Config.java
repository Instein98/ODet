package edu.illinois.odex.agent;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.objectweb.asm.Opcodes.ASM9;

public class Config {

    private static String agentName = "templateAgent";
    public static int ASM_Version = ASM9;

    static {
        System.err.println(Paths.get(workingDirectory()).toAbsolutePath());
    }

    public static synchronized String version() {
        String version = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date());
        return version;
    }

    public static String workingDirectory() {
        return System.getProperty("user.home") + "/agentLogs/" + agentName + "/" + version() + "/";
    }
}
