package iuh.fit.goat.common;

import lombok.Getter;

@Getter
public enum Role {

    ADMIN("SUPER_ADMIN"), RECRUITER("HR"), APPLICANT("APPLICANT"), COMPANY("COMPANY");

    private final String value;

    Role(String value) {
        this.value = value;
    }
}
