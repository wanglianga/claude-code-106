# 同济堂·县城中药房代煎配送与用药回访平台

基于 **Vue 3 + Spring Boot 3 + PostgreSQL 16** 的 0-1 演示工程,围绕"一条处方履约记录"贯通:
患者/诊所提交处方 → 药师审方(十八反十九畏、剂量异常、缺药替代、医保目录、结算)→ 抓药扣费 →
煎药房排锅/扫码/煎煮/包装复核(浸泡、次数、袋数、波次、异常气味、漏袋)→ 配送派单/出库/签收(波次、
老人代取、改地址、超时)→ 用药回访(不适联动提醒暂停、药房赔付、诊所合作评分)→ 管理者按医生、
药味、锅号、配送员复盘异常。

## 原始需求

> 开发县城中药房代煎配送与用药回访平台,可采用 Vue 3、Spring Boot 和 PostgreSQL。患者或诊所提交中药处方后,平台记录药味、剂数、特殊煎法、先煎后下、是否加糖、配送地址、取药方式、医生信息和患者禁忌。药师审方时核对十八反十九畏、剂量异常、缺药替代、医保或自费结算,审方结论会影响抓药、收费和代煎排队。煎药房按处方剂数、锅号、浸泡时间、煎煮次数、包装袋数和配送波次安排任务,煎药员要扫描药包、记录煎煮开始结束、异常气味、漏袋和复核人。若处方缺药、患者临时改地址、特殊煎法遗漏、包装袋破损、配送超时、患者服药后不适或诊所要求补开发票,平台把患者、诊所、药师、煎药房、配送和财务放在同一条处方履约记录中处理。回访结果会影响后续用药提醒、药房赔付和诊所合作评分。平台还要处理诊所批量处方、老人代取、医保目录限制和夜间急煎。药师需要从处方审方、抓药、代煎、配送、签收到回访看到完整履约;患者提出不适或漏袋时,药房也能定位到锅号、包装人和配送节点。药房管理者还能按医生、药味、锅号和配送员复盘异常。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3(Composition API)+ Vue Router 4 + Vite 5,纯手写 CSS,无重型 UI 库 |
| 后端 | Spring Boot 3.3 / Java 21 / Spring Security + JWT(jjwt)/ Spring Data JPA / Bean Validation / Actuator |
| 数据库 | PostgreSQL 16(仅 compose 内网,不发布到宿主) |
| 部署 | Docker Compose;后端多阶段 Dockerfile(maven 构建 → jre 运行,非 root + HEALTHCHECK);前端多阶段(node 构建 → nginx,非 root + HEALTHCHECK) |

## 一键启动(宿主 docker compose up)

> 验证方式 = 在宿主机用 `docker compose up` 启动。数据库不发布宿主端口,仅 Web(应用)端口通过
> `CC_PUBLISH_PORT` 发布。

```bash
cp .env.example .env        # 可按需修改 CC_PUBLISH_PORT(默认 3106)
docker compose up -d --build
# 查看健康状态
docker compose ps
# 取实际映射端口(避免与并行任务端口冲突)
docker compose port web 8080
```

浏览器访问:`http://host.docker.internal:<映射端口>/`(若在本机直接访问则为 `http://localhost:<CC_PUBLISH_PORT>/`)。

停止并释放资源:

```bash
docker compose down          # 保留数据库卷
docker compose down -v       # 同时删除数据库卷,恢复初始演示数据
```

健康检查:

- 前端容器 `HEALTHCHECK` → `http://127.0.0.1:8080/`
- 后端容器 `HEALTHCHECK` → `http://127.0.0.1:8080/actuator/health`
- 数据库容器 `pg_isready`

## 测试账号 / 演示数据(逐角色)

所有账号密码统一:**`123456`**

| 角色 | 用户名 | 显示名 | 权限与可操作功能 |
|---|---|---|---|
| 患者 | `patient` | 张伟 | 提交处方、改地址、签收确认、上报不适/漏袋、申请补开发票、查看本人完整履约 |
| 患者 | `patient2` | 李桂兰 | 第二位患者(老人代取演示) |
| 诊所 | `clinic` | 济世堂账号 | 单张/批量处方、代患者提交、回访、补开发票申请;只能看本诊所处方 |
| 诊所 | `clinic2` | 同仁和账号 | 第二家诊所(其医生无医保资质,用于医保目录限制演示) |
| 药师 | `pharmacist` | 王药师 | 审方(校验/通过/驳回/缺药替代/医保结算)、抓药扣费、派单、回访、全部履约可见 |
| 煎药员 | `decoctor` | 赵师傅 | 排产(锅号/浸泡/次数/袋数/波次)、扫码、开始/结束煎煮、异常气味/漏袋/破损/包装复核 |
| 配送员 | `courier` | 刘小军 | 接单、出库、签收(超时自动开单);另有 `courier2` 周大勇 |
| 财务 | `finance` | 陈会计 | 开票/补开发票、赔付台账、配送看板 |
| 管理员 | `admin` | 药房管理员 | 全部数据可见、异常复盘分析(医生/药味/锅号/配送员/诊所评分) |

### 种子处方(覆盖各履约状态)

| 处方号 | 预置状态 | 演示要点 |
|---|---|---|
| RX20260920-0001 | 待审方 | 患者提交、加糖、含**金银花缺药**(库存 50g)可审方时替代为忍冬藤、砂仁后下 |
| RX20260920-0002 | 待审方 | **十八反:甘草反海藻**,审方应驳回 |
| RX20260920-0003 | 待审方 | **夜间急煎 + 老人代取 + 医保限制**(麻黄超量、西洋参自费、杏仁目录外、医生无医保资质) |
| RX20260920-0004 | 审方通过待抓药 | 自费,一键抓药 |
| RX20260920-0005 | 已抓药待煎煮 | 医保,已入煎药队列 |
| RX20260920-0006 | 代煎中 | G-02 锅、已扫码、已开始;石膏先煎、砂仁后下 |
| RX20260920-0007 | 煎煮完成待配送 | G-01 锅、10 袋;已挂一条**患者临时改地址**工单 |
| RX20260920-0008 | 配送中 | 已过承诺时间,签收/定时扫描将登记**配送超时**工单 |
| RX20260920-0009 | 已签收 | **漏袋赔付 30 元**、补开发票全流程、用药提醒运行中,可直接做回访 |
| RX20260920-0010 | 待自取 | 自取处方抓药后不进代煎 |

### 建议全流程走查(对应验收点)

1. `pharmacist` 登录 → 药师审方 → 对 0001 点"实时审方校验"→ 勾选缺药替代 → 通过并结算(医保/自付自动计算)。
2. 对 0002 校验可见十八反阻断 → 驳回;对 0003 可见剂量异常、目录外药材、医保资质预警、夜间急煎附加费。
3. 对 0004 抓药(库存扣减);进入 0005 用 `decoctor` 排产(锅号 G-04、浸泡 30 分、2 煎、袋数=剂数×2、波次)→
   扫码(码 `RXNO-<处方ID>`)→ 开始 → 结束并填包装人/复核人(漏袋填 1 会自动开包装袋破损工单)。
4. 对煎好的处方用 `pharmacist`/`courier` 派单(承诺 60 分钟)→ 出库 → 签收(老人代取填代取人;
   0008 签收自动产生超时工单与超时分钟数)。
5. `patient` 在详情页"患者临时改地址"→ 工单挂到同一履约记录,定位含波次/配送员/锅号。
6. 对 0009 做回访:选"服药后不适"→ 自动开工单、用药提醒暂停;在异常工单中心解决并登记赔付;
   "服药正常"后可手动恢复提醒。
7. 诊所端对已结算处方"申请补开发票" → `finance` 在工单中心"补开发票",工单自动闭环。
8. `admin` → 异常复盘:按医生、药味、锅号、配送员、诊所合作评分查看统计。
9. `clinic` 登录 → 提交处方 → "批量开方"一次提交多张(同一批次号)。

## 业务规则说明

- **审方引擎**(`ReviewEngine`):十八反/十九畏与超剂量、目录外药材为**阻断项**;缺药若药房有替代表药材
  (金银花→忍冬藤、砂仁→白豆蔻、郁金→香附)可由药师确认替代后通过,否则阻断。
- **费用**:药费 = 目录克单价 × 单剂剂量 × 剂数;代煎 3 元/剂;配送 6 元;夜间急煎 +20 元;自取不收代煎/配送费。
  医保目录内药费按 60% 报销,目录外药材全额自付,自付 = 总额 − 医保支付。
- **锅号/包装人/配送员定位**:异常工单自动拼接煎药记录(锅号、包装人、复核人)与配送记录(波次、配送员),
  患者反馈不适或漏袋时可一键定位。
- **配送超时**:超过承诺送达时间未签收,定时任务每 60 秒扫描自动登记超时工单(幂等),签收时也会回算超时分钟。
- **回访联动**:不适 → 开 DISCOMFORT 工单 + 暂停用药提醒;赔付进入财务赔付台账;异常率/不适数/满意度
  汇入医生与诊所合作评分。

## 目录结构

```
.
├── backend/                # Spring Boot 工程
│   ├── Dockerfile          # 多阶段:maven 构建 → temurin jre(非 root tcm 用户 + HEALTHCHECK)
│   ├── pom.xml
│   └── src/main/java/com/county/tcm/
│       ├── domain/         # JPA 实体与枚举(处方、药味、审方、抓药、煎药、配送、回访、工单、发票、提醒、事件)
│       ├── repo/           # Spring Data JPA Repository
│       ├── security/       # JWT、SecurityConfig、当前登录用户
│       ├── service/        # ReviewEngine 审方引擎 + 各环节业务服务 + Analytics 复盘
│       ├── web/            # REST 控制器、全局异常、DTO
│       └── bootstrap/      # DataSeeder 演示种子数据
├── frontend/               # Vue 3 + Vite 工程
│   ├── Dockerfile          # 多阶段:node 构建 → nginx(非 root 101 + HEALTHCHECK)
│   ├── nginx.conf          # SPA history 与 /api 反代到 app:8080
│   └── src/views/          # 登录、工作台、处方列表/提交/详情、审方、煎药、配送、工单、提醒、财务、复盘
├── docker-compose.yml      # db(内网)+ app + web(仅 web 发布宿主端口)
└── .env.example
```

## 常用接口(均需 `Authorization: Bearer <token>`,登录接口除外)

- `POST /api/auth/login` 登录
- `GET  /api/meta` 医生/药材目录/配送员/枚举
- `POST /api/prescriptions`、`POST /api/prescriptions/batch` 提交/批量提交
- `GET  /api/prescriptions/{id}/review-preview` 实时审方校验
- `POST /api/prescriptions/{id}/review|dispense` 审方 / 抓药
- `POST /api/prescriptions/{id}/decoct/{schedule|scan|start|end}` 代煎四步
- `POST /api/prescriptions/{id}/delivery/{assign|outbound|sign}` 配送三步
- `POST /api/prescriptions/{id}/address-change|follow-up` 改地址 / 回访
- `POST /api/prescriptions/{id}/issues`、`POST /api/issues/{id}/processing|resolve` 异常工单
- `POST /api/prescriptions/{id}/invoice`、`/invoice/reissue-request`、`POST /api/issues/{id}/reissue`
- `GET  /api/analytics/{overview|by-doctor|by-herb|by-pot|by-courier|clinics}` 复盘分析
- `GET  /api/prescriptions/{id}` 返回**完整履约聚合**(处方/审方/煎药/配送/回访/工单/发票/提醒/事件时间线)
