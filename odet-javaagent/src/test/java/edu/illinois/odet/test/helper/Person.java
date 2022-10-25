package edu.illinois.odet.test.helper;

/**
 * @author Yicheng Ouyang
 * @Date 10/23/22
 */

public class Person {
        Name name;
        public Person(Name name) {
            this.name = name;
        }

    public void setName(Name name) {
        this.name = name;
    }

    @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return name.equals(person.name);
        }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Person{" +
                "name=" + name +
                '}';
    }
}
