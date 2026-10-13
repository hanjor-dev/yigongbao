# 文件统一下载地址设计方案

> 文档版本：1.0
> 创建日期：2026-08-25
> 作者：Codex
> 状态：设计中
> 归属模块：basic、order、design、production

---

## 一、背景与目标

### 1.1 背景

系统文件由 x-file-storage 统一管理，文件元数据持久化在 file_detail 表。开发环境使用阿里云 OSS，生产环境使用腾讯云 COS：

| 环境 | 平台标识 |
|---|---|
| dev/test | aliyun-oss-1 |
| prod | tencent-cos-1 |

现有接口返回 fileUrl。对象在 COS/OSS 中保存为随机文件名时，浏览器会按对象名下载，而不是按 file_detail.original_filename 下载。

直接在公开 URL 后追加 response-content-disposition 已验证不可作为统一方案：未签名的 URL 不会可靠应用响应头覆盖参数。

### 1.2 目标

1. 服务端统一生成带自定义下载文件名的临时预签名 URL。
2. 同一套业务代码兼容阿里云 OSS 和腾讯云 COS。
3. 从 file_detail 转换出的通用文件信息统一返回 downloadUrl。
4. 订单、设计、生产等业务文件 VO 复用统一能力。
5. 大文件保持 COS/OSS 直传浏览器，不经过后端文件流，不使用前端 Blob。
6. 避免每个文件单独查询 file_detail 造成 N+1 查询。
7. 保留 fileUrl，确保预览及旧前端逻辑不受影响。

### 1.3 非目标

- 不修改对象实际保存名。
- 不重新上传已有文件。
- 不删除或替换 fileUrl。
- 不引入 COS/OSS 两套业务 SDK。
- 不通过 fetch + Blob 下载大文件。
- 不改变现有文件权限模型。

---

## 二、现状调查与事实依据

### 2.1 订单详情链路

订单 Controller 位于：

~~~text
yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderController.java
~~~

Controller 只调用 orderMainService.getOrderDetail(id)。文件实际由 OrderMainServiceImpl#fillOrderFiles() 批量查询并分组返回：

~~~text
imageDataFiles
imageReportFiles
approvalFiles
~~~

现有订单文件 VO 已有 fileId、fileName、fileUrl、fileSize、fileExt 等字段，其中 fileName 对应 file_detail.original_filename。

### 2.2 统一文件转换链路

~~~text
FileServiceImpl
    ↓
FileRecorderService
    ↓
FileDetail
    ↓
FileVO
~~~

FileDetail 已保存生成预签名 URL 所需的 platform、path、filename、originalFilename、url 等信息。

FileRecorderService#toFileVO(FileDetail) 是通用实体到返回对象的转换点，但应保持为纯字段映射，不能在批量场景中逐条查询或逐条触发额外的文件处理。downloadUrl 由文件服务在批量查询完成后统一填充。

### 2.3 自动生成文件的数据库核验

已查询本地数据库 yigongbao.file_detail：

~~~text
有效文件记录：1969 条
aliyun-oss-1：18 条
tencent-cos-1：1951 条
~~~

指令单、图纸、流转卡等自动生成文件也已落入 file_detail。例如：

~~~text
孙延LJ流转卡.xlsx
platform = aliyun-oss-1
path = instruction_file/202608/
object_type = 10.8
~~~

~~~text
孙延LJ指令单.xlsx
platform = tencent-cos-1
path = instruction_file/202608/
object_type = 10.8
~~~

~~~text
孙延LJ-模型类图纸.xlsx
platform = tencent-cos-1
path = drawing_file/202608/
object_type = 10.7
~~~

因此本方案以 file_detail 为统一事实来源，不需要重新上传或通过 URL 反查历史文件。

---

## 三、核心设计

### 3.1 字段职责

所有通用文件 VO 增加：

~~~java
private String downloadUrl;
~~~

| 字段 | 职责 |
|---|---|
| fileUrl | 原始访问地址，预览和兼容已有逻辑 |
| downloadUrl | 临时预签名下载地址，带自定义文件名 |
| fileName | 浏览器下载显示的业务文件名 |
| fileId | 对应 file_detail.id |

downloadUrl 不写入数据库，因为它有过期时间，应在返回文件数据时动态生成。

### 3.2 统一生成服务

新增：

~~~text
yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/file/service/FileDownloadUrlService.java
yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/file/service/impl/FileDownloadUrlServiceImpl.java
~~~

接口建议：

~~~java
public interface FileDownloadUrlService {

    String generate(FileInfo fileInfo, String downloadFileName);

    List<String> generateBatch(List<FileDownloadUrlRequest> requests);
}
~~~

生成服务只负责签名，不负责查询数据库。调用方先批量查询 file_detail，再把 FileInfo 传入。

### 3.3 x-file-storage 预签名参数

~~~java
fileStorageService
        .generatePresignedUrl()
        .setPlatform(fileInfo.getPlatform())
        .setPath(fileInfo.getPath())
        .setFilename(fileInfo.getFilename())
        .setMethod(Constant.GeneratePresignedUrl.Method.GET)
        .setExpiration(expiration)
        .putResponseHeaders(
                Constant.Metadata.CONTENT_DISPOSITION,
                contentDisposition
        )
        .generatePresignedUrl();
~~~

Content-Disposition 必须通过 x-file-storage 的响应头覆盖参数参与签名，不能在签名 URL 生成后由前端追加；OSS/COS 适配器会把它转换为平台要求的 `response-content-disposition` 参数。

文件名使用：

~~~http
Content-Disposition: attachment; filename*=UTF-8''<encoded-file-name>
~~~

文件名必须清理 CR/LF，并进行 UTF-8 百分号编码；ASCII 兼容客户端使用安全的 `filename` 回退名，现代客户端使用 `filename*` 原始 UTF-8 文件名。

### 3.4 平台选择

业务代码不判断 COS 或 OSS，直接使用：

~~~java
fileInfo.getPlatform()
~~~

x-file-storage 根据 aliyun-oss-1 或 tencent-cos-1 自动选择对应平台的签名实现。

---

## 四、调用时机与数据流

### 4.1 推荐调用时机

~~~text
查询 file_detail
    ↓
批量转换 FileInfo
    ↓
批量生成 downloadUrl
    ↓
组装 FileVO / 业务 VO
    ↓
序列化响应
~~~

不在上传时永久保存 downloadUrl，不在 Controller 中逐字段拼接，也不由前端追加参数。

### 4.2 批量查询和批量生成

建议增加：

~~~java
List<FileVO> listByIdsWithDownloadUrl(List<String> ids);
~~~

内部流程：

1. 一次查询 file_detail。
2. 按结果转换 FileInfo。
3. 串行批量生成 downloadUrl。
4. 组装 FileVO。

订单详情使用：

~~~java
List<FileVO> fileVOs =
        fileService.listByIdsWithDownloadUrl(fileIds);
~~~

不使用逐文件 getById，避免 N+1 查询。

### 4.3 通用 FileVO 转换

FileRecorderService 保留纯转换职责，提供带 URL 和不带 URL 的重载：

~~~java
FileVO toFileVO(FileDetail detail);

FileVO toFileVO(FileDetail detail, String downloadUrl);
~~~

批量入口先生成地址，再调用第二个方法组装 VO。这样单文件接口和批量接口都能复用同一套字段映射，同时避免在 toFileVO() 内部产生 N 次签名调用或数据库访问。

对于单文件查询接口，可以由 FileService 在查询到 FileDetail 后调用一次生成服务；对于批量查询接口，必须走批量入口。禁止在 FileRecorderService.toFileVO() 内部隐式查询 file_detail 或调用远程存储服务。

### 4.4 订单文件 VO

订单当前使用嵌套 OrderDetailVO.OrderFileVO，不是直接返回 FileVO。推荐让它复用通用字段：

~~~java
public static class OrderFileVO extends FileVO {

    private String fileCategory;

    private String fileCategoryName;
}
~~~

OrderMainServiceImpl#toOrderFileVO() 只补充订单特有字段，不重新生成 downloadUrl。

如果暂不调整继承关系，至少从 FileVO 复制 downloadUrl：

~~~java
vo.setDownloadUrl(fileVO.getDownloadUrl());
~~~

### 4.5 其他影响域

需要统一检查所有返回文件信息的 VO：

| 模块 | 典型对象 | 处理方式 |
|---|---|---|
| basic | FileVO | 在统一转换层填充 |
| order | OrderFileVO、草稿文件 VO | 复用通用字段 |
| design | DesignPackageVO、DesignModelVO、DesignDocVersionVO、截图/二维码 VO | 优先由 FileVO 转换 |
| production | 流转卡、指令单、图纸相关 VO | 使用 file_detail 对应 FileVO |
| classic case | 经典案例文件 VO | 复用统一转换结果 |
| imaging | 影像文件 VO | 预览用 fileUrl，下载用 downloadUrl |

凡是可下载文件，都应提供明确的 downloadUrl；预览专用字段可继续只返回 fileUrl。对只复制 fileUrl 的自定义 VO，不能期待 FileVO 的新字段自动出现，必须改用公共文件转换器或显式复制 downloadUrl。

---

## 五、性能设计

### 5.1 主要风险

1. 为每个文件重复查询 file_detail。
2. 每个业务接口重复组装文件元数据。
3. 生成地址时误触发实际文件下载或远程 HEAD 请求。
4. 对几十个文件无界创建线程。

### 5.2 控制方式

1. 文件 ID 一次性批量查询。
2. 生成服务不查询数据库。
3. 预签名只做本地签名计算，不读取文件内容。
4. 默认串行批量生成，不使用 parallelStream。
5. 通过接口耗时监控判断是否需要优化。

验证时应分别记录三段耗时：file_detail 查询、预签名计算、JSON 序列化，避免把数据库或响应体耗时误判为签名生成耗时。

### 5.3 并发策略

首期不使用并发。预签名生成通常是本地计算，几十个文件的线程调度成本可能抵消收益；串行实现更容易测试并保持顺序。

保留 generateBatch() 抽象。若压测证明签名生成成为瓶颈，再在服务内部引入有界专用线程池，不改变业务调用方。

### 5.4 缓存策略

首期不增加缓存。URL 依赖有效期和文件名，缓存还需要以 fileId + downloadFileName 为 Key。后续如果监控证明同一文件重复生成成本明显，再增加 TTL 小于 URL 有效期的本地缓存。

---

## 六、兼容性与安全性

### 6.1 保留 fileUrl

不覆盖或删除 fileUrl。预签名 downloadUrl 会过期，不适合作为永久展示地址；现有图片预览和旧前端继续使用 fileUrl。

### 6.2 权限边界

预签名 URL 必须在现有业务权限校验之后生成。订单详情仍先执行数据权限校验，再生成文件下载地址。

不要新增允许前端传任意 fileId 即生成下载地址的公开接口，避免越权。

### 6.3 有效期

默认 10 分钟，可配置：

~~~yaml
yigongbao:
  file-storage:
    download-url-expire-minutes: 10
~~~

### 6.4 文件名安全

- 清理 CR/LF。
- 使用 UTF-8 编码。
- 优先使用 file_detail.original_filename。
- 不信任前端传入的文件名。

### 6.5 失败策略

默认单文件降级，不影响整个详情接口：

- 记录告警日志。
- downloadUrl 生成失败时保留原有 fileUrl，新增 downloadUrl 为空；受保护文件的下载按钮不得回退到 fileUrl，且不得把签名地址写入日志。
- 不因一个文件签名失败而丢弃其他文件。

---

## 七、接口示例

~~~json
{
  "fileId": "2092233664317784065",
  "fileName": "孙延LJ流转卡.xlsx",
  "fileUrl": "https://yigongbao.oss-cn-hangzhou.aliyuncs.com/instruction_file/202608/random.xlsx",
  "downloadUrl": "https://yigongbao.oss-cn-hangzhou.aliyuncs.com/instruction_file/202608/random.xlsx?...signature..."
}
~~~

前端仅访问：

~~~javascript
function downloadFile(file) {
  window.location.href = file.downloadUrl
}
~~~

不使用 fetch + Blob。

---

## 八、实现范围

### 8.1 basic 模块

| 操作 | 文件 | 内容 |
|---|---|---|
| Modify | file/vo/FileVO.java | 增加 downloadUrl |
| Create | file/service/FileDownloadUrlService.java | 统一生成接口 |
| Create | file/service/impl/FileDownloadUrlServiceImpl.java | x-file-storage 预签名实现 |
| Modify | file/service/FileService.java | 增加带下载地址的批量查询方法 |
| Modify | file/service/impl/FileServiceImpl.java | 批量查询、生成和组装 |
| Modify | file/service/impl/FileRecorderService.java | 支持带 downloadUrl 的 VO 转换 |
| Modify | file/config/FileStorageProperties.java | 下载有效期配置 |

### 8.2 order 模块

| 操作 | 文件 | 内容 |
|---|---|---|
| Modify | vo/order/OrderDetailVO.java | OrderFileVO 复用或增加 downloadUrl |
| Modify | service/impl/OrderMainServiceImpl.java | 使用批量带下载地址的文件查询 |
| Add/Modify | order tests | 验证三个文件列表均有 downloadUrl |

### 8.3 design、production、classic case、imaging

逐项检查返回文件 VO，优先将重复的 fileUrl/fileName 映射改为复用 FileVO；不改变业务表和已有存储数据。

---

## 九、测试与验收

### 9.1 单元测试

覆盖：

1. OSS 平台生成 GET 预签名 URL。
2. COS 平台生成 GET 预签名 URL。
3. URL 包含 response-content-disposition。
4. 中文文件名 UTF-8 编码正确。
5. CR/LF 被清理。
6. 空文件名回退 originalFilename。
7. 批量输入输出数量一致。
8. 批量输出顺序与输入一致。
9. 空列表返回空列表。
10. 单个失败时其余结果仍可返回。

### 9.2 服务层测试

订单详情测试验证：

1. imageDataFiles 有 downloadUrl。
2. imageReportFiles 有 downloadUrl。
3. approvalFiles 有 downloadUrl。
4. 原 fileUrl 不变。
5. 批量查询而非逐文件查询。

### 9.3 双平台集成验证

dev 使用 OSS，prod 使用 COS，检查实际响应头：

~~~http
Content-Disposition: attachment; filename*=UTF-8''<expected-name>
~~~

同时验证：

- 中文文件名正确。
- 大文件直接由浏览器从对象存储下载。
- 后端没有转发文件流。
- 预签名 URL 过期后访问失败。

### 9.4 回归验证

- 图片和影像预览仍使用 fileUrl。
- 文件上传返回结构兼容旧字段。
- 文件删除仍按原存储 URL 执行。
- 经典案例迁移不受影响。
- 本地存储配置仍能启动。
- 现有数据权限校验先于下载地址生成。

---

## 十、上线步骤

1. basic 增加 downloadUrl 和统一签名服务。
2. 增加单元测试、服务层测试和订单详情回归测试。
3. dev 使用 OSS 验证。
4. 检查 CDN/网关未移除响应头覆盖参数。
5. prod 使用 COS 验证。
6. 前端下载按钮切换为 downloadUrl。
7. 保留 fileUrl 作为兼容字段。
8. 监控签名失败、接口耗时和过期 URL。

本次不需要数据库迁移，现有 file_detail 已具备生成地址所需字段。

---

## 十一、风险与应对

| 风险 | 应对 |
|---|---|
| 自定义业务 VO 只复制 fileUrl | 复用 FileVO 或统一公共转换器 |
| 参数未参与签名 | 使用 putResponseHeaders 后再生成 URL |
| URL 过期 | 默认 10 分钟并监控 |
| CDN 丢弃参数 | 先用源站验证，再配置 CDN |
| 单条转换重复查询 | 批量查询后传递 FileInfo |
| 文件名响应头注入 | 清理 CR/LF 并编码 |
| 一个文件失败导致接口失败 | 默认单文件降级保留 fileUrl |
| 本地存储不支持云端预签名 | local-plus-1 使用原 URL 或现有后端下载接口，云存储平台使用预签名 URL |

---

## 十二、验收标准

1. 所有直接返回 FileVO 的文件接口带 downloadUrl。
2. 订单详情三个文件列表均带 downloadUrl。
3. dev OSS 文件按原始文件名下载。
4. prod COS 文件按原始文件名下载。
5. 下载不经过后端文件流、不进入前端 Blob。
6. 多文件接口无 N+1 查询。
7. fileUrl、预览、删除、上传和权限逻辑兼容。
8. 单元测试、服务层测试和双平台集成验证通过。
