package edu.illinois.odet.test.helper;

import com.google.common.base.Objects;

/**
 * @author Yicheng Ouyang
 * @Date 10/23/22
 */

public class Name {
    String firstName;
    String familyName;
    public Name(String firstName, String familyName) {
        this.firstName = firstName;
        this.familyName = familyName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Name name = (Name) o;
        return Objects.equal(firstName, name.firstName) && Objects.equal(familyName, name.familyName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(firstName, familyName);
    }

    @Override
    public String toString() {
        return "Name{" +
                "firstName='" + firstName + '\'' +
                ", familyName='" + familyName + '\'' +
                '}';
    }
}
