# Drawing QR Image Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 接收前端生成的二维码图片，按“二维码业务类型 + 订单ID”保存到文件系统，并在生成图纸时读取该图片嵌入 Excel，同时记录图纸版本实际使用的二维码文件。

**Architecture:** 复用 `file_detail.object_type/object_id` 作为订单当前二维码的关联关系，新增独立二维码上传接口；图纸生成时按订单获取当前二维码文件并下载字节，`DrawingExcelBuilder` 只接收前端图片字节，不再由后端生成二维码。`design_drawing.qr_file_id` 保存版本快照，避免订单后续替换二维码影响历史图纸。

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Apache POI, JUnit 5, Mockito, existing FileService/x-file-storage.

---

### Task 1: Extend the file business type and drawing schema

**Files:**
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/FileBizTypeEnum.java`
- Modify: `sql/ddl.sql`
- Modify: `sql/ddl-prod.sql`
- Modify: `sql/init.sql`
- Create: `sql/migration-drawing-qr-image-2026-07-17.sql`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/entity/DesignDrawingEntity.java`
- Modify: `yigongbao-parent/yigongbao-module-design/pom.xml`
- Test: `yigongbao-parent/yigongbao-common/src/test/java/com/yigongbao/common/enums/FileBizTypeEnumTest.java` (existing test file or the nearest enum test)
- Test: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/entity/DesignDrawingEntityTest.java`

- [ ] **Step 1: Write the failing enum/schema assertions**

  In the common module assert that `10.21` resolves through `FileBizTypeEnum.getByDictCode`; in the design module add a separate entity/service expectation that a drawing can hold `qrFileId`.

- [ ] **Step 2: Run the focused test and verify it fails**

Run from `yigongbao-parent/`: `mvn -pl yigongbao-common -am -Dtest=FileBizTypeEnumTest test`, then `mvn -pl yigongbao-module-design -am -Dtest=DesignDrawingEntityTest test`.

  Expected: FAIL because the new business type and entity field do not exist.

- [ ] **Step 3: Add `DRAWING_QR_IMAGE` and `qr_file_id`**

  Add a distinct file business type, add the nullable `qrFileId` field to `DesignDrawingEntity`, add matching nullable columns to both development and production DDL, seed `10.21` in `sql/init.sql`, and add an idempotent production migration that adds `design_drawing.qr_file_id` and inserts the missing `sys_dict` row for `10.21`.

- [ ] **Step 4: Run the focused test and verify it passes**

  Run from the repository root: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-common -am -Dtest=FileBizTypeEnumTest test`, then `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-design -am -Dtest=DesignDrawingEntityTest test`.

- [ ] **Step 5: Commit**

  `git add ... && git commit -m "feat: add drawing QR file metadata"`

### Task 2: Add the independent order QR image upload service and endpoint

**Files:**
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignQrImageService.java`
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignQrImageServiceImpl.java`
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignQrImageVO.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignDocController.java`
- Test: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignQrImageServiceImplTest.java`
- Test: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/controller/DesignDocControllerTest.java` (if the existing controller test pattern supports multipart)

- [ ] **Step 1: Write failing service tests**

  Cover: upload PNG, replace the current order QR association, return the current file for an identical MD5 before any upload, reject blank/unsupported images, and return the new file ID. Mock only `FileService` and order validation dependencies.

- [ ] **Step 2: Run the focused service test and verify it fails**

Run from `yigongbao-parent/`: `mvn -pl yigongbao-module-design -am -Dtest=DesignQrImageServiceImplTest test`

  Expected: FAIL because the service and endpoint do not exist.

- [ ] **Step 3: Implement upload and replacement semantics**

  Validate a non-empty PNG upload (PNG magic bytes, `.png` extension/content type, and a bounded size), read its bytes, compute MD5, and compare with the current file hash before calling `FileService.uploadFile`; identical content returns the current association without creating an orphan. For a changed image, upload unlinked, clear the current association using `unlinkByBiz("10.21", orderId)`, associate the new file with `linkFile(fileId, "10.21", orderId)`, and keep old physical files because historical `design_drawing.qr_file_id` values may reference them. Wrap replacement in a transaction and use a process-local order lock; across multiple application instances the explicitly documented rule is last successful replacement wins. If linking fails, call `deleteById` for the new unlinked file; if cleanup also fails, log it for operations. There is no current automated orphan-cleanup task, so unlinked files that cannot be deleted are retained for manual operations cleanup; historical snapshot-referenced files are never deleted by this feature. Use MD5 consistently with `FileVO.fileHash`; a blank current hash is treated as non-matching. Return a compact VO containing file ID, URL, size, and upload time.

- [ ] **Step 4: Add the controller route**

  Add `POST /design/workorder/{orderId}/qr-image` and `GET /design/workorder/{orderId}/qr-image`. Both routes validate that the order is readable through the existing `DesignQueryHelper`; POST additionally validates the design phase and accepts `@RequestParam("file") MultipartFile`, while GET only returns metadata. The GET returns the current file ID/metadata so either the QR viewer or drawing viewer can discover whether upload has already happened; when absent it returns a successful response with `data=null`. Add `@MockBean DesignQrImageService` to the controller test and cover both routes plus authorization delegation.

- [ ] **Step 5: Run focused tests and verify they pass**

Run from `yigongbao-parent/`: `mvn -pl yigongbao-module-design -am -Dtest=DesignQrImageServiceImplTest,DesignDocControllerTest test`

- [ ] **Step 6: Commit**

  `git add ... && git commit -m "feat: add order drawing QR image upload"`

### Task 3: Change the Excel builder to consume frontend QR bytes

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/helper/DrawingExcelBuilder.java`
- Modify: `yigongbao-parent/yigongbao-module-design/pom.xml`
- Modify: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/helper/DrawingExcelBuilderTest.java`

- [ ] **Step 1: Write failing builder tests**

  Assert that supplied `qrBytes` are embedded into the workbook, square and non-square QR images keep their aspect ratio inside the QR cell range, unreadable-but-accepted bytes still use the rectangular fallback anchor, and the builder no longer creates a QR image from a URL. Keep the existing multi-page and screenshot tests green.

- [ ] **Step 2: Run the focused test and verify it fails**

Run from `yigongbao-parent/`: `mvn -pl yigongbao-module-design -am -Dtest=DrawingExcelBuilderTest test`

  Expected: FAIL because `BuildContext` does not expose frontend QR bytes and the current builder still generates QR code with ZXing.

- [ ] **Step 3: Implement the minimal builder change**

  Replace `viewerQrUrl` with `qrBytes`, remove ZXing QR generation and related imports/helpers, and reuse the supplied bytes for every page. Use the existing contain/center anchor calculation for QR images as well, with a rectangular fallback anchor so a valid upload is never omitted from the workbook. Remove the design module's now-unused ZXing dependencies, `ConfigService` injection, old QR URL builder, and related test stubs. Preserve the existing image insertion fallback behavior for other images.

- [ ] **Step 4: Run the focused test and verify it passes**

  Run from the repository root: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-design -Dtest=DrawingExcelBuilderTest test`

- [ ] **Step 5: Commit**

  `git add ... && git commit -m "refactor: embed frontend QR bytes in drawings"`

### Task 4: Read the current order QR file during drawing generation and snapshot it

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignDocServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignDocServiceImplTest.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignDocService.java` only if the generation contract needs a public QR preparation method.

- [ ] **Step 1: Write failing generation tests**

  Cover: current order QR file is resolved by `object_type=10.21 + object_id=orderId`, its bytes are downloaded and passed into `DrawingExcelBuilder`, and `DesignDrawingEntity.qrFileId` is saved on both new-version and overwrite paths. Cover both `downloadDrawing` and `getDrawingPreviewUrl` because both call `ensureDrawing`, the missing-file behavior, QR replacement invalidating an auto drawing, legacy auto drawings with null `qr_file_id`, and manual revised drawings remaining unchanged when the current QR is replaced. Use an order+package lock around `ensureDrawing` so concurrent preview/download requests share one generation; persist the QR snapshot only with the drawing metadata after the Excel upload succeeds, and delete a newly uploaded Excel file on metadata failure when possible.

- [ ] **Step 2: Run the focused test and verify it fails**

Run from `yigongbao-parent/`: `mvn -pl yigongbao-module-design -am -Dtest=DesignDocServiceImplTest test`

  Expected: FAIL because the builder context and drawing entity do not yet use QR file data.

- [ ] **Step 3: Implement current-file lookup and snapshot**

  Resolve the newest associated file from `fileService.listByBiz`, download it with `downloadToBytes`, set `ctx.qrBytes`, and persist `qrFileId` alongside the generated drawing file. When no current QR file exists or the stored file cannot be read, generate the existing backend viewer QR as a fallback and leave `qr_file_id` null to indicate that the version used fallback content. Make `ensureDrawing` compare the current QR file ID with the latest AUTO drawing snapshot: a changed QR invalidates/rebuilds an AUTO drawing, while a MANUAL drawing remains the source of truth and is not mutated or regenerated solely because the QR changed. A historical AUTO drawing with null `qr_file_id` is treated as stale when a current QR exists. If an existing drawing can be reused, a missing current QR does not invalidate that historical file; if generation is required, it succeeds with the backend fallback instead of returning an upload-required error.

- [ ] **Step 4: Run focused design tests**

Run from `yigongbao-parent/`: `mvn -pl yigongbao-module-design -am -Dtest=DesignDocServiceImplTest,DrawingExcelBuilderTest,DesignQrImageServiceImplTest test`

- [ ] **Step 5: Commit**

  `git add ... && git commit -m "feat: use stored frontend QR in drawing generation"`

### Task 5: Verify integration and document frontend contract

**Files:**
- Create: `docs/前端接入/图纸二维码上传接口.md`
- Modify: no frontend source in this repository; the repository currently contains no tracked frontend source tree.

- [ ] **Step 1: Document the frontend sequence**

  Document both entry points: QR viewer uploads after `vue-qr` generation; drawing viewer calls GET and, when needed, the same upload flow before requesting either `drawing/preview-url` or `drawing/download`. Document the GET endpoint, the `object_type=10.21 + object_id=orderId` relation, the upload validation, MD5 retry behavior, failure/orphan manual-cleanup policy, and the fact that QR replacement regenerates AUTO drawings but does not mutate MANUAL drawings. State explicitly that the existing frontend QR content-generation configuration/API remains the single source of QR content; this change only receives its rendered PNG and does not add a duplicate content endpoint.

- [ ] **Step 2: Run all design module tests**

Run from `yigongbao-parent/`: `mvn -pl yigongbao-module-design -am -Dtest=DrawingExcelBuilderTest,DesignQrImageServiceImplTest,DesignDocServiceImplTest,DesignDocControllerTest test`

  Expected: all focused design tests pass; separately record pre-existing failures from the baseline basic-module suite if they remain. Also run the common enum/entity tests and compile the design module to catch removed ZXing imports.

- [ ] **Step 3: Inspect the final diff and status**

  Run: `git diff --check`, `git status --short`, and review the changed files for unrelated edits.

- [ ] **Step 4: Commit**

  `git add ... && git commit -m "docs: document drawing QR upload integration"`
