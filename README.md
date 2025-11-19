# 17-track Microservice Wrapper

该仓库提供一个使用 Spring Boot 构建的微服务，包装 17TRACK 最新 API，为其它系统提供统一的运单查询、监听、更新与 webhook 接入能力。

## 关键特性

- **REST API**：`/api/v1/tracks` 提供查询、手工上传以及 webhook 接口。
- **MySQL + Redis**：JPA 持久化运单数据，Redis 缓存热点运单以降低官方 API 压力。
- **定时刷新**：`TrackingRefreshScheduler` 周期性刷新在途运单，确保状态同步。
- **Webhook 监听**：`/api/v1/tracks/webhook` 接收官方推送或第三方转发的运单状态。
- **占位符配置**：所有密钥、数据库账号等敏感信息均使用占位符，请通过外部配置或环境变量覆盖。

## 快速开始

1. **准备环境**
   - Java 17+
   - Maven 3.9+
   - MySQL、Redis 实例

2. **初始化数据库**
   - 执行 `docs/schema.sql` 中提供的建库建表脚本，或将内容复制到 DB 管理平台运行。
   - 如需自定义库名/字符集，可在脚本顶部直接修改。

3. **配置凭证**
   - 复制 `src/main/resources/application.yml`，或在部署环境中设置以下变量：
     - `TRACK17_API_KEY`、`TRACK17_API_SECRET`：来自 17TRACK 官方最新控制台。
     - `17TRACK_WEBHOOK_SECRET`：用于 webhook 签名校验（可选）。
     - 数据库/Redis 用户名密码等。

4. **配置 Maven 仓库（可选）**
   - 仓库根目录已提供 `.mvn/settings.xml`，默认将 `central` 指向可定制镜像地址。
   - 如需切换到公司内网或自建仓库，只需修改该文件中的 `<url>`，或在本地覆盖 `~/.m2/settings.xml`。
   - 所有 Maven 命令都会自动引用 `.mvn/maven.config` 中配置的 `-s` 参数，无需额外手动传入。

5. **构建与启动**
   ```bash
   mvn spring-boot:run
   ```

6. **典型调用**
   - 查询缓存：`GET /api/v1/tracks/{trackingNumber}`
   - 即时刷新：`POST /api/v1/tracks/query`，body 示例：
     ```json
     {
       "trackingNumber": "123456789CN",
       "carrierCode": "CNEMS"
     }
     ```
   - Webhook：`POST /api/v1/tracks/webhook`，body 结构见 `WebhookPayload`。

## 目录结构

```
src/
 └─ main/java/com/example/trackservice
    ├─ controller     # REST 接口
    ├─ service        # 业务逻辑、Webhook 处理
    ├─ client         # 17TRACK API 封装
    ├─ listener       # 定时刷新任务
    ├─ model          # JPA 实体
    └─ repository     # 数据访问
```

## 测试

```bash
mvn test
```

> **注意**：若运行 `mvn` 时出现因网络策略导致的仓库访问失败，可在 `.mvn/settings.xml` 中替换为可用的镜像；如需进一步诊断，可添加额外的 `<mirror>`、`<proxy>` 节点来满足所在环境的访问要求。

> 提示：若需与官方最新规范对齐，请根据 17TRACK 官方文档更新 `SeventeenTrackClient` 的路径、Header 和签名规则，本项目保留了易于替换的结构。
