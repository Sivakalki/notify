package com.notify.backend.service;

public interface DeduplicationService {

    /**
     * Atomically checks and adds externalUserId to the campaign's Cuckoo Filter.
     * Returns true if the item is new (proceed with notification).
     * Returns false if already in the filter (duplicate — skip).
     */
    boolean addIfAbsent(Long campaignId, String externalUserId);

    boolean exists(Long campaignId, String externalUserId);

    void delete(Long campaignId, String externalUserId);

    // Cohort-scoped deduplication — uses cuckoo:cohort:<cohortId> key namespace
    boolean addCohortMemberIfAbsent(Long cohortId, String externalUserId);

    void removeCohortMember(Long cohortId, String externalUserId);
}
