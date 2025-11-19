# 17-track 查询微服务 API 说明

> 所有接口均为无状态 REST API，默认 `Content-Type: application/json`，编码 UTF-8。
> 基础 URL 示例：`https://track.example.com`。

## 1. 查询类接口

### 1.1 GET /api/trackings/{trackingNo}
- **说明**：按单号查询最新状态，优先从 Redis 缓存读取。
- **请求路径参数**
  - `trackingNo` *(string, required)*：17-track 单号，最大 64 字符。
- **可选查询参数**
  - `forceRefresh` *(boolean, default=false)*：若为 true，命中缓存也触发后台异步刷新。
- **响应 (200)**
```json
{
  "trackingNo": "YT1234567890",
  "carrierCode": "YANWEN",
  "status": "IN_TRANSIT",
  "statusDesc": "Package departed facility",
  "lastEventTime": "2023-09-01T09:30:00Z",
  "refreshedAt": "2023-09-01T09:35:12Z",
  "stale": false,
  "events": [
    {
      "time": "2023-09-01T09:30:00Z",
      "location": "Shenzhen",
      "description": "Departed facility"
    }
  ],
  "source": "api-upload"
}
```
- **错误码**
  - 404：不存在该单号。
  - 429：刷新队列已满或速率受限。
  - 502：17-track API 异常（仅在直连失败且无缓存时返回）。

### 1.2 POST /api/trackings/query
- **说明**：批量查询，最大 50 条。
- **请求体**
```json
{
  "trackingNos": ["YT123", "YT456"],
  "forceRefresh": false
}
```
- **响应 (200)**
```json
{
  "results": [
    { "trackingNo": "YT123", "status": "DELIVERED", ... },
    { "trackingNo": "YT456", "status": "IN_TRANSIT", ... }
  ],
  "failed": [
    { "trackingNo": "YT789", "error": "NOT_FOUND" }
  ]
}
```
- **幂等性**：由 trackingNo 数组及 forceRefresh 计算哈希作为幂等键。

## 2. 单号接入接口

### 2.1 POST /api/trackings
- **说明**：提交待追踪单号，可批量。
- **请求体**
```json
{
  "trackingNos": ["YT123", "YT456"],
  "carrierCode": "YANWEN",
  "source": "wms",
  "priority": "NORMAL",
  "metadata": {
    "orderId": "SO20230901001"
  }
}
```
- **响应 (202)**
```json
{
  "accepted": ["YT123", "YT456"],
  "duplicates": ["YT123"],
  "rejected": [
    { "trackingNo": "YT999", "reason": "INVALID_FORMAT" }
  ]
}
```
- **行为**：入参校验后写入 Redis 队列，刷新 Worker 异步处理。

### 2.2 POST /api/trackings/upload
- **说明**：手动上传 CSV/Excel 的文件 URL。
- **请求体**
```json
{
  "fileUrl": "https://oss.example.com/batch1.csv",
  "source": "manual",
  "carrierCode": "AUTO"
}
```
- **响应 (202)**：返回 `jobId`，供后续查询导入进度。
- **进度查询**：`GET /api/trackings/upload/{jobId}` → 返回总条数、成功/失败统计及失败原因列表。

### 2.3 POST /api/webhooks/17track
- **说明**：监听外部推送的单号或状态更新。
- **Headers**
  - `X-Signature`: HMAC-SHA256 计算的签名。
- **请求体示例**
```json
{
  "event": "TRACKING_NUMBER",
  "trackingNo": "YT123",
  "status": "IN_TRANSIT",
  "payload": { ... }
}
```
- **响应**：200 表示接收成功；如签名失败返回 401。
- **安全**：通过签名 + IP 白名单校验。

## 3. 刷新与任务接口（内部/可选暴露）

### 3.1 POST /api/trackings/{trackingNo}/refresh
- **说明**：手工触发单号刷新，仅供平台服务调用。
- **响应**
```json
{
  "trackingNo": "YT123",
  "queued": true,
  "reason": "manual_refresh"
}
```

### 3.2 GET /api/trackings/{trackingNo}/history
- **说明**：返回状态变更历史及事件日志，方便调试。

## 4. 错误码约定
| code | message | 场景 |
| --- | --- | --- |
| INVALID_ARGUMENT | 参数格式错误 | JSON 解析失败、必填缺失 |
| NOT_FOUND | 单号不存在 | 查询接口 |
| RATE_LIMITED | 触发服务端限流 | 刷新压力过大 |
| UPSTREAM_ERROR | 17-track 返回异常 | 回源失败 |
| DUPLICATED | 单号重复提交 | 上传/队列 |

## 5. 版本与兼容性
- 通过 `X-API-Version` 头标记版本；默认 `v1`。
- 向后兼容策略：添加字段时保持可选，重大变更通过新路径 `/api/v2/...` 提供。

## 6. 示例集成流程
1. 调用 `POST /api/trackings` 上传单号。
2. 后台异步刷新，状态变化后可通过事件流/钩子通知下游。
3. 下游系统周期性调用 `GET /api/trackings/{trackingNo}` 校验状态。
4. 如需批量查询或导入，使用 `POST /api/trackings/query` 与 `POST /api/trackings/upload`。

## 7. OpenAPI 参考
- 未来将提供 `docs/openapi/track-service.yaml` 供客户端自动生成。
- 当前文档可转换为 OpenAPI：每个端点的请求/响应模型见上文。
