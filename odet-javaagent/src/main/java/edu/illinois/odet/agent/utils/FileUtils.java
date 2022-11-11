package edu.illinois.odet.agent.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileUtils {

    public static void prepare(String path){
        File target = new File(path);
        if (!target.getParentFile().exists()){
            boolean succeed = target.getParentFile().mkdirs();
            if (!succeed){
                throw new RuntimeException("Can not prepare " + path);
            }
        }
    }

    public static void write(String path, byte[] bytes) {
        prepare(path);
        try{
            Files.write(Paths.get(path), bytes);
        }catch (Throwable t){
            t.printStackTrace();
        }
    }

    public static void write(String path, String content) {
        prepare(path);
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(path, true))){
            bw.write(content);
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    public static void clear(String path){
        File targetFile = new File(path);
        if (!targetFile.exists() || !targetFile.isFile()){
            return;
        }
        try(PrintWriter writer = new PrintWriter(path)){
            writer.print("");
        } catch (Throwable t){
            t.printStackTrace();
        }
    }
}
