package edu.illinois.odet.test;

import edu.illinois.odet.test.helper.Name;
import edu.illinois.odet.test.helper.Person;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.illinois.odet.test.TestUtils.assertTestNotPollutesField;

/**
 * @author Yicheng Ouyang
 * @Date 10/23/22
 */

public class NoPollutionTest {

    private static Name defaultName = new Name("foo", "bar");
    private static Person someone = new Person(defaultName);
    private static List<Person> personList = new ArrayList<>();
    private static Map<Name, Person> nameToPersonMap = new HashMap<>();

    static {
        personList.add(someone);
        nameToPersonMap.put(defaultName, someone);
    }

    private static String fid_defaultName =     "edu.illinois.odet.test.NoPollutionTest#defaultName";
    private static String fid_someone =         "edu.illinois.odet.test.NoPollutionTest#someone";
    private static String fid_personList =      "edu.illinois.odet.test.NoPollutionTest#personList";
    private static String fid_nameToPersonMap = "edu.illinois.odet.test.NoPollutionTest#nameToPersonMap";

    @Test
    public void valueNotChangeTest(){
        System.out.println("***** Running valueNotChangeTest *****");
        String tid = "edu.illinois.odet.test.NoPollutionTest#valueNotChangeTest";
        defaultName = new Name("foo", "bar");
        someone = new Person(new Name("foo", "bar"));
        assertTestNotPollutesField(tid, fid_defaultName);
        assertTestNotPollutesField(tid, fid_someone);
    }

    @Test
    public void listMapNotChangeTest(){
        System.out.println("***** Running listMapNotChangeTest *****");
        String tid = "edu.illinois.odet.test.NoPollutionTest#listMapNotChangeTest";
        personList = new ArrayList<>();
        personList.add(new Person(new Name("foo", "bar")));
        nameToPersonMap = new HashMap<>();
        nameToPersonMap.put(new Name("foo", "bar"), new Person(new Name("foo", "bar")));
        assertTestNotPollutesField(tid, fid_personList);
        assertTestNotPollutesField(tid, fid_nameToPersonMap);
    }
}
