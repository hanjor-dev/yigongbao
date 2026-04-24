# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Thinking & Planning Mode

「以第一性原理！从原始需求和问题本质出发，不从惯例或模板出发。
1. 不要假设我清楚自己想要什么。动机或目标不清晰时，停下来讨论。
2. 目标清晰但路径不是最短的，直接告诉我并建议更好的办法。
3. 遇到问题追根因，不打补丁。每个决策都要能回答"为什么"。
4. 输出说重点，砍掉一切不改变决策的信息。」

## Build & Run Commands

All Maven commands must be run from `yigongbao-parent/`:

```bash
# Full clean build
mvn clean package -DskipTests

# Run application (dev profile is active by default)
mvn -pl yigongbao-boot spring-boot:run

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=BodyPartServiceImplTest

# Run tests in a specific module
mvn test -pl yigongbao-module-basic
```

**Prerequisites:** Java 21, Maven 3.6+, MySQL 8 on `localhost:3307`, Redis on `localhost:6379`.

After startup: API at `http://localhost:8080/api/`, Swagger UI at `http://localhost:8080/api/swagger-ui.html`.

## Module Architecture

```
yigongbao-parent/
├── yigongbao-common/        # Shared entities, enums, exceptions, Result<T> wrapper
├── yigongbao-framework/     # AOP aspects, interceptors, Spring configs (SaToken, Redis, Jackson)
├── yigongbao-module-system/ # Users, roles, permissions, orgs, depts, dicts
├── yigongbao-module-basic/  # Master data: hospitals, doctors, products, body parts, files, areas
├── yigongbao-module-flow/   # Generic workflow/state-machine engine (Facade pattern)
├── yigongbao-module-order/  # Order management — integrates with flow module for status transitions
└── yigongbao-boot/          # Application entry point; aggregates all modules
```

`yigongbao-boot` depends on all business modules. Business modules depend on `common` and `framework`.

## Key Patterns

**Standard layer structure for each domain:**
`Controller → Service (interface + impl) → Mapper → Entity`

**Unified API response:** All endpoints return `Result<T>` from `yigongbao-common`. Use `Result.success(data)` and `Result.error(code, message)`.

**Base entity:** All tables extend `BaseEntity` which provides `id`, `createTime`, `updateTime`, `createBy`, `updateBy`, and `isDeleted` (soft-delete via MyBatis Plus `@TableLogic`).

**Request/Response separation:** Use DTOs for input and VOs for output. Never expose Entity objects directly in controllers.

**Permission enforcement:** Use the `@RequirePermission` annotation on service methods. The `PermissionAspect` in `yigongbao-framework` handles the check.

**Audit logging:** Apply `@OperationLog` on controller or service methods. The `OperationLogAspect` captures params, result, duration, and user context automatically.

**Query endpoints use POST + JSON body** (not GET + query string) for list/page/tree operations.

## Flow Module

`yigongbao-module-flow` is a reusable state-machine engine. Business modules (like `order`) must not implement their own status transitions — use `FlowFacade` to drive state changes. The flow module uses `FlowContext` to hold runtime state and delegates to `FlowStatusEnum`/`FlowPhaseEnum` for state definitions.

`FlowFacade` is the **only** public interface — never call flow services directly. Actions are defined in `FlowActionEnum` (25+ actions: CREATE, SUBMIT_ORDER, DATA_AUDIT_PASS, START_DESIGN, QC_PASS, REWORK, etc.). `TransitionResult` carries `targetPhase`, `targetStatus`, `initialStatus`, and a `phaseChanged` boolean — phase-advancing transitions are distinct from status-only transitions.

## Auth & Captcha

Login endpoint: `POST /system/auth/login` supports `PASSWORD`, `PHONE`, and `EMAIL` modes. Token is Bearer, stored in `Authorization` header (30-day timeout, concurrent login enabled).

Behavior verification uses **Tianai Captcha** (SLIDER default) with a two-stage flow:
1. `/image-captch/genCaptcha` → validate track → issue UUID token (2-min Redis TTL, prefix `captcha:secondary:`)
2. Login endpoint validates and invalidates the token (one-time use)

SaToken excluded paths (must stay in sync in both Filter and MVC Interceptor): static resources, auth endpoints, captcha, Swagger/OpenAPI, `/files/public/**`.

## Conversion & Validation

**Converters** (`*Convert` classes in each module's `convert/` package) use `BeanUtils.copyProperties()` — not MapStruct. Exclude nested collections explicitly: `BeanUtils.copyProperties(dto, entity, "items")`.

**Validators** (`*Validator` components) are reusable Spring components, not inline logic. They support validation modes (DRAFT / DIRECT / SUBMIT) and enforce "never trust frontend" — all name fields are overwritten from DB. Hospital scope is checked via `UserHospitalService`.

## Cross-Cutting Infrastructure

**Operation logs** (`@OperationLog`): async (`@Async`), auto-masks password/token/secret fields, captures IP, User-Agent, execution time. User info is pulled from SaToken session — the login endpoint is handled specially since the user isn't authenticated until after the method returns.

**Slow request detection**: `ResultInterceptor` warns (log level WARN) on requests exceeding 1 second.

**`@RequirePermission`**: annotation is wired but permission validation is not yet fully implemented (TODO in `PermissionAspect`).

## Testing

Tests use JUnit 5 + Mockito. Unit tests use `@ExtendWith(MockitoExtension.class)`. Integration tests use H2 via `application-test.yml` (SaToken interceptor is disabled in test profile).

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FooServiceImplTest {
    @Mock private FooMapper fooMapper;
    @InjectMocks private FooServiceImpl fooService;

    @BeforeEach
    void setUp() throws Exception {
        // Required when service extends ServiceImpl — inject baseMapper via reflection
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(fooService, fooMapper);
    }
}
```

## Database

SQL files are in `sql/`: `ddl.sql` (schema), `init.sql` (seed data). Tables use auto-increment PKs, soft-delete (`is_deleted`), and audit timestamp columns. Tree structures use `parent_id`. Enum values are stored as integers and mapped via `mybatis-plus.type-enums-package`.

## Tech Stack Reference

| Concern | Library |
|---|---|
| ORM | MyBatis Plus 3.5.8 |
| Auth | SaToken 1.37.0 |
| File storage | x-file-storage (Dromara) 2.3.0 |
| Utilities | Hutool 5.8.26 |
| API docs | SpringDoc OpenAPI 2.3.0 |
| Excel | Apache POI 5.2.5 |
