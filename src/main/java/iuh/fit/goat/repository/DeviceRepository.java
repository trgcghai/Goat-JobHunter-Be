package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {
    Device findByNameAndAccount_EmailAndDeletedAtIsNull(String name, String email);
}
