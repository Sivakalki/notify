package com.notify.backend.service.impl;

import com.notify.backend.dto.cohort.AddCohortUsersRequest;
import com.notify.backend.dto.cohort.AddCohortUsersResponse;
import com.notify.backend.dto.cohort.CohortResponse;
import com.notify.backend.dto.cohort.CreateCohortRequest;
import com.notify.backend.dto.upload.UserRow;
import com.notify.backend.entity.*;
import com.notify.backend.exception.CohortNotFoundException;
import com.notify.backend.exception.DuplicateRequestException;
import com.notify.backend.repository.*;
import com.notify.backend.service.CohortService;
import com.notify.backend.service.DeduplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CohortServiceImpl implements CohortService {

    private final CohortRepository cohortRepository;
    private final CohortUserRepository cohortUserRepository;
    private final UserRepository userRepository;
    private final FrontendClientRepository clientRepository;
    private final DeduplicationService deduplicationService;

    @Override
    @Transactional
    public CohortResponse createCohort(CreateCohortRequest request, UUID clientId) {
        if (cohortRepository.existsByClientIdAndName(clientId, request.getName())) {
            throw new DuplicateRequestException(
                    "Cohort '" + request.getName() + "' already exists for this client");
        }

        FrontendClient client = clientRepository.getReferenceById(clientId);

        Cohort cohort = Cohort.builder()
                .client(client)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        cohort = cohortRepository.save(cohort);
        log.info("Created cohort: id={}, name={}, clientId={}", cohort.getId(), cohort.getName(), clientId);
        return CohortResponse.from(cohort);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CohortResponse> listCohorts(UUID clientId) {
        return cohortRepository.findByClientId(clientId).stream()
                .map(CohortResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public AddCohortUsersResponse addUsers(Long cohortId, UUID clientId, AddCohortUsersRequest request) {
        Cohort cohort = cohortRepository.findByIdAndClientId(cohortId, clientId)
                .orElseThrow(() -> new CohortNotFoundException("Cohort not found: " + cohortId));

        int added = 0;
        int duplicates = 0;

        for (UserRow row : request.getUsers()) {
            if (row.getExternalUserId() == null || row.getExternalUserId().isBlank()) {
                log.warn("Skipping row with blank externalUserId in cohortId={}", cohortId);
                duplicates++;
                continue;
            }

            boolean isNew = deduplicationService.addCohortMemberIfAbsent(cohortId, row.getExternalUserId());
            if (!isNew) {
                duplicates++;
                continue;
            }

            User user = upsertUser(row);

            CohortUser member = CohortUser.builder()
                    .id(new CohortUserId(cohortId, user.getId()))
                    .cohort(cohort)
                    .user(user)
                    .build();
            cohortUserRepository.save(member);
            added++;
        }

        log.info("addUsers cohortId={}: total={}, added={}, duplicates={}",
                cohortId, request.getUsers().size(), added, duplicates);

        return AddCohortUsersResponse.builder()
                .total(request.getUsers().size())
                .added(added)
                .duplicates(duplicates)
                .build();
    }

    @Override
    @Transactional
    public void removeUser(Long cohortId, UUID clientId, Long userId) {
        cohortRepository.findByIdAndClientId(cohortId, clientId)
                .orElseThrow(() -> new CohortNotFoundException("Cohort not found: " + cohortId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        cohortUserRepository.deleteByCohorIdAndUserId(cohortId, userId);
        deduplicationService.removeCohortMember(cohortId, user.getExternalUserId());

        log.info("Removed userId={} from cohortId={}", userId, cohortId);
    }

    private User upsertUser(UserRow row) {
        return userRepository.findByExternalUserId(row.getExternalUserId())
                .orElseGet(() -> userRepository.save(User.builder()
                        .externalUserId(row.getExternalUserId())
                        .email(row.getEmail())
                        .phone(row.getPhone())
                        .build()));
    }
}
