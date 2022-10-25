package edu.illinois.odet.test;

import edu.illinois.odet.test.helper.Name;
import edu.illinois.odet.test.helper.Person;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.illinois.odet.test.TestUtils.assertTestPollutesField;

/**
 * @author Yicheng Ouyang
 * @Date 10/24/22
 */

public class PollutionTest {

    private static Name defaultName = new Name("foo", "bar");
    private static Person someone = new Person(defaultName);
    private static List<Person> personList = new ArrayList<>();
    private static Map<Name, Person> nameToPersonMap = new HashMap<>();

    static {
        personList.add(someone);
        nameToPersonMap.put(defaultName, someone);
    }

    private static String fid_defaultName =     "edu.illinois.odet.test.PollutionTest#defaultName";
    private static String fid_someone =         "edu.illinois.odet.test.PollutionTest#someone";
    private static String fid_personList =      "edu.illinois.odet.test.PollutionTest#personList";
    private static String fid_nameToPersonMap = "edu.illinois.odet.test.PollutionTest#nameToPersonMap";

    @Test
    public void valueChangeTest(){
        System.out.println("***** Running valueChangeTest *****");
        String tid = "edu.illinois.odet.test.PollutionTest#valueChangeTest";
        defaultName.setFirstName("Tom");
        someone.setName(new Name("xxx", "yyy"));  // Todo: "someone.setName(defaultName);" can fail the test
        assertTestPollutesField(tid, fid_defaultName);
        assertTestPollutesField(tid, fid_someone);
    }

    @Test
    public void ListMapChangeTest(){
        System.out.println("***** Running ListMapChangeTest *****");
        String tid = "edu.illinois.odet.test.PollutionTest#ListMapChangeTest";
        personList.remove(0);
        nameToPersonMap.remove(defaultName);
        assertTestPollutesField(tid, fid_personList);
        assertTestPollutesField(tid, fid_nameToPersonMap);
    }
}
