# CangJie 遥测系统 API 文档

## API 结构概览

遥测系统 API 分为两个主要部分：

1. `/api/telemetry` - 遥测数据收集 API
2. `/api/v1` - 管理后台 API

### 基础 URL

所有 API 都以 `/api` 为前缀。

## 遥测数据收集 API

### 基本端点

- `POST /api/telemetry` - 提交遥测数据
- `GET /api/telemetry` - 检查遥测服务状态

### 遥测数据查询

- `GET /api/telemetry/events` - 获取最近的遥测事件
  - 参数：`limit` - 限制返回的事件数量

- `GET /api/telemetry/metadata` - 获取最近的遥测元数据
  - 参数：`limit` - 限制返回的元数据数量

## 管理后台 API

### 认证 API

- `POST /api/v1/auth/login` - 用户登录
  - 请求体：`{ "username": "string", "password": "string" }`

- `POST /api/v1/auth/logout` - 用户登出

- `GET /api/v1/auth/me` - 获取当前用户信息

### 用户管理 API

- `GET /api/v1/users` - 获取所有用户
  - 参数：`page`, `pageSize`

- `POST /api/v1/users` - 创建新用户

- `GET /api/v1/users/{username}` - 获取指定用户信息

- `PUT /api/v1/users/{username}` - 更新用户信息

- `DELETE /api/v1/users/{username}` - 删除用户

### 仪表盘 API

- `GET /api/v1/dashboard` - 获取仪表盘数据

### 事件 API

- `GET /api/v1/events` - 获取事件列表
  - 参数：`page`, `pageSize`, `category`, `name`, `startDate`, `endDate`, `groupBy`

- `DELETE /api/v1/events/{id}` - 删除单个事件

- `POST /api/v1/events/bulk-delete` - 批量删除事件
  - 请求体：`{ "ids": ["string"] }`

### 元数据 API

- `GET /api/v1/metadata` - 获取元数据列表
  - 参数：`page`, `pageSize`

### 报表 API

- `GET /api/v1/reports` - 获取报表数据
  - 参数：`timeRange`, `category`, `groupBy`

## 响应格式

大多数 API 响应使用以下标准格式：

```json
{
  "success": true,
  "message": "操作成功",
  "data": { ... }
}
```

错误响应：

```json
{
  "success": false,
  "message": "错误信息",
  "data": null
}
``` 