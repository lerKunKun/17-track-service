# 查询微服务功能列表

## 1. 对外接口
- `GET /api/trackings/{trackingNo}`：单号查询，返回状态、承运商、最后更新时间、轨迹摘要。
- `POST /api/trackings/query`：批量查询（<=50 条），返回数组及失败原因。
- `POST /api/trackings`：上传单号（单条/批次），支持携带来源、备注、优先级。
- Webhook `POST /api/webhooks/17track`：接收外部推送的单号或状态更新，含签名校验与幂等处理。

## 2. 状态刷新
- 定时刷新任务：按配置频率扫描待刷新列表，调用 17-track API。
- 刷新队列：Redis List/Stream 存放待刷新单号，支持优先级。
- 刷新锁：Redis Key 防止重复刷新，自动过期。
- 状态落库：将最新轨迹摘要、更新时间写入 MySQL；冗余 JSON 存入 Redis。

## 3. 事件通知
- 状态变更检测：对比上次状态，发生变化时写入事件表或推送到消息通道（Kafka/Redis Stream）。
- 下游订阅：提供可配置的推送渠道（HTTP 回调预留）或供下游轮询查询。

## 4. 配置与监控
- 配置项：API Key、刷新频率、批处理大小、超时/重试次数，通过配置文件或环境变量管理。
- 健康检查：Spring Boot Actuator `/health`、`/info`、`/metrics`。
- 日志：记录每次 API 请求、刷新结果、外部推送校验情况，便于追溯。

## 5. 数据管理
- MySQL 表：tracking（单号、承运商、最新状态、更新时间、来源）、tracking_events（历史轨迹/状态变更）。
- Redis：缓存最新状态、刷新锁、待刷新队列、幂等标记。
- 数据保留策略：可配置状态历史保留天数，周期性清理过期记录。
