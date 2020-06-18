package com.niks.authenticationservice.repository;

import com.niks.authenticationservice.enums.ERole;
import com.niks.authenticationservice.model.db.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(ERole name);
}