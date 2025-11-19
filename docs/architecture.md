# 17-track 查询微服务架构说明

## 1. 目标
- 对接 17-track 官方接口，提供标准 REST API，供多系统复用。
- 以 Java 17 + Spring Boot 微服务实现，部署在容器环境中，可水平扩展。
- 使用 MySQL 存储单号与最新轨迹，用 Redis 缓存热点数据、维护刷新队列与分布式锁。

## 2. 核心组件
1. **API 层**
   - Spring Boot Web 层暴露查询、上传、批量查询和 webhook 端点。
   - 统一入参校验、幂等键生成、序列化/反序列化。
2. **Ingestion Service**
   - 将上传或推送的单号写入 Redis Stream/List，补充来源、优先级等标签。
   - 负责 CSV 下载解析、批次拆分、异常回执。
3. **Refresh Worker**
   - Scheduler/线程池消费队列，串行或并行调用 17-track API。
   - 通过 Redis 分布式锁避免重复刷新，失败重试策略集中在此。
   - 更新 MySQL + Redis，并在状态变更时发送事件（Kafka/Redis Stream topic）。
4. **Query Service**
   - 提供查询封装，优先命中 Redis；未命中则回源 MySQL 或触发即时刷新。
   - 负责响应装配，包括轨迹列表、最后更新、来源渠道。
5. **监控与配置**
   - Spring Boot Actuator 暴露 `/actuator/health`, `/metrics`, `/prometheus`。
   - 应用配置使用 `application.yml`，敏感信息（API Key）注入自配置中心或 K8s Secret。

## 3. 数据流
1. **单号接入**：外部系统调用 `POST /api/trackings` 或 webhook → API 层校验 → Ingestion Service 写入 Redis 队列 → Refresh Worker 消费 → 17-track API → 更新数据库与缓存。
2. **状态刷新**：Scheduler 扫描需刷新单号（例如最近 6 小时未更新）→ 写入队列 → Refresh Worker → 数据落库并触发事件。
3. **查询**：外部系统调用查询接口 → Query Service 查 Redis → 命中返回；未命中查 MySQL 并可异步触发刷新 → 结果缓存。
4. **通知**（可选）：状态变化后由 Refresh Worker 写入 Kafka/Redis Stream，供其它系统订阅，实现“被动接收”能力。

## 4. 数据模型
- **trackings**（MySQL）
  | 字段 | 说明 |
  | --- | --- |
  | id | PK |
  | tracking_no | 运单号，唯一索引 |
  | carrier_code | 承运商/通道标识 |
  | status | 最近一次状态枚举 |
  | status_detail | JSON/文本，包含 17-track 返回的详细节点 |
  | last_event_time | 轨迹最新事件时间 |
  | refreshed_at | 最后刷新时间 |
  | source | 单号来源（api-upload, webhook, csv, system） |
  | created_at / updated_at | 审计字段 |

- **Redis 键设计**
  - `track:{trackingNo}`：JSON 缓存，TTL 2-6 小时。
  - `track:refreshing:{trackingNo}`：刷新锁，TTL 2 分钟。
  - `track:queue`：List/Stream 存放待刷新 payload。
  - `track:event:last-state:{trackingNo}`：上次推送状态哈希，用于判断是否需要通知。

## 5. 部署拓扑
- 以 Docker/Kubernetes 部署，推荐副本数 ≥ 2，启用水平自动扩缩。
- MySQL 与 Redis 由平台提供高可用集群，应用通过内网访问。
- 外部系统通过 API Gateway/Service Mesh 访问服务，若无网关，可直接通过内网 DNS 调用。
- 监控告警：Prometheus + Grafana 收集指标；ELK/EFK 收集日志。

## 6. 扩展与容错
- **扩展**：API 层与 Refresh Worker 可分别横向扩展；通过 Redis 队列实现天然负载均衡。
- **限流与退避**：对 17-track API 调用设置速率限制（例如 50 RPS），当触发限制时指数退避并记录告警。
- **重试**：刷新失败时立即重试 2 次，仍失败则写入死信队列等待人工处理。
- **降级**：当 17-track 不可用时，查询接口返回最近一次缓存并带上 `stale=true` 标记。
- **可观测性**：关键路径埋点耗时、成功率、队列堆积量；链路追踪（Zipkin/Jaeger）可选。

## 7. 接口契约管理
- 使用 OpenAPI 3.0（YAML/JSON）描述所有 REST API，存放于 `docs/api-spec.md`。
- 通过 Swagger UI 或 Stoplight 生成可视化文档，供对接系统自动生成客户端代码。
