package edu.illinois.odex.agent.utils;

import edu.illinois.odex.agent.Config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import static edu.illinois.odex.agent.utils.FileUtils.prepare;

public class LogUtils {

    public static final String agentInfoPath = Config.workingDirectory() + "out.log";
    public static final String agentErrPath = Config.workingDirectory() + "err.log";

    static {
        prepare(agentInfoPath);
        prepare(agentErrPath);
        File infoFile = new File(agentInfoPath);
        if (infoFile.exists()){
            infoFile.delete();
        }
        File errFile = new File(agentErrPath);
        if (errFile.exists()){
            errFile.delete();
        }
    }

    public static void agentInfo(String message){
        FileUtils.write(agentInfoPath, message + "\n");
    }

    public static void agentErr(String message){
        FileUtils.write(agentErrPath, message + "\n");
    }

    public static void agentErr(Throwable t){
        prepare(agentErrPath);
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(agentErrPath, true))){
            t.printStackTrace(new PrintWriter(bw));
        } catch (Throwable x){
            x.printStackTrace();
        }
    }
}
