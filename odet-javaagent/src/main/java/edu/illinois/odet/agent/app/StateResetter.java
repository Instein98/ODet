package edu.illinois.odet.agent.app;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;
import edu.illinois.odet.agent.utils.LogUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author Yicheng Ouyang
 * @Date 12/5/22
 */

public class StateResetter {

    public static XStream xs = new XStream();

    static {
        xs.addPermission(AnyTypePermission.ANY);
    }

    public static Object deserialize(String path){
        try{
            File file = new File(path);
            return xs.fromXML(file);
        } catch (Throwable t){
            LogUtils.agentErr(t);
            System.exit(-124);
        }
        return null;
    }
}
