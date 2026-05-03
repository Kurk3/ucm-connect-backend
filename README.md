# UCM Connect API

Backend REST API for **UCM Connect** — a student community platform built as a bachelor's thesis project at the University of SS. Cyril and Methodius in Trnava (UCM).

Students can share study materials (PDFs, images, ZIP files) organized by subject, comment on posts, and like content. Authentication is handled entirely via AWS Cognito with JWT tokens.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 14+ (AWS RDS) |
| Authentication | AWS Cognito (JWT / RS256) |
| File Storage | AWS S3 (pre-signed URLs) |
| Caching | Caffeine (in-memory) |
| DB Migrations | Flyway |
| API Docs | Swagger / OpenAPI 3 |
| Rate Limiting | Bucket4j |
| Deployment | AWS Elastic Beanstalk |

---

## Architecture Overview

```
Client
  │
  ▼
Spring Boot API  ──►  AWS Cognito    (JWT validation)
                 ──►  AWS RDS        (PostgreSQL data)
                 ──►  AWS S3         (file storage)
```

Every protected endpoint validates the Bearer JWT issued by AWS Cognito. The `cognito_user_id` (the Cognito `sub` claim) is stored in the local `users` table so the API can map tokens to local user records without calling Cognito on every request.

Files are uploaded to S3 and never served directly — the API generates pre-signed S3 URLs (1-hour expiry) and returns them to the client for direct download.

---

## Data Model

Six tables, all using UUID primary keys:

```
users
  id, name, nick_name, email, cognito_user_id,
  email_verified, two_factor_enabled, discord_id,
  role (USER | MODERATOR | ADMIN), created_at, updated_at

subjects
  id, name, description, created_at, updated_at

posts
  id, user_id → users, subject_id → subjects,
  title, description,
  number_of_likes, number_of_comments, is_published,
  created_at, updated_at

files
  id, post_id → posts, user_id → users,
  file_name, file_key (S3 object key),
  file_type (PDF | IMAGE | ZIP), file_size, mime_type,
  created_at

comments
  id, post_id → posts, user_id → users,
  content, number_of_likes,
  created_at, updated_at

likes
  id, user_id → users, post_id → posts (nullable),
  comment_id → comments (nullable), created_at
  -- CHECK: exactly one of post_id / comment_id must be set
  -- UNIQUE: (user_id, post_id), (user_id, comment_id)
```

Cascade rules: deleting a user cascades to their posts, comments, and likes. Deleting a subject sets `subject_id` to NULL on posts (so posts are not lost).

---

## Project Structure

```
src/main/java/com/example/ucmconnectapi/
│
├── UcmConnectApiApplication.java       # Entry point
│
├── config/
│   ├── SecurityConfig.java             # Spring Security — CORS, JWT filter chain, public routes
│   ├── CacheConfig.java                # Caffeine cache setup (subjects 24h, posts 15min, etc.)
│   ├── RateLimitingFilter.java         # Bucket4j — 10 req/min per IP on /auth/** endpoints
│   ├── JpaAuditingConfig.java          # Enables @CreatedDate / @LastModifiedDate on entities
│   └── OpenApiConfig.java              # Swagger/OpenAPI 3 configuration with Bearer auth scheme
│
├── controller/
│   ├── AuthController.java             # POST /auth/register, login, logout, verify-email,
│   │                                   #   forgot-password, reset-password, refresh-token
│   ├── UserController.java             # GET|PUT /users/me, GET /users/{id}
│   ├── PostController.java             # CRUD /posts — with file upload (multipart)
│   ├── CommentController.java          # CRUD /posts/{id}/comments, /comments/{id}
│   ├── SubjectController.java          # CRUD /subjects — admin/mod create, public read
│   ├── LikeController.java             # POST|DELETE /posts/{id}/likes, /comments/{id}/likes
│   ├── FileController.java             # GET /files/{key} — returns S3 pre-signed URL
│   ├── AdminController.java            # PATCH /admin/users/{id}/role — admin only
│   ├── DiscordController.java          # Discord OAuth2 callback + role sync
│   ├── CacheController.java            # DELETE /cache/** — manual cache eviction (admin)
│   ├── DataSeedController.java         # POST /seed — dev/local data seeding endpoint
│   ├── TestController.java             # GET /test — health/auth smoke test
│   └── HelloController.java            # GET / — basic liveness check
│
├── service/
│   ├── CognitoService.java             # AWS Cognito SDK calls: register, login, verify,
│   │                                   #   forgot/reset password, token refresh
│   ├── PostService.java                # Post CRUD — creates File record after S3 upload
│   ├── CommentService.java             # Comment CRUD with ownership checks
│   ├── SubjectService.java             # Subject CRUD — cached reads
│   ├── LikeService.java                # Like/unlike — updates denormalized counters on Post/Comment
│   ├── FileService.java                # Delegates to S3FileStorageService, manages File records
│   ├── S3FileStorageService.java       # AWS S3 SDK: upload, delete, generate pre-signed URLs
│   ├── UserService.java                # User profile reads/updates, role management
│   ├── DataSeedService.java            # Creates test subjects/posts for local development
│   └── ProductionDataSeedService.java  # One-time production seeding (subjects, initial data)
│
├── entity/
│   ├── User.java                       # @Table(users) — includes role helpers isAdmin(), etc.
│   ├── Post.java                       # @Table(posts) — LAZY loads user and subject
│   ├── Comment.java                    # @Table(comments)
│   ├── Subject.java                    # @Table(subjects)
│   ├── File.java                       # @Table(files) — stores S3 key, not URL
│   ├── Like.java                       # @Table(likes) — polymorphic (post OR comment)
│   └── Role.java                       # Enum: USER, MODERATOR, ADMIN
│
├── dto/
│   ├── AuthResponse.java               # token + user returned after login
│   ├── LoginRequest.java               # email + password
│   ├── RegisterRequest.java            # name + nickName + email + password
│   ├── VerifyEmailRequest.java         # email + confirmationCode (Cognito OTP)
│   ├── ForgotPasswordRequest.java      # email
│   ├── ResetPasswordRequest.java       # email + confirmationCode + newPassword
│   ├── RefreshTokenRequest/Response    # Cognito refresh token exchange
│   ├── PostDTO.java                    # flat post for list views
│   ├── PostDetailDTO.java              # post with nested author, subject, file info
│   ├── PostListResponse.java           # paginated wrapper: posts + total + page + limit
│   ├── CommentDTO / CommentDetailDTO   # comment representations
│   ├── CommentListResponse.java
│   ├── SubjectDTO / SubjectListResponse
│   ├── FileDTO / FileUploadResponse / FileDownloadResponse
│   ├── LikeDTO.java
│   ├── UserDTO.java
│   ├── UpdateUserRequest.java
│   ├── RoleUpdateRequest.java          # used by AdminController
│   ├── LogoutResponse.java
│   ├── ErrorResponse.java
│   └── nested/
│       ├── AuthorDTO.java              # id + name + nickName (embedded in PostDetailDTO)
│       ├── SubjectInfoDTO.java         # id + name (embedded in PostDetailDTO)
│       ├── FileInfoDTO.java            # key + type + size (embedded in PostDetailDTO)
│       └── StatsDTO.java              # likes + comments count
│
├── repository/
│   ├── UserRepository.java             # findByEmail, findByCognitoUserId, findByNickName
│   ├── PostRepository.java             # findBySubjectId, findByUserId (paginated)
│   ├── CommentRepository.java          # findByPostId (paginated)
│   ├── SubjectRepository.java          # findByName
│   ├── LikeRepository.java             # findByUserIdAndPostId, countByPostId
│   └── FileRepository.java             # findByPostId
│
├── security/
│   └── JwtAuthenticationUtil.java      # Validates Cognito JWT: fetches JWKS, verifies
│                                       # RS256 signature, extracts sub + email claims
│
├── exception/
│   ├── GlobalExceptionHandler.java     # @ControllerAdvice — maps exceptions to HTTP codes
│   ├── ResourceNotFoundException.java  # → 404
│   ├── ConflictException.java          # → 409
│   ├── ForbiddenException.java         # → 403
│   └── ErrorResponse.java              # { error, message, details }
│
└── util/
    ├── HtmlSanitizer.java              # Strips HTML/JS from user-supplied text fields
    └── LogSanitizer.java               # Redacts tokens/passwords before writing to logs
```

---

## API Endpoints

Base URL: `/api/v1`

```
Auth
  POST   /auth/register
  POST   /auth/login
  POST   /auth/logout
  POST   /auth/verify-email
  POST   /auth/forgot-password
  POST   /auth/reset-password
  POST   /auth/refresh-token

Users
  GET    /users/me
  PUT    /users/me
  GET    /users/{id}

Posts
  GET    /posts                  ?page, limit, subjectId, userId
  POST   /posts                  multipart/form-data (title, subjectId, file)
  GET    /posts/{id}
  PUT    /posts/{id}
  DELETE /posts/{id}

Files
  GET    /files/{key}            returns S3 pre-signed download URL

Subjects
  GET    /subjects
  POST   /subjects               (MODERATOR / ADMIN only)
  GET    /subjects/{id}
  PUT    /subjects/{id}          (MODERATOR / ADMIN only)
  DELETE /subjects/{id}          (ADMIN only)

Comments
  GET    /posts/{id}/comments
  POST   /posts/{id}/comments
  PUT    /comments/{id}
  DELETE /comments/{id}

Likes
  POST   /posts/{id}/likes
  DELETE /posts/{id}/likes
  POST   /comments/{id}/likes
  DELETE /comments/{id}/likes

Admin
  PATCH  /admin/users/{id}/role  (ADMIN only)

Discord
  GET    /discord/callback        OAuth2 callback
  POST   /discord/link            Link Discord account to user
```

All endpoints except `/auth/**` and `GET /subjects` require `Authorization: Bearer <token>`.

---

## Key Features

**Rate limiting** — `RateLimitingFilter` applies a token-bucket (10 req/min per IP) to all `/auth/**` endpoints using Bucket4j to prevent brute-force attacks.

**Caching** — Caffeine in-memory cache with per-cache TTL strategy:
- `subjects` / `subjectById` — 24h (subjects rarely change)
- `userById` — 1h
- `postById` — 15min
- `commentsByPost` — 5min

**Input sanitization** — `HtmlSanitizer` strips HTML from text fields before persistence. `LogSanitizer` redacts tokens and passwords before they reach log output.

**Role-based access** — Three roles: `USER`, `MODERATOR`, `ADMIN`. Role checks are enforced in service layer methods; `AdminController` is additionally restricted at the security filter chain level.

**Discord integration** — Users can link their Discord account via OAuth2. A separate bot service (not in this repo) syncs Discord server roles with platform roles.

---

## Database Migrations

Flyway runs automatically on startup. Files are in `src/main/resources/db/migration/`:

| Migration | Description |
|-----------|-------------|
| `V1__initial_schema.sql` | Creates all 6 tables with indexes and constraints |
| `V2__add_user_role.sql` | Adds `role` column to `users` |
| `V3__set_initial_admin.sql` | Sets the first registered user as ADMIN |
| `V4__add_discord_id.sql` | Adds `discord_id` column to `users` |

---

## Prerequisites

- Java 21
- PostgreSQL 14+
- Maven (wrapper included — no install needed)
- AWS account with: Cognito User Pool, S3 bucket, RDS instance

---

## Local Setup

1. Clone the repository:
```bash
git clone git@github.com:Kurk3/ucm-connect-api.git
cd ucm-connect-api
```

2. Create `.env` from the example:
```bash
cp .env.example .env
```

3. Fill in your environment variables in `.env`:
```env
DB_URL=jdbc:postgresql://localhost:5432/ucmconnect
DB_USERNAME=postgres
DB_PASSWORD=your_password

AWS_COGNITO_REGION=eu-central-1
AWS_COGNITO_USER_POOL_ID=eu-central-1_xxxxxxx
AWS_COGNITO_CLIENT_ID=your_client_id
AWS_COGNITO_CLIENT_SECRET=your_client_secret

AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_S3_REGION=eu-central-1
AWS_S3_BUCKET_NAME=your_bucket_name

DISCORD_CLIENT_ID=your_discord_client_id
DISCORD_CLIENT_SECRET=your_discord_client_secret
```

4. Create the database:
```bash
createdb ucmconnect
```

5. Run with the local profile (uses `application-local.properties`):
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

API: `http://localhost:8080/api/v1`  
Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`

---

## Running Tests

```bash
./mvnw test
```

Integration tests use a real PostgreSQL instance (configured in `application-performance.properties`). Unit tests mock the service layer.

---

## Deployment

The app deploys to AWS Elastic Beanstalk. All credentials are set as environment variables in the EB environment — nothing is committed to the repository.

```bash
./deploy.sh
```

The script builds the JAR, runs tests, and calls `eb deploy`.

---

## Security

See [security/SECURITY-AUDIT.md](security/SECURITY-AUDIT.md) for the full security audit report and procedures.

---

## License

MIT
