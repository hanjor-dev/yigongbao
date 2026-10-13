/# 3D 医学影像查看器 API 文档

## 一、初始化参数

**路由**：`/#/viewer?kv=base64`

`kv` 接收一个 JSON 的 Base64 编码字符串。

```json
{
  "paths": {
    "dcmPath": {
      "path": "",
      "params": {},
      "type": "post" //接口类型
    },
    "stlPath": {
      "path": "",
      "params": {},
      "type": "post"//接口类型
    },
    "markPath": {
      "path": "",
      "params": {},
      "type": "post"//接口类型
    }
  },
  "token": {"token":"dqwdewiewhjiwdq"}//header中加入的token key和value
}
```
**dcmPath和stlPath都存在2d，3d可以切换显示，只存在一个就只能访问其中一个**

**只要markPath存在的才可以标注数据提交**

| 字段    | 类型   | 必填 | 说明                                   |
|---------|--------|------|----------------------------------------|
| paths    | Object | 是   | 各模块接口路径配置                       |
| token   | Object | 否   | 携带 Token 的接口会将其中每个 key-value 注入到请求 Header |

| paths 字段 | 类型   | 说明                                           |
|-----------|--------|------------------------------------------------|
| dcmPath   | Object | DICOM 数据获取接口，`path` 为地址，`params` 为提交参数，`type` 为请求类型（get/post） |
| stlPath   | Object | STL 数据获取接口，`path` 为地址，`params` 为提交参数，`type` 为请求类型（get/post）  |
| markPath  | Object | 标注截图提交接口，`path` 为地址，`params` 为提交参数，`type` 为请求类型（get/post）   |

---

## 二、DICOM 数据接口
**请求地址**：`paths.dcmPath.path`
**参数**：`paths.dcmPath.params`
**方法类型**：`paths.dcmPath.type`
### 响应格式

```json
{
  "code": "200",
  "data": [
    "url数据1.zip",
    "url数据2.zip"
  ],
  "message": ""
}
```

| 字段   | 类型           | 说明                    |
|--------|----------------|-------------------------|
| code   | String/Number  | 状态码                  |
| data   | Array\<String\> | DICOM 压缩包 URL 列表，支持 zip / rar / 7z / tar 格式 |
| message| String         | 提示信息                |

---

**路由**：`/#/modelView?kv=base64`

## 三、STL 数据接口
**请求地址**：`paths.stlPath.path`
**参数**：`paths.stlPath.params`
**方法类型**：`paths.stlPath.type`
### 响应格式

```json
{
  "code": "200",
  "data": {
    "isGroup": true,
    "list": [
      {
        "groupId": "",
        "groupName": "",
        "stls": [
          {
            "id": "",
            "stlName": "",
            "color": "255,0,255",
            "opacity": 1
          }
        ]
      }
    ]
  },
  "message": ""
}
```

| 字段          | 类型    | 说明                                      |
|---------------|---------|-------------------------------------------|
| isGroup       | Boolean | `true` — 分组展示；`false` — 非分组展示     |
| groupId       | String  | 分组唯一标识（非分组时可不传）              |
| groupName     | String  | 分组名称（非分组时可不传）                  |
| id            | String  | STL 模型唯一标识                           |
| stlName       | String  | STL 模型名称                               |
| color         | String  | 颜色值，支持 HEX / RGB / 颜色名，如 `#ffffff`、`255,255,255` |
| opacity       | Number  | 透明度，范围 `0 ~ 1`，`1` 为不透明          |

---

## 四、标注数据提交接口

**请求地址**：`paths.markPath.path`
**参数**：`paths.markPath.params`
**类型**：`paths.markPath.type`

**请求格式**：`multipart/form-data`

标注提交时，`file` 和 `params` 中的参数一并作为 FormData 字段提交。

| 参数名    | 类型   | 必填 | 说明               |
|-----------|--------|------|--------------------|
| file      | File   | 是   | 截图文件（PNG/JPG） |
| groupId   | String | 否   | 分组标识            |
| id        | String | 否   | 模型标识            |

> `params` 中的自定义字段（如 `orderId` 等）也一并随 `file` 提交，具体字段由业务方定义。
