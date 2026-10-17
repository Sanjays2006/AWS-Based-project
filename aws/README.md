# AWS Integration Guide — Cloud Identity Behavior Fingerprinting System

This guide explains how to connect the platform to AWS, which resources are **required** vs **optional**, how to run the scheduled and manual CloudTrail ingestion paths, and how to verify the integration is working.

> **Security note:** This document never contains real credentials. All keys, ARNs, and bucket names shown are placeholders. Never commit real secrets — `.env` is already in `.gitignore`.

---

## 1. Overview

AWS integration is **optional** — the application runs fully offline without credentials. When AWS is configured, the following 7 service clients are wired in `config/AwsClientConfig.java`:

| Client | Consumed by | Purpose |
| --- | --- | --- |
| **STS** | startup verifier + `/api/v1/diagnostics/status` | Confirms credentials via `GetCallerIdentity` |
| **IAM** | `AwsIamSyncService` | Syncs IAM users/roles into `iam_identities` |
| **CloudTrail** | `CloudTrailIngestionScheduler` | LookupEvents API ingestion |
| **S3** | `AwsS3CloudTrailReader` | Reads CloudTrail log files from a bucket |
| **SNS** | `NotificationService` | Fans out security alerts |
| **GuardDuty** | `AwsGuardDutyService` | Imports threat findings as anomalies |
| **CloudWatch** | `AwsCloudWatchMetricsService` | Publishes risk-score metrics |

---

## 2. Required vs Optional resources

### Required (only if you want live AWS integration at all)

- **IAM user** (or role) with programmatic access and the least-privilege policy in `iam-policy.json`.
- **AWS credentials** (access key + secret, or an IAM role via the default credential chain).

### Optional (enable the features you need)

| Resource | Needed for | If absent |
| --- | --- | --- |
| **CloudTrail trail + S3 bucket** | S3 log-file ingestion & LookupEvents | Ingestion returns `sourceAvailable=false` (no error) |
| **SNS topic** | Alert fan-out | SNS publish is skipped (in-app + email alerts still work) |
| **GuardDuty detector** | Findings import | Findings endpoint returns 0 ingested |
| **CloudWatch** | Risk metrics | Metric publish is a logged no-op |

Every integration is **resilient**: if a resource or permission is missing, the app logs a warning and continues — it never crashes.

---

## 3. IAM permissions setup

### Step 3.1 — Create the IAM user

1. AWS Console → **IAM** → **Users** → **Create user** (e.g. `cloud-identity-app`).
2. Do **not** enable console access — programmatic only.

### Step 3.2 — Attach the least-privilege policy

1. **Create policy** → **JSON** → paste the contents of `iam-policy.json`.
2. Replace `REPLACE_WITH_CLOUDTRAIL_BUCKET` (in **two** places) with your real CloudTrail bucket name.
3. Name it `CloudIdentityAppPolicy` and attach it to the user.

The policy grants **read-only** access only:

- API actions (`sts:GetCallerIdentity`, `iam:List*`/`GetUser`, `cloudtrail:LookupEvents`, `sns:Publish`, `cloudwatch:GetMetricData`/`ListMetrics`/`PutMetricData`, `guardduty:List*`/`GetFindings`) on `Resource: "*"`.
- S3 scoped to the CloudTrail bucket: `s3:ListBucket` on the bucket ARN and `s3:GetObject` on `bucket/*`. **No write/delete permissions.**

### Step 3.3 — Create an access key

IAM → user → **Security credentials** → **Create access key** → *Application running outside AWS*. Copy the **Access Key ID** and **Secret Access Key** (the secret is shown only once).

---

## 4. CloudTrail + S3 bucket configuration (optional)

1. **CloudTrail** → **Create trail** (e.g. `cloud-identity-trail`).
2. Point it at an **S3 bucket** (e.g. `my-cloudtrail-logs-bucket`); CloudTrail delivers gzipped JSON log files under a key prefix (default `AWSLogs/`).
3. Record the **bucket name** and **prefix** for the environment variables below.
4. Ensure the IAM policy's S3 statements reference this exact bucket.

CloudTrail delivers objects as `*.json.gz`; the reader lists, downloads, gunzips, extracts the `Records` array, and ingests each event.

---

## 5. SNS configuration (optional)

1. **SNS** → **Create topic** → *Standard* → e.g. `cloud-identity-alerts`.
2. Copy the **Topic ARN**.
3. Add a subscription (email/SMS/HTTPS) so alerts reach a destination.
4. Set `AWS_SNS_TOPIC_ARN` (below). If unset, SNS publishing is skipped.

---

## 6. CloudWatch (optional)

No resource to create — metrics are published to the custom namespace `CloudIdentity/Risk` automatically when AWS is configured. Each evaluated risk score emits a `RiskScore` metric tagged with `IdentityArn` and `RiskLevel` dimensions. Build dashboards/alarms on that namespace (e.g. alert on CRITICAL spikes). Requires `cloudwatch:PutMetricData` (included in the policy).

---

## 7. Environment variables

Copy the template and fill in real values:

```bash
cp .env.example .env     # Windows: copy .env.example .env

```

| Variable | Required? | Description |
| --- | --- | --- |
| `AWS_REGION` | for AWS | Region, e.g. `us-east-1` (default `us-east-1`) |
| `AWS_ACCESS_KEY_ID` | for static creds | Leave blank to use the default credential chain |
| `AWS_SECRET_ACCESS_KEY` | for static creds | Leave blank to use the default credential chain |
| `AWS_ACCOUNT_ID` | optional | Your 12-digit account id (tagging) |
| `AWS_SNS_TOPIC_ARN` | optional | SNS topic ARN for alert fan-out |
| `AWS_CLOUDTRAIL_BUCKET` | for S3 ingest | CloudTrail S3 bucket name |
| `AWS_CLOUDTRAIL_PREFIX` | optional | Key prefix (default `AWSLogs/`) |

> **Credential resolution** (in `AwsClientConfig`): if `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` are set, static credentials are used; otherwise the **default AWS chain** applies (env vars → `~/.aws/credentials` profile → IAM instance/container role). Prefer an IAM role when deployed on AWS.

---

## 8. Scheduled S3 ingestion

The S3 poller (`scheduler/S3CloudTrailIngestionScheduler.java`) periodically calls the **same** `AwsS3CloudTrailReader.ingestRecentEvents()` service as the manual trigger — no duplicated logic.

**Disabled by default.** Enable it only after AWS credentials and the bucket are configured.

| Variable | Default | Description |
| --- | --- | --- |
| `SCHEDULER_S3_ENABLED` | `false` | Master on/off for the S3 poller |
| `SCHEDULER_S3_POLL_MS` | `300000` | Poll interval in ms (5 min); next run starts only after the previous finishes |
| `SCHEDULER_S3_INITIAL_DELAY_MS` | `60000` | Delay before the first run after startup |

```bash
SCHEDULER_S3_ENABLED=true
SCHEDULER_S3_POLL_MS=300000

```

When disabled, the scheduler logs `"S3 CloudTrail ingestion scheduler is disabled. Skipping."` each interval — expected and harmless.

Related schedulers (also off by default): `SCHEDULER_CLOUDTRAIL_ENABLED` (LookupEvents API), `SCHEDULER_FINGERPRINT_ENABLED`, `SCHEDULER_REPORT_ENABLED`, `SCHEDULER_BASELINE_ENABLED`.

---

## 9. Manual admin trigger

All ingestion/sync operations can be triggered on demand. These endpoints live under `/api/v1/scheduler/**` and are **ADMIN-only**, enforced at two layers: a Spring Security rule (`hasRole('ADMIN')` in `SecurityConfig`) **and** a class-level `@PreAuthorize("hasRole('ADMIN')")` on the controller.

| Endpoint (POST) | Action |
| --- | --- |
| `/api/v1/scheduler/aws/s3-cloudtrail/trigger` | Ingest CloudTrail logs from S3 (returns a status report) |
| `/api/v1/scheduler/cloudtrail/trigger` | Ingest via the CloudTrail LookupEvents API |
| `/api/v1/scheduler/aws/iam-sync/trigger` | Sync IAM users/roles |
| `/api/v1/scheduler/aws/guardduty/trigger` | Import GuardDuty findings |

The S3 trigger returns an `IngestionResultResponse`:

```json
{
  "source": "S3",
  "scanned": 120, "ingested": 98, "duplicates": 20,
  "evaluated": 98, "failed": 2, "objectsRead": 5,
  "sourceAvailable": true,
  "message": "S3 ingestion complete: ...",
  "errors": []
}

```

Duplicate events are detected by the CloudTrail `eventID` (unique index on `activity_logs.event_id`, migration `V13`), so repeated runs never double-insert.

Example (obtain a JWT first via `/api/v1/auth/login`):

```bash
curl -X POST http://localhost:8080/api/v1/scheduler/aws/s3-cloudtrail/trigger \
     -H "Authorization: Bearer <ADMIN_JWT>"

```

---

## 10. Local build & run

Prerequisites: **JDK 21**, **Maven 3.8+**, **PostgreSQL 15+** on `localhost:5432` with database `cloud_identity_db`.

```powershell
# Windows helper — loads .env, builds, and runs
.\run.ps1

# Or manually:
mvn clean package
mvn spring-boot:run

```

- App URL: `http://localhost:8080`
- Default admin: `admin` / `AdminPass123!`
- Run tests: `mvn test`

---

## 11. Verify AWS integration is working

**1. Startup log** — look for:

```
[AWS] Connection verified OK  account=123456789012 arn=arn:aws:iam::...:user/cloud-identity-app (region=us-east-1)

```

If credentials are missing you'll instead see a warning (app still starts).

**2. Diagnostics endpoint** — log in as admin, then `GET /api/v1/diagnostics/status`:

```json
{ "awsConnected": true,
  "awsAccount": "123456789012",
  "awsStatus": "CONNECTED (account 123456789012, region us-east-1)",
  "cloudTrailStatus": "INGESTION_READY" }

```

**3. Manual trigger** — `POST /api/v1/scheduler/aws/s3-cloudtrail/trigger` and confirm `sourceAvailable: true` with a non-zero `scanned`/`ingested` count (once log files exist in the bucket).

**4. Data checks** — after ingestion, verify rows in PostgreSQL:

```sql
SELECT COUNT(*) FROM activity_logs WHERE event_id IS NOT NULL;
SELECT COUNT(*) FROM anomaly_events;    -- if any rules triggered

```

**5. CloudWatch** — open the `CloudIdentity/Risk` namespace in the CloudWatch console to see `RiskScore` data points.

---

## 12. Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| Startup warns `Connection NOT established` | No/invalid credentials | Set `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`/`AWS_REGION`, or configure an IAM role |
| `awsConnected: false` in diagnostics | Credentials or region wrong | Re-check `.env`; test with `aws sts get-caller-identity` locally |
| S3 trigger returns `sourceAvailable: false` | `AWS_CLOUDTRAIL_BUCKET` unset | Set the bucket env var |
| `AccessDenied` on S3 | Policy bucket ARN mismatch | Ensure `iam-policy.json` references the exact bucket (both statements) |
| `403 Forbidden` on `/scheduler/**` | Non-admin JWT | Log in as an ADMIN user |
| `401 Unauthorized` | Missing/expired JWT | Re-authenticate via `/api/v1/auth/login` |
| GuardDuty returns 0 | Detector not enabled in region | Enable GuardDuty in the configured region |
| Scheduler never runs | `SCHEDULER_S3_ENABLED=false` | Set it to `true` (after creds + bucket are ready) |
| No SNS alerts | `AWS_SNS_TOPIC_ARN` unset or no subscription | Set the ARN and subscribe an endpoint |
| Duplicate events ingested | `V13` migration not applied | Confirm Flyway ran `V13__add_activity_log_event_id.sql` |

---

## 13. Security best practices

- **Never commit secrets.** `.env` is git-ignored; use env vars or AWS Secrets Manager.
- **Least privilege.** The provided policy is read-only and bucket-scoped — don't broaden it.
- **Prefer IAM roles** over static keys once deployed on EC2/ECS/EKS (leave the access-key env vars blank to use the instance/container role).
- **Rotate access keys** regularly (~90 days) and delete unused keys.
- **Scope S3 tightly** — point `s3:GetObject`/`s3:ListBucket` at only the CloudTrail bucket.
- **Keep schedulers off** until credentials and resources are verified, to avoid noisy failed runs.
- **Protect admin endpoints** — the `/scheduler/**` routes are ADMIN-only by design; don't relax that in `SecurityConfig`.

