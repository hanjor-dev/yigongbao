# 统一文件下载地址实施计划

> 依据：2026-08-25-file-download-url-unification-design.md

**目标：** 在不改变现有 fileUrl、权限和文件流转逻辑的前提下，为各业务返回的文件统一增加基于 x-file-storage 的临时 downloadUrl。

**架构：** basic 模块负责从已查询的 FileDetail/FileInfo 批量生成预签名 URL；业务模块只负责复用或复制通用文件字段。OSS/COS 平台通过 FileInfo.platform 自动选择，首期串行生成；file_detail 查询按 ID 或 URL 批量完成，避免 N+1 查询。

**技术栈：** Spring Boot、MyBatis-Plus、x-file-storage 2.3.0、JUnit 5、Mockito。

---

## 任务 1：核对真实 API 与现有影响域

- [x] 确认 GeneratePresignedUrlResult.getUrl()、putResponseHeaders()、setPlatform() 等 2.3.0 API。
- [x] 确认 FileDetail 到 FileInfo 的字段映射。
- [x] 列出所有自定义文件 VO 和只复制 fileUrl 的返回链路。
- [x] 确认 production URL-only 返回可通过 file_detail 的 URL 查询还原 FileInfo；数据库抽查已覆盖自动生成文件。

## 任务 2：统一下载 URL 生成器

测试文件：

~~~text
yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/file/service/impl/FileDownloadUrlServiceImplTest.java
~~~

实现文件：

~~~text
yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/file/service/FileDownloadUrlService.java
yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/file/service/impl/FileDownloadUrlServiceImpl.java
~~~

- [x] 先写 OSS/COS、中文文件名、响应头参数、批量顺序和异常策略测试。
- [x] 使用 getUrl() 和 putResponseHeaders() 实现单个 URL。
- [x] 实现串行批量生成，保持输入顺序，单项失败返回 null。
- [x] 对无 platform/path/filename 的数据定义失败结果，不输出完整签名 URL。
- [x] 运行测试确认 GREEN。

## 任务 3：FileVO 和 FileService 批量接入

- [x] FileVO 增加 downloadUrl。
- [x] FileService 增加按 ID、按 URL 批量生成下载地址能力。
- [x] FileServiceImpl 一次查询 file_detail，再转换 FileInfo，再批量生成。
- [x] FileRecorderService 保持纯映射，避免在 toFileVO 内产生 N+1 查询。
- [x] 增加下载地址有效期配置。
- [x] 运行 basic 模块测试和编译。

## 任务 4：订单及草稿接入

- [x] OrderFileVO/DraftFileVO 增加并复制 FileVO.downloadUrl。
- [x] 订单详情的三个文件列表使用带 downloadUrl 的批量查询。
- [x] 草稿详情的多分类文件查询使用统一文件转换。
- [ ] 增加订单详情和草稿详情回归测试（待接入真实存储测试夹具）。

## 任务 5：设计、生产、经典案例、影像接入

- [x] 修改只复制 fileUrl 的设计、生产和影像 VO 转换方法。
- [x] URL-only 记录通过一次 URL 批量查询获得 FileInfo，无法匹配时返回空 downloadUrl。
- [x] 不改变预览字段 fileUrl。
- [ ] 增加各模块最小回归测试（保留既有模块测试，新增 basic 生成器测试）。

## 任务 6：验证与提交

- [x] 运行 basic 相关测试。
- [x] 运行 order、design、production、imaging 跨模块编译验证。
- [ ] 用 dev OSS 配置和 prod COS 配置分别验证真实响应头（需部署环境凭证/对象访问条件）。
- [ ] 检查 git diff、git diff --check、工作区状态。
- [ ] 只提交本次功能相关文件，使用中文 Conventional Commit。
