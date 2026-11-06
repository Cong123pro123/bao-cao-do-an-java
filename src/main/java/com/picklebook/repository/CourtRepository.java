package com.picklebook.repository;

import com.picklebook.entity.Court;
import com.picklebook.entity.enums.CourtStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourtRepository extends JpaRepository<Court, Long> {

    Optional<Court> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Court> findAllByOrderBySortOrderAsc();

    List<Court> findByStatusOrderBySortOrderAsc(CourtStatus status);

    List<Court> findByBranchIdAndStatusOrderBySortOrderAsc(Long branchId, CourtStatus status);

    List<Court> findByStatusInOrderBySortOrderAsc(Collection<CourtStatus> statuses);

    @EntityGraph(attributePaths = {"schedules", "branch"})
    @Query("select c from Court c where c.id = :id")
    Optional<Court> findWithDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"images", "branch"})
    @Query("select c from Court c where c.slug = :slug")
    Optional<Court> findWithImagesBySlug(@Param("slug") String slug);

    @EntityGraph(attributePaths = {"images", "branch"})
    @Query("select c from Court c where c.status = :status and c.branch.status = :branchStatus order by c.sortOrder asc")
    List<Court> findWithImagesByStatus(@Param("status") CourtStatus status,
                                       @Param("branchStatus") CourtStatus branchStatus);

    @EntityGraph(attributePaths = {"images", "branch"})
    @Query("select c from Court c where c.status = :status and c.branch.status = :branchStatus "
            + "and c.branch.id = :branchId order by c.sortOrder asc")
    List<Court> findWithImagesByStatusAndBranchId(@Param("status") CourtStatus status,
                                                   @Param("branchStatus") CourtStatus branchStatus,
                                                   @Param("branchId") Long branchId);

    @EntityGraph(attributePaths = "branch")
    @Query("select c from Court c order by c.sortOrder asc, c.name asc")
    List<Court> findAllWithBranchOrderBySortOrderAsc();

    @EntityGraph(attributePaths = "branch")
    @Query("select c from Court c where c.status = :status order by c.sortOrder asc, c.name asc")
    List<Court> findWithBranchByStatus(@Param("status") CourtStatus status);

    @EntityGraph(attributePaths = "branch")
    @Query("select c from Court c where c.branch.id = :branchId and c.status = :status order by c.sortOrder asc, c.name asc")
    List<Court> findWithBranchByBranchIdAndStatus(@Param("branchId") Long branchId, @Param("status") CourtStatus status);

    /**
     * Khóa bi quan hàng sân trong transaction đặt sân, đảm bảo các request
     * cùng sân được xử lý tuần tự trước khi kiểm tra trùng khung giờ.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "branch")
    @Query("select c from Court c where c.id = :id")
    Optional<Court> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(CourtStatus status);
}
