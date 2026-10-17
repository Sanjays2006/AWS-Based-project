# Cloud Identity Behavior Fingerprinting System

Production-quality cloud security platform built with **Spring Boot 3 (Java 21)**, **PostgreSQL**, **Spring Security + JWT**, and **HTML5/CSS3/JavaScript**.

---

## 🌟 Key Features

1. **10-Dimension Behavior Fingerprinting Baseline Engine**:
   - Device Fingerprint (Browser + OS SHA-256 tokens)
   - Login Frequency & Failed Login Ratios
   - Session Duration & Data Download Volume Limits
   - Geographic & AWS Region Baseline Deviations
   - Resource & Privilege Escalation Activity Monitoring

2. **Weighted Risk Engine**:
   - Risk Score $= \sum(\text{triggered rule weights})$
   - Classifications: **LOW (0-25)**, **MEDIUM (26-50)**, **HIGH (51-75)**, **CRITICAL (76-100)**
   - Database-configurable rule weights (`RiskRule` entity & `/api/v1/risk-rules`)

3. **Admin Investigation Console**:
   - Deep forensic identity breakdown
   - Real-time activity timeline & risk factor explanation
   - Actionable automated remediation suggestions

4. **Self-Service Security Portal**:
   - In-app security verification alerts ("Was It Me?" vs "Not Me!")
   - Immediate session termination capability
   - Device fingerprint trust management

5. **Automated Schedulers**:
   - **CloudTrail Ingestion**: Polls AWS CloudTrail S3 logs every 5 minutes
   - **Fingerprint Recompute**: Aggregates 30-day behavioral baselines hourly
   - **Daily Snapshots**: Generates end-of-day history records at midnight
   - **ML Retraining**: Retrains machine learning prediction models weekly

---

## 🚀 Technology Stack

- **Backend**: Java 21, Spring Boot 3.2, Spring Security, Spring Data JPA, Flyway, Jackson (JSONB)
- **Frontend**: HTML5, Vanilla CSS3 (Deep Space Glassmorphism Design System), JavaScript (ES6+), Chart.js, Leaflet Maps
- **Database**: PostgreSQL 15+ (with native JSONB column mappings)
- **Authentication**: Stateless JWT Authentication with optional TOTP MFA

---

## 🛠 Project Architecture

```text
com.cloudidentity
├── config          # Security, Jwt, Cors, AppProperties, Flyway, DataInitializer
├── controller      # REST Controllers (Auth, Dashboard, Investigation, Anomalies, etc.)
├── dto             # Request & Response Data Transfer Objects
├── engine          # Core Security Engine Interfaces & Implementation Classes
│   ├── model       # BehaviorVector, EngineResult, RiskFactors
│   └── impl        # DefaultBehaviorEngine, RuleBasedRuleEngine, WeightedRiskEngine
├── entity          # 19 JPA PostgreSQL Domain Entities
├── exception       # Custom Exceptions & GlobalExceptionHandler
├── repository      # Spring Data JPA Repositories
├── scheduler       # CloudTrail, Fingerprint, Daily Snapshot & ML Retraining Schedulers
├── security        # JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal
├── service         # Business Services (Auth, User, Session, Dashboard, etc.)
└── util            # GeoIpUtil, DeviceFingerprintUtil, MapJsonConverter, MapperUtil
```

---

## ⚙️ Setup & Execution

### Prerequisites
- JDK 21+
- Apache Maven 3.8+
- PostgreSQL 15+ running on `localhost:5432` with database `cloud_identity_db` (or updated credentials in `src/main/resources/application.yml`)

### Running the Application

1. **Build the Application**:
   ```bash
   mvn clean package
   ```

2. **Run Spring Boot**:
   ```bash
   mvn spring-boot:run
   ```

3. **Access the Application**:
   - Web App URL: `http://localhost:8080`
   - Default Admin Credentials: `admin` / `AdminPass123!`
   - Default Analyst Credentials: `analyst` / `AnalystPass123!`

---

## ☁️ AWS Connection — Step by Step

The platform integrates with 7 AWS services via the AWS SDK v2 (wired in
`config/AwsClientConfig.java`). AWS integration is **optional** — the app runs
fully offline without credentials. Follow these steps to connect live AWS.

### 1. Create an IAM user
AWS Console → **IAM** → **Users** → **Create user** (e.g. `cloud-identity-app`),
programmatic access only (no console access).

### 2. Attach least-privilege permissions
Create a policy from [`aws/iam-policy.json`](aws/iam-policy.json) and attach it
to the user. It grants read access to STS, IAM, CloudTrail, S3, SNS, CloudWatch,
and GuardDuty.

### 3. Create an access key
IAM → user → **Security credentials** → **Create access key** →
*Application running outside AWS*. Copy the **Access Key ID** and
**Secret Access Key** (shown only once).

### 4. (Optional) Provision AWS resources
| Resource | Purpose |
|----------|---------|
| **SNS topic** | Security-alert fan-out (copy the topic ARN) |
| **CloudTrail** | Event source for the ingestion scheduler (default trail works) |
| **GuardDuty** | Enable a detector in your region for threat enrichment |

### 5. Configure environment variables
Copy `.env.example` to `.env` and fill in your values:
```bash
cp .env.example .env
```
Key variables:
```bash
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=...
AWS_ACCOUNT_ID=123456789012
AWS_SNS_TOPIC_ARN=arn:aws:sns:us-east-1:123456789012:cloud-identity-alerts
SCHEDULER_CLOUDTRAIL_ENABLED=true
```
> Leave `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` blank to use the default
> AWS credential chain instead (profile, or IAM role when deployed on AWS).

### 6. Build & run
On Windows, the helper script loads `.env` automatically:
```powershell
.\run.ps1
```
Or manually:
```bash
mvn clean package
mvn spring-boot:run
```

### 7. Verify the connection
- **Startup log** should show:
  `[AWS] Connection verified  account=... arn=... (region=...)`
- **Diagnostics endpoint** `GET /api/v1/diagnostics/status` returns:
  ```json
  { "awsConnected": true, "awsAccount": "123456789012",
    "awsStatus": "CONNECTED (account 123456789012, region us-east-1)",
    "cloudTrailStatus": "INGESTION_READY" }
  ```

### Credential resolution order
1. **Static keys** from `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` (if set)
2. **Default AWS chain** otherwise — env vars, `~/.aws/credentials` profile, or
   IAM instance/container role (preferred when deployed on AWS)
