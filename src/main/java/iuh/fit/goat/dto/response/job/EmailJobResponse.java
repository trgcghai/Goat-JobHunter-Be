package iuh.fit.goat.dto.response.job;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EmailJobResponse {
    private String title;
    private double salary;
    private CompanyEmail company;
    private List<SkillEmail> skills;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class CompanyEmail {
        private String name;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class SkillEmail {
        private String name;
    }
}
