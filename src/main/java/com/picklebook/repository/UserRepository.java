package com.picklebook.repository;

import com.picklebook.entity.User;
import com.picklebook.entity.enums.RoleName;
import com.picklebook.entity.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = "role")
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(UserStatus status);

    long countByRole_Name(RoleName roleName);

    @Query("""
            select u from User u
            where lower(u.fullName) like lower(concat('%', :keyword, '%'))
               or lower(u.email) like lower(concat('%', :keyword, '%'))
               or u.phone like concat('%', :keyword, '%')
            """)
    Page<User> search(@Param("keyword") String keyword, Pageable pageable);
}
