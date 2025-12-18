package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Career;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface CareerService {
    Career handleCreateCareer(Career career);

    Career handleUpdateCareer(Career career);

    void handleDeleteCareer(long id);

    Career handleGetCareerById(long id);

    ResultPaginationResponse handleGetAllCareers(Specification<Career> spec, Pageable pageable);

    boolean handleExistCareer(String name);
}
