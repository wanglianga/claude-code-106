# 县城中药房代煎配送与用药回访平台

基于 **Vue 3 + Spring Boot + PostgreSQL** 的中药处方履约平台：患者/诊所提交处方 → 药师审方（十八反十九畏、剂量、缺药替代、医保/自费）→ 抓药 → 代煎排队（锅号/浸泡/煎煮次数/包装袋数/配送波次）→ 煎药（扫描药包、起止记录、异常气味、漏袋、复核人）→ 配送/自提/老人代取 → 签收 → 用药回访（用药提醒、药房赔付、诊所合作评分）。缺药、改地址、特殊煎法遗漏、包装破损、配送超时、服药不适、补开发票等异常统一挂在同一条处方履约记录中处理，管理者可按医生、药味、锅号、配送员复盘异常。

## 原始需求

> 开发县城中药房代煎配送与用药回访平台，可采用 Vue 3、Spring Boot 和 PostgreSQL。患者或诊所提交中药处方后，平台记录药味、剂数、特殊煎法、先煎后下、是否加糖、配送地址、取药方式、医生信息和患者禁忌。药师审方时核对十八反十九畏、剂量异常、缺药替代、医保或自费结算，审方结论会影响抓药、收费和代煎排队。煎药房按处方剂数、锅号、浸泡时间、煎煮次数、包装袋数和配送波次安排任务，煎药员要扫描药包、记录煎煮开始结束、异常气味、漏袋和复核人。若处方缺药、患者临时改地址、特殊煎法遗漏、包装袋破损、配送超时、患者服药后不适或诊所要求补开发票，平台把患者、诊所、药师、煎药房、配送和财务放在同一条处方履约记录中处理。回访结果会影响后续用药提醒、药房赔付和诊所合作评分。平台还要处理诊所批量处方、老人代取、医保目录限制和夜间急煎。药师需要从处方审方、抓药、代煎、配送、签收到回访看到完整履约；患者提出不适或漏袋时，药房也能定位到锅号、包装人和配送节点。药房管理者还能按医生、药味、锅号和配送员复盘异常。

## 技术栈与结构

- 前端：Vue 3 + Vite + vue-router + axios（nginx 非 root 镜像托管，反向代理 `/api`）
- 后端：Spring Boot 3（Java 17）+ Spring Data JPA + Spring Security(JWT) + Actuator
- 数据库：PostgreSQL 16（仅 compose 内部网络，不发布宿主端口）

```
├── backend/            # Spring Boot 后端（多阶段 Dockerfile：maven 构建 + jre 运行，非 root + HEALTHCHECK）
│   └── src/main/java/com/tcm/
│       ├── config/     # 安全/JWT/全局异常/种子数据
│       ├── model/      # 15 个实体 + 13 个枚举
│       ├── repository/ # Spring Data JPA
│       ├── service/    # 审方/煎药/配送/异常/回访/财务/统计业务
│       └── controller/ # REST API
├── frontend/           # Vue 3 前端（多阶段 Dockerfile：node 构建 + nginx-unprivileged 运行）
├── docker-compose.yml  # db + backend + frontend，仅前端发布 ${CC_PUBLISH_PORT}
└── .env.example
```

## 一键启动（验证方式 = 宿主 docker compose up）

前置：已注入 `DOCKER_HOST`、`COMPOSE_PROJECT_NAME`、`CC_PUBLISH_PORT` 环境变量。

```bash
cp .env.example .env        # 可选，环境变量已注入时无需此步
docker compose up -d --build
docker compose ps           # 三个服务 healthy 后
docker compose port frontend 8080
# 浏览器访问 http://host.docker.internal:<上一步输出的端口>
```

验证完成后释放资源：

```bash
docker compose down
```

## 测试账号（逐角色）

| 角色 | 用户名 | 密码 | 权限说明 |
|---|---|---|---|
| 药房管理者 | `admin` | `admin123` | 全部功能 + 统计复盘（按医生/药味/锅号/配送员） |
| 药师 | `pharmacist` | `123456` | 审方、抓药、异常处理、回访、取药签收 |
| 煎药员 | `decocter` | `123456` | 煎药台：扫描药包、煎煮起止、异常气味、漏袋、复核人 |
| 配送员 | `courier` | `123456` | 配送台：认领派送、签收（另有 `courier2`/`123456` 孙配送） |
| 财务 | `finance` | `123456` | 发票列表、补开发票、结算与赔付汇总 |
| 诊所 | `clinic1` | `123456` | 提交处方（含批量模式）、本诊所处方/发票（另有 `clinic2`/`123456`） |
| 患者 | `patient1` | `123456` | 提交处方、查看本人处方履约（另有 `patient2`/`123456`） |

## 演示数据（首次启动自动初始化）

- 药材字典 45 味（含库存、单价、最大日剂量、医保目录标记；甘遂库存为 0 用于演示缺药替代）
- 十八反/十九畏配伍禁忌 16 组（甘草-甘遂、附子-半夏、藜芦-人参、丁香-郁金等）
- 3 家诊所、4 名医生、12 张覆盖全部状态的处方：
  - 待审方 5 张：含「甘草+甘遂」十八反 + 缺药处方、康民诊所同批次批量处方 3 张、夜间急煎（附子先煎）处方
  - 审方驳回 1 张（细辛超量）、代煎排队 1 张、煎药中 1 张
  - 配送中 1 张（煎药漏袋 1 袋 → 自动登记包装破损异常）
  - 已签收 1 张（诊所已申请补开发票，财务可演示补开）
  - 老人代取待取药 1 张
  - 已回访 1 张（服药不适 → 自动异常 + 用药提醒 + 诊所评分下降）

## 关键业务流（建议演示顺序）

1. **审方**：`pharmacist` 登录 → 审方台 → 选择「甘草+甘遂」处方，自动核对出十八反、甘遂缺药 → 可驳回或做缺药替代后「调整后通过」→ 选择医保/自费结算（非目录药味自动转自费）→ 审方通过后自动扣库存、开发票、进入抓药。
2. **抓药/代煎排队**：处方详情点「抓药完成」→ 自动生成煎药任务（锅号 G01-G08 轮换、浸泡 30 分钟、煎煮 2 次、袋数=剂数×2、按时段/急煎分配波次）。
3. **煎药**：`decocter` 登录 → 煎药台 → 输入处方编号扫描药包（不一致会拒绝）→ 开始煎煮 → 结束煎煮填写复核人；漏袋/异常气味自动登记异常；含先煎后下药味未确认执行 → 自动登记「特殊煎法遗漏」。
4. **配送**：`courier` 登录 → 配送台认领派送 → 签收；超过 `DELIVERY_TIMEOUT_HOURS`（默认 4 小时）自动标记超时并登记异常。自提/老人代取由药师在处方详情「取药签收」。
5. **异常**：任一角色在处方详情「上报异常」（患者临时改地址会同步更新配送单）；药师/管理者在异常中心处理并登记赔付金额。
6. **回访**：`pharmacist` → 回访管理 → 对签收处方登记；服药不适自动登记异常进入赔付流程，满意度影响诊所合作评分，可生成 3 天用药提醒（到期自动置为已提醒）。
7. **财务**：`finance` → 财务发票 → 对「补开申请中」的发票执行补开，关联异常自动完结。
8. **复盘**：`admin` → 统计复盘 → 按医生/药味/锅号/配送员查看异常分布与赔付，查看诊所合作评分。

## API 概览

- `POST /api/auth/login` 登录；`GET /api/auth/me` 当前用户
- `POST /api/prescriptions` 提交处方（诊所可用同一 `batchNo` 批量提交）；`GET /api/prescriptions?status=&batchNo=`；`GET /api/prescriptions/{id}` 完整履约记录
- `GET /api/prescriptions/{id}/precheck` 审方预检；`POST /api/prescriptions/{id}/review` 审方；`POST /api/prescriptions/{id}/dispense` 抓药
- `GET /api/decoct/queue`；`POST /api/decoct/tasks/{id}/scan|start|finish`
- `GET /api/deliveries[/pending]`；`POST /api/deliveries/{id}/dispatch|sign`；`POST /api/prescriptions/{id}/pickup-sign`
- `GET|POST /api/exceptions`；`POST /api/exceptions/{id}/resolve`
- `GET /api/followups[/pending]`；`POST /api/followups`；`GET /api/reminders`
- `GET /api/invoices`；`POST /api/invoices/{id}/reissue`；`GET /api/finance/summary`
- `GET /api/herbs`；`PUT /api/herbs/{id}/stock`；`GET /api/stats/dashboard|exceptions?dim=|clinics`

## 环境变量

见 `.env.example`：`POSTGRES_*` 数据库、`JWT_SECRET`、`DELIVERY_TIMEOUT_HOURS` 配送超时阈值、`CC_PUBLISH_PORT` 宿主发布端口（仅前端映射 `${CC_PUBLISH_PORT}:8080`，db/backend 不发布端口）。
