package com.notify.backend.repository;

import com.notify.backend.entity.FileStatus;
import com.notify.backend.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    List<UploadedFile> findByCampaignId(Long campaignId);

    List<UploadedFile> findByCampaignIdAndStatus(Long campaignId, FileStatus status);

    @Query("SELECT COALESCE(SUM(f.duplicateCount), 0) FROM UploadedFile f")
    long sumDuplicateCount();

    @Query("SELECT COALESCE(SUM(f.duplicateCount), 0) FROM UploadedFile f WHERE f.campaign.id = :campaignId")
    long sumDuplicateCountByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT COALESCE(SUM(f.duplicateCount), 0) FROM UploadedFile f " +
           "WHERE f.createdAt >= :startDate AND f.createdAt <= :endDate")
    long sumDuplicateCountInDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT COALESCE(SUM(f.duplicateCount), 0) FROM UploadedFile f " +
           "WHERE f.campaign.id IN :campaignIds " +
           "AND f.createdAt >= :startDate AND f.createdAt <= :endDate")
    long sumDuplicateCountByCampaignIdsInDateRange(
            @Param("campaignIds") List<Long> campaignIds,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT DISTINCT f.campaign.id FROM UploadedFile f " +
           "WHERE LOWER(f.fileName) LIKE LOWER(CONCAT('%', :fileName, '%'))")
    List<Long> findCampaignIdsByFileNameContaining(@Param("fileName") String fileName);
}