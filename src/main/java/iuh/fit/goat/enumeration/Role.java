package iuh.fit.goat.enumeration;

import lombok.Getter;

@Getter
public enum Role {

    ADMIN("SUPER_ADMIN"), RECRUITER("HR"), APPLICANT("APPLICANT");

    private final String value;

    Role(String value) {
        this.value = value;
    }
}
