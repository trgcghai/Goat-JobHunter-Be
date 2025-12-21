package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {
    boolean existsByApiPathAndModuleAndMethod(String apiPath, String module, String method);
    List<Permission> findByPermissionIdIn(List<Long> permissionIds);
    Permission findByName(String name);
}