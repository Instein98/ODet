package edu.illinois.odet.agent.app;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;

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

    public static Object deserialize(String path) throws IOException {
        File file = new File(path);
        return xs.fromXML(file);
    }
}
