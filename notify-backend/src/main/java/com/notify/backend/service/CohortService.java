package com.notify.backend.service;

import com.notify.backend.dto.cohort.AddCohortUsersRequest;
import com.notify.backend.dto.cohort.AddCohortUsersResponse;
import com.notify.backend.dto.cohort.CohortResponse;
import com.notify.backend.dto.cohort.CreateCohortRequest;

import java.util.List;
import java.util.UUID;

public interface CohortService {

    CohortResponse createCohort(CreateCohortRequest request, UUID clientId);

    List<CohortResponse> listCohorts(UUID clientId);

    AddCohortUsersResponse addUsers(Long cohortId, UUID clientId, AddCohortUsersRequest request);

    void removeUser(Long cohortId, UUID clientId, Long userId);
}
