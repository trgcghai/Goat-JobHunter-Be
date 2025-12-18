package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import iuh.fit.goat.exception.*;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.service.*;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SkillController {
    private final SkillService skillService;

    @PostMapping("/skills")
    public ResponseEntity<Skill> createSkill(@Valid @RequestBody Skill skill) throws InvalidException {
        if(skill.getName() != null && this.skillService.handleExistSkill(skill.getName())) {
            throw new InvalidException("Skill exists");
        }
        Skill newSkill = this.skillService.handleCreateSkill(skill);

        return ResponseEntity.status(HttpStatus.CREATED).body(newSkill);
    }

    @PutMapping("/skills")
    public ResponseEntity<Skill> updateSkill(@Valid @RequestBody Skill skill) throws InvalidException {
        if(skill.getName() != null && this.skillService.handleExistSkill(skill.getName())) {
            throw new InvalidException("Skill exists");
        }
        Skill updateSkill = this.skillService.handleUpdateSkill(skill);

        if(updateSkill == null) {
            throw new InvalidException("Skill doesn't exist");
        }

        return ResponseEntity.status(HttpStatus.OK).body(updateSkill);
    }

    @DeleteMapping("/skills/{id}")
    @ApiMessage("Delete skill by id")
    public ResponseEntity<Void> deleteSkill(@PathVariable("id") String id) throws InvalidException {
        Pattern pattern = Pattern.compile("^[0-9]+$");

        if(pattern.matcher(id).matches()) {
            Skill skill = this.skillService.handGetSkillById(Long.parseLong(id));
            if(skill != null) {
                this.skillService.handleDeleteSkill(Long.parseLong(id));
                return ResponseEntity.status(HttpStatus.OK).body(null);
            } else {
                throw new InvalidException("Skill doesn't exist");
            }
        } else {
            throw new InvalidException("Id is number");
        }
    }

    @GetMapping("/skills/{id}")
    public ResponseEntity<Skill> getSkillById(@PathVariable("id") String id) throws InvalidException {
        Pattern pattern = Pattern.compile("^[0-9]+$");

        if(pattern.matcher(id).matches()) {
            Skill skill = this.skillService.handGetSkillById(Long.parseLong(id));
            if(skill != null) {
                return ResponseEntity.status(HttpStatus.OK).body(skill);
            } else {
                throw new InvalidException("Skill doesn't exist");
            }
        } else {
            throw new InvalidException("Id is number");
        }
    }

    @GetMapping("/skills")
    public ResponseEntity<ResultPaginationResponse> getAllSkills(
            @Filter Specification<Skill> spec, Pageable pageable
    ) {
        ResultPaginationResponse result = this.skillService.handleGetAllSkills(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
