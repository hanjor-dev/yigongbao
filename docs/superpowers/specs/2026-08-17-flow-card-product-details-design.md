# 流转卡产品明细导出修正设计

## 目标

修正 `generateFlowCardExcel` 导出的产品明细列表：确保产品编号沿用模板中 A 列已有的居中样式，并让“描述”列显示打印文件名（去除路径和最后一个扩展名）。

## 数据流

`ProductionRecordServiceImpl.generateFlowCardExcel` 已查询到 `ProductionProductEntity` 列表。映射为 `FlowCardExcelBuilder.ProductInfo` 时新增并传递 `fileName`。构建器填充产品列表的“描述”列时，对该值取基础文件名并移除最后一个扩展名。

## 样式处理

产品编号仅通过现有 `setCellValue` 写入模板中对应的 A 列单元格，不额外创建、克隆或设置 A 列样式。对于动态增加的产品行，继续沿用模板行的整行样式复制逻辑，使模板预设的 A 列水平居中自然生效。

## 边界行为

- `fileName` 为空、空白或仅含路径时，“描述”列按现有空值策略显示 `-`。
- 含路径的文件名只显示最终文件名部分。
- 仅移除最后一个扩展名；无扩展名或以点开头的文件名保持原样。

## 验证

新增/调整 `FlowCardExcelBuilderTest`，验证多条产品明细生成后：

1. 每一行 A 列的产品编号保留模板的水平居中对齐。
2. “描述”列显示不含路径和扩展名的 `fileName`，不再显示颜色与材质。
