package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Career;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.CareerService;
import iuh.fit.goat.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CareerController {
//    private final CareerService careerService;
//
//    @PostMapping("/careers")
//    public ResponseEntity<Career> createCareer(@Valid @RequestBody Career career) throws InvalidException {
//        if(career.getName() != null && this.careerService.handleExistCareer(career.getName())) {
//            throw new InvalidException("Career exists");
//        }
//        Career newCareer = this.careerService.handleCreateCareer(career);
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(newCareer);
//    }
//
//    @PutMapping("/careers")
//    public ResponseEntity<Career> updateCareer(@Valid @RequestBody Career career) throws InvalidException {
//        if(career.getName() != null && this.careerService.handleExistCareer(career.getName())) {
//            throw new InvalidException("Career exists");
//        }
//        Career updateCareer = this.careerService.handleUpdateCareer(career);
//
//        if(updateCareer == null){
//            throw new InvalidException("Career doesn't exist");
//        }
//
//        return ResponseEntity.status(HttpStatus.OK).body(updateCareer);
//    }
//
//    @DeleteMapping("/careers/{id}")
//    @ApiMessage("Delete career by id")
//    public ResponseEntity<Void> deleteCareer(@PathVariable("id") String id) throws InvalidException {
//        Pattern pattern = Pattern.compile("^[0-9]+$");
//
//        if(pattern.matcher(id).matches()){
//            Career career = this.careerService.handleGetCareerById(Long.parseLong(id));
//            if(career != null){
//                this.careerService.handleDeleteCareer(Long.parseLong(id));
//                return ResponseEntity.status(HttpStatus.OK).body(null);
//            } else {
//                throw new InvalidException("Career doesn't exist");
//            }
//        } else {
//            throw new InvalidException("Id is number");
//        }
//    }
//
//    @GetMapping("/careers/{id}")
//    public ResponseEntity<Career> getCareerById(@PathVariable("id") String id) throws InvalidException {
//        Pattern pattern = Pattern.compile("^[0-9]+$");
//
//        if(pattern.matcher(id).matches()){
//            Career career = this.careerService.handleGetCareerById(Long.parseLong(id));
//            if(career != null){
//                return ResponseEntity.status(HttpStatus.OK).body(career);
//            } else {
//                throw new InvalidException("Career doesn't exist");
//            }
//        } else {
//            throw new InvalidException("Id is number");
//        }
//    }
//
//    @GetMapping("/careers")
//    public ResponseEntity<ResultPaginationResponse> getAllCareers(
//            @Filter Specification<Career> spec, Pageable pageable
//    ) {
//        ResultPaginationResponse result = this.careerService.handleGetAllCareers(spec, pageable);
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
}

