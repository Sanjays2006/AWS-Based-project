package com.cloudidentity.service;

import com.cloudidentity.entity.IamIdentity;
import com.cloudidentity.repository.IamIdentityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.ListRolesRequest;
import software.amazon.awssdk.services.iam.model.ListRolesResponse;
import software.amazon.awssdk.services.iam.model.ListUsersRequest;
import software.amazon.awssdk.services.iam.model.ListUsersResponse;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.model.User;

/**
 * Syncs real AWS IAM users and roles into the local {@code iam_identities}
 * table via the AWS SDK {@link IamClient}. New identities are created as
 * monitored; existing ones (matched by ARN) are refreshed in place.
 *
 * <p>Safe to call when AWS is not configured — any SDK error is caught and
 * logged, and the method returns the count synced so far (0 on failure).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwsIamSyncService {

    private final IamClient iamClient;
    private final IamIdentityRepository iamIdentityRepository;

    @Value("${aws.account-id:unknown}")
    private String accountId;

    @Value("${aws.region:us-east-1}")
    private String region;

    /**
     * Pulls all IAM users and roles from AWS and upserts them locally.
     *
     * @return number of identities created or updated
     */
    @Transactional
    public int syncIdentities() {
        log.info("[AWS IAM SYNC] Starting IAM identity synchronization...");
        int synced = 0;
        try {
            synced += syncUsers();
            synced += syncRoles();
            log.info("[AWS IAM SYNC] Synchronization complete. Upserted {} identities.", synced);
        } catch (Exception ex) {
            log.warn("[AWS IAM SYNC] Synchronization failed (AWS not reachable or missing "
                    + "iam:ListUsers/iam:ListRoles permission): {}", ex.getMessage());
        }
        return synced;
    }

    private int syncUsers() {
        int count = 0;
        String marker = null;
        do {
            ListUsersRequest.Builder req = ListUsersRequest.builder().maxItems(100);
            if (marker != null) {
                req.marker(marker);
            }
            ListUsersResponse resp = iamClient.listUsers(req.build());
            for (User u : resp.users()) {
                upsert(u.arn(), u.userName(), "USER");
                count++;
            }
            marker = Boolean.TRUE.equals(resp.isTruncated()) ? resp.marker() : null;
        } while (marker != null);
        return count;
    }

    private int syncRoles() {
        int count = 0;
        String marker = null;
        do {
            ListRolesRequest.Builder req = ListRolesRequest.builder().maxItems(100);
            if (marker != null) {
                req.marker(marker);
            }
            ListRolesResponse resp = iamClient.listRoles(req.build());
            for (Role r : resp.roles()) {
                upsert(r.arn(), r.roleName(), "ROLE");
                count++;
            }
            marker = Boolean.TRUE.equals(resp.isTruncated()) ? resp.marker() : null;
        } while (marker != null);
        return count;
    }

    /** Creates the identity on first sighting, or refreshes name/type if it exists. */
    private void upsert(String arn, String name, String type) {
        IamIdentity identity = iamIdentityRepository.findByArn(arn)
                .orElseGet(() -> IamIdentity.builder()
                        .arn(arn)
                        .isMonitored(true)
                        .build());
        identity.setIdentityName(name);
        identity.setIdentityType(type);
        identity.setAccountId(accountId);
        if (identity.getRegion() == null) {
            identity.setRegion(region);
        }
        iamIdentityRepository.save(identity);
    }
}
