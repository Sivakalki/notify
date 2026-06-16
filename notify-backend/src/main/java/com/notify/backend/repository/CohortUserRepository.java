package com.notify.backend.repository;

import com.notify.backend.entity.CohortUser;
import com.notify.backend.entity.CohortUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CohortUserRepository extends JpaRepository<CohortUser, CohortUserId> {

    List<CohortUser> findByCohortId(Long cohortId);

    boolean existsByIdCohortIdAndIdUserId(Long cohortId, Long userId);

    @Modifying
    @Query("DELETE FROM CohortUser cu WHERE cu.cohort.id = :cohortId AND cu.user.id = :userId")
    void deleteByCohorIdAndUserId(@Param("cohortId") Long cohortId, @Param("userId") Long userId);
}