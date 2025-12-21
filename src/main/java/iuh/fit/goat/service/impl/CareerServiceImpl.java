package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Career;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.repository.CareerRepository;
import iuh.fit.goat.repository.JobRepository;
import iuh.fit.goat.service.CareerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CareerServiceImpl implements CareerService {
    private final CareerRepository careerRepository;
    private final JobRepository jobRepository;

    @Override
    public Career handleCreateCareer(Career career) {
        return this.careerRepository.save(career);
    }

    @Override
    public Career handleUpdateCareer(Career career) {
        Career currentCareer = this.handleGetCareerById(career.getCareerId());

        if(currentCareer != null){
            currentCareer.setName(career.getName());
            return this.careerRepository.save(currentCareer);
        }
        return null;
    }

    @Override
    public void handleDeleteCareer(long id) {
        Career currentCareer = this.handleGetCareerById(id);

        if(currentCareer.getJobs() != null){
            List<Job> jobs = this.jobRepository.findByCareer(currentCareer);
            this.jobRepository.deleteAll(jobs);
        }

        this.careerRepository.deleteById(currentCareer.getCareerId());
    }

    @Override
    public Career handleGetCareerById(long id) {
        Optional<Career> career = this.careerRepository.findById(id);

        return career.orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllCareers(Specification<Career> spec, Pageable pageable) {
        Page<Career> page = this.careerRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        return new ResultPaginationResponse(meta, page.getContent());
    }

    @Override
    public boolean handleExistCareer(String name) {
        return this.careerRepository.existsByName(name);
    }
}
