# design模块初始化

> **任务序号**：2
> **状态**：已完成
> **前置依赖**：01_数据库设计

---

## 一、任务概述

创建 `yigongbao-module-design` 模块，初始化项目结构，配置模块依赖。

---

## 二、模块结构

```
yigongbao-module-design/
├── pom.xml
└── src/main/java/com/yigongbao/module/design/
    ├── controller/
    ├── service/
    │   └── impl/
    ├── mapper/
    ├── entity/
    ├── vo/
    ├── dto/
    ├── convert/
    └── enums/
```

---

## 三、模块依赖

```xml
<dependencies>
    <!-- order：订单信息查询（传递引入 flow、basic、system） -->
    <dependency>
        <groupId>com.yigongbao</groupId>
        <artifactId>yigongbao-module-order</artifactId>
    </dependency>
</dependencies>
```

> **说明**：order 模块已依赖 flow、basic、system，通过 Maven 传递依赖机制自动引入，无需重复声明。

---

## 四、Entity 清单

| 序号 | 类名 | 对应表 |
|:----:|------|--------|
| 1 | DesignPackageEntity | design_package |
| 2 | DesignPackageFileEntity | design_package_file |
| 3 | DesignProductEntity | design_product |
| 4 | DesignInstructionEntity | design_instruction |
| 5 | DesignDrawingEntity | design_drawing |
| 6 | DesignModelEntity | design_model |
| 7 | DesignReviewEntity | design_review |

---

## 五、Mapper 清单

| 序号 | 类名 | 说明 |
|:----:|------|------|
| 1 | DesignPackageMapper | 数据包 CRUD |
| 2 | DesignPackageFileMapper | 包内文件 CRUD |
| 3 | DesignProductMapper | 打印产品 CRUD |
| 4 | DesignInstructionMapper | 指令单 CRUD |
| 5 | DesignDrawingMapper | 图纸 CRUD |
| 6 | DesignModelMapper | 可视化模型 CRUD |
| 7 | DesignReviewMapper | 审核记录 CRUD |

---

## 六、枚举清单

| 序号 | 类名 | 说明 |
|:----:|------|------|
| 1 | DesignModeEnum | 设计模式：1=线下修改，2=在线编辑 |
| 2 | ReviewResultEnum | 审核结果：0=驳回，1=通过 |

---

## 七、完成清单

- [x] 创建模块目录结构
- [x] 配置 pom.xml
- [x] 创建 7 个 Entity
- [x] 创建 7 个 Mapper
- [x] 创建 2 个 Enum
- [x] 更新父 pom.xml（模块声明 + 版本管理）
- [x] 更新 boot 模块依赖
- [x] 验证编译通过

---

*文档版本：v1.2 | 最后更新：2026-04-15*
