# 育己 · 女性全周期健康平台

> 技术栈：Vue3 H5 + Element Plus（机构端）｜Java 21 + Spring Boot 3 + MyBatis-Plus + Flyway｜Python FastAPI（AI 微服务）
>
> 一期：孕产档案 / 产检 / 指标 / 报告 / 家属协同 / 知识库 / 机构看板
> 二期：经期与周期预测 / 月事餐饮 / 私密健康咨询 / 她社区 / 42 天月子 / 按月龄育儿 / EPDS 心理康复 / 合作机构与预约线索
> 二期迭代 2：**机构租户化（SaaS）**——机构作为租户用平台管理自己的用户（会员列表 / 线索 / 风险摘要，按 org_id 隔离）；**AI 社区风控**——发帖 / 评论 / 情绪打卡自动过风控引擎，危机表达屏蔽进待审核并生成红档告警，消极情绪放行内容但对人提交黄档预警（`ai-service /api/v1/moderate/scan`，AI 失联时本地词典兜底）

## 目录

```
yuji/
├── frontend-h5/     用户端（妈妈/家属，Vue3 + Vant）
├── frontend-admin/  机构端（Vue3 + Element Plus + ECharts）
├── backend/         Spring Boot 3 多模块（common / auth / profile / family / content / wellness / admin / server）
├── ai-service/      AI 微服务（规则版情绪分级 + 私密咨询共情文案；医学结论不交给模型）
└── deploy/          docker-compose + nginx
```

## 快速启动（本地开发）

**0. 前置**：JDK 21、Maven 3.9+、Node 18+、Docker（用于 MySQL）

**1. 起基础设施**（MySQL 8 / Redis / MinIO）：

```bash
cd deploy
docker compose up -d
```

**2. 起后端**（自动执行 Flyway 建表 + 种子数据）：

```bash
cd backend
mvn spring-boot:run -pl yuji-server
# 或打包：mvn -DskipTests package && java -jar yuji-server/target/*.jar
```

**3. 起用户端 H5**：

```bash
cd frontend-h5
npm install
npm run dev     # http://localhost:5173
```

**4. 起机构端**：

```bash
cd frontend-admin
npm install
npm run dev     # http://localhost:5174
```

机构账号由 `V9__seed_org_account.sql` 预置：`org001 / org123456`（**上线前必须改密码**）。
非机构账号登录会被前端拦下（后端也会返 40300），妈妈请走 H5 端。

**5. 起 AI 微服务**（可选，W7 接入）：

```bash
cd ai-service
pip install -r requirements.txt
uvicorn main:app --port 8000
```

## 一键容器化演示

```bash
cd deploy
docker compose --profile app up -d --build
# 前端构建后由 nginx 托管：docker build -t yuji-h5 ../frontend-h5（按需）
```

## 测试

```bash
cd backend
mvn test          # JDK 21 直接跑
# 本机只有 JDK 17 时：mvn -Dmaven.compiler.release=17 test
```

209 个用例（common 11 / auth 25 / profile 96 / family 42 / content 13 / admin 7 / **wellness 15**）。一期范围（不含 wellness）行覆盖 93.7%（923/985）、分支覆盖 83.3%（338/406），报告在 `backend/*/target/site/jacoco/index.html`；wellness 二期覆盖周期预测、EPDS 分级与食谱工具函数，其余依赖数据库的链路由冒烟脚本验证。机构看板的统计 SQL（`JdbcDashboardStatsQuery`）与内容种子数据不在单测范围，由冒烟脚本对真实库验证。

| 模块 | 测试类 | 用例 | 覆盖什么 |
|---|---|---|---|
| common | `ResultTest` | 8 | 返回体契约、错误码分段不重复、错误码稳定性 |
| common | `GlobalExceptionHandlerTest` | 3 | 业务异常映射、校验消息拼接、**500 不泄露堆栈/连接串** |
| auth | `JwtServiceTest` | 5 | 签发/解析往返、跨实例无状态、换密钥失败、过期失败、脏数据 |
| auth | `JwtAuthFilterTest` | 4 | 无头放行、非 Bearer 忽略、合法注入、**脏 token 清空上下文（防身份继承）** |
| auth | `AuthServiceTest` | 7 | 密码哈希入库、重复手机号 40900、错密码/禁用账号、不泄露账号是否存在 |
| auth | `AuthControllerTest` | 5 | **接口层 @Valid 真的生效**、错误码映射、token 返回结构 |
| auth | `SecurityConfigTest` | 4 | 匿名访问受保护接口 401/40100、伪造 token 拒绝、`/api/auth/**` 放行 |
| profile | `GestationalCalculatorTest` | 25 | 13+6/14+0、27+6/28+0、280/281 天产后边界、未来日期、孕周自洽不变量 |
| profile | `ProfileServiceTest` | 7 | lmp/edd 二选一换算、建档自动生成计划、重复建档、40401 契约 |
| profile | `CheckupServiceTest` | 7 | 计划日 = lmp+孕周×7、排序与逾期标记、**越权改他人产检项被拒**、非法状态 |
| profile | `ProfileControllerTest` | 5 | JSON 契约、昵称超长拦截、40401 透传 |
| profile | `CheckupControllerTest` | 5 | 日历字段、状态更新、`{}` 空 body 返回 40000（回归用例） |
| profile | `HealthRecordServiceTest` | 14 | 四类指标范围校验、**收缩压必须高于舒张压**、未来时间拦截、趋势同日取最后一次 + 摘要、历史脏类型不 500 |
| profile | `HealthRecordControllerTest` | 6 | 录入 / 趋势 JSON 契约、`@Valid` 缺值拦截、参数透传 |
| profile | `ReportServiceTest` | 10 | 类型白名单（扩展名由 contentType 推导）、10MB 上限、**写库失败回删文件（不留孤儿）**、越权、文件名清洗 |
| profile | `ReportControllerTest` | 5 | multipart 上传、下载 `Content-Disposition`（中文名 RFC 5987）、删除归属 |
| profile | `LocalFileStorageTest` | 4 | 真实读写临时目录、**目录穿越 `../` 拦截**、删除幂等 |
| profile | `MinioFileStorageTest` | 6 | 桶不存在自动创建、SDK 异常映射为业务码（client mock，无外部依赖） |
| profile | `StorageConfigTest` | 2 | 一行配置切换本地磁盘 / MinIO |
| family | `FamilyServiceTest` | 24 | **邀请码一次性/过期/并发抢占**、不能加入自己家庭、一个用户只属一个家庭、非妈妈改授权 40300、非指标字段拒绝、授权归一去重、移除越权 40400 |
| family | `TaskServiceTest` | 11 | 按模板生成任务与截止日、执行人必须在本家庭、**家属只能改派给自己的任务**、跨家庭任务 40400、状态白名单、列表按角色收窄 |
| family | `SharedServiceTest` | 7 | 摘要默认可见且不含情绪字段、妈妈未建档不 500、**未授权指标 40300**、读取的是 ownerId 而非当前用户、按授权字段合并排序 |
| content | `ContentServiceTest` | 9 | **按孕周区间命中**（不是按发布时间）、离当前孕周最近的排第一、limit 归一、孕周越界 40000、未知分类 40000、详情隐藏下线/已删内容（统一 40400） |
| content | `ContentControllerTest` | 4 | 本周知识默认 limit=3 且**列表不带正文**、筛选参数透传、详情含 source/disclaimer、40400 透传 |
| admin | `AdminDashboardServiceTest` | 5 | 妈妈账号读看板 40300、空库不除零、完成率四舍五入、孕周分桶（含未建档/未来日期）、**近 7 天缺数据补 0** |
| admin | `AdminControllerTest` | 2 | 看板 JSON 契约、非机构账号 40300 |
| wellness | `CycleServiceTest` | 8 | 四阶段分段、短周期无空档、平均周期过滤异常值、记录过期不硬猜、三次预测、经量白名单 |
| wellness | `EpdsScorerTest` | 5 | 绿/黄/红界值、**第 10 题阳性直接红档**、答案数量与取值校验 |
| wellness | `MealServiceTest` | 2 | 任意日期归一到周一、食材拆分去空 |

设计取舍：

- **Controller 用 standalone MockMvc**，不启动 Spring 容器，单个用例毫秒级；`MvcTestSupport` 统一配好 JavaTime、`@AuthenticationPrincipal`、`ResourceHttpMessageConverter`（报告下载用）与全局异常处理
- **只有安全链用 `@WebMvcTest` 切片**，因为 401 拦截与放行规则必须在真实 `SecurityFilterChain` 下才可信
- **对象存储用 mock 验证契约**（而非真起 MinIO），保证 `mvn test` 在任意机器裸跑；冒烟脚本默认走本地磁盘，若要验证真实 MinIO，部署时设 `STORAGE_TYPE=minio`
- 需要数据库的链路不写单测，交给 `smoke.ps1` 冒烟脚本（集成级）

## 当前进度（对应 docs/08 排期）

- [x] W1 脚手架 + 登录注册（JWT）+ 建档（3 步引导）
- [x] W2 孕周计算 + 首页 + 产检日历（模板种子数据已入库）
- [x] W3 指标记录（体重/血压/血糖/胎动）+ 趋势图（ECharts）+ 报告上传（本地磁盘默认，MinIO 一行配置切换）
- [x] W4 家属协同：邀请码绑定、4 套任务包下发与打卡、字段级授权（情绪/量表不可授权）
- [x] W5 知识内容按孕周推送（14 篇种子 + 区间命中 + 来源标注）、机构端登录 + 真实数据看板、家属端指标趋势图（与妈妈端共用一套图表配置）—— **模块 C 完结**
- [x] W6 她社区（五类分区 / 匿名 / 危机词审核）、合作机构与预约线索
- [x] W7–W8 情绪打卡 + EPDS + 三级分级 + 风险工单 + 机构端跟进
- [x] 二期扩展：全周期档案、周期记录与预测、月事餐饮、私密健康咨询、42 天月子、按月龄育儿、机构端四个工作台

## 演示流程

1. 打开 http://localhost:5173 → 注册（手机号 11 位 + 密码 ≥ 6 位）
2. 建档三步：昵称 → 末次月经或预产期 → 关注点
3. 首页显示孕周卡片 + 今日 3 件事；产检日历显示全孕期计划
4. 左滑产检项可标记 完成 / 跳过；点「查看 / 上传报告」进报告页
5. 「记录」页：切体重/血压/血糖/胎动 → 记一笔 → 看趋势曲线与参考区间
6. 报告页：拍照或选 PDF 上传（≤10MB），点击预览、左滑删除
7. 「家人」页：妈妈生成 6 位邀请码 → 家属账号输入加入 → 妈妈勾选授权指标 → 派任务包 → 家属打卡，妈妈实时看到进度
8. 首页「本周知识」→ 只看得到和当前孕周匹配的 2 篇（孕 28 周会看到糖耐、数胎动，不会看到叶酸）；点进详情，正文底部有来源与审核人
9. 机构端 http://localhost:5174：`org001 / org123456` → 全周期运营 + 孕周分布、近 7 天记录、任务与产检完成率（全部为聚合数字）
10. 新用户引导选「经期管理」→ 填最近一次月经 → 首页显示周期第 X 天、下次月经与阶段饮食提示（不强制填孕周）
11. 「健康 - 月事餐饮」：按当前阶段生成一周食谱，切日期看四餐与营养重点，底部自动汇总购物清单
12. 「健康 - 私密健康咨询」：选主题 → 描述情况 → 获得"可以先做 / 必须就医信号 / 建议科室 / 来源"；输入大出血、晕厥等词会升红档，可一键转人工并推荐合作机构
13. 「她社区」：五类分区、匿名发帖、点赞评论；命中危机词/导流词的帖子进入机构端待审核
14. 「关怀」：月子调理填分娩日期生成 42 天清单并打卡；育儿教育按月龄匹配课程；心理康复做情绪打卡与 EPDS，黄/红档生成工单
15. 「合作机构」：按类型筛选 → 查看服务包 → 提交预约意向；机构端「预约线索」跟进状态

## 已实现接口（W1–W3）

| 域 | 端点 | 说明 |
|---|---|---|
| auth | `POST /api/auth/register` `POST /api/auth/login` | 注册 / 登录（返回 token + 用户） |
| profile | `POST /api/profiles` `GET /api/profiles/mine` | 建档 / 查询（未建档返回 40401） |
| checkup | `GET /api/checkups` `PUT /api/checkups/{id}` | 日历、状态标记 |
| health | `POST/GET /api/health-records` `GET /api/health-records/trend` `DELETE /api/health-records/{id}` | 指标录入 / 列表 / 趋势 / 删除 |
| report | `POST/GET /api/checkups/{id}/reports` `GET /api/reports` `GET /api/reports/{id}/download` `DELETE /api/reports/{id}` | 上传 / 列表 / 下载 / 删除 |
| family | `POST /api/family/invitations` `POST /api/family/join/{code}` `GET /api/family/mine` `GET /api/family/members` `PUT /api/family/members/{id}/permissions` `DELETE /api/family/members/{id}` `GET /api/family/share-fields` | 邀请码 / 加入 / 归属 / 成员 / 字段级授权 |
| family | `GET /api/family/shared/summary` `GET /api/family/shared/records` `GET /api/family/shared/trend` | 家属视角（按授权过滤） |
| task | `GET /api/tasks` `POST /api/tasks/assign` `PUT /api/tasks/{id}/status` `GET /api/task-templates` | 任务包下发 / 打卡 / 模板 |
| admin | `GET /api/admin/dashboard` | 机构看板（仅 ORG/ADMIN，纯聚合） |
| content | `GET /api/articles` `GET /api/articles/for-me` `GET /api/articles/{id}` | 知识库 / 按孕周推荐 / 详情（带来源与免责） |
| wellness | `POST /api/wellness/profiles` `GET /api/wellness/overview` | 全周期档案 / 首页聚合（阶段、周期、孕周、月子进度） |
| cycle | `GET/POST /api/wellness/cycles` `DELETE /api/wellness/cycles/{id}` `GET /api/wellness/cycles/summary` | 周期记录 / 预测 / 阶段 |
| meal | `GET /api/wellness/meal-plan?week=` | 四阶段一周食谱 + 购物清单 |
| consult | `GET /api/wellness/consult/topics` `GET/POST /api/wellness/consult/sessions` `POST .../messages` `POST .../referral` | 知识卡 / 会话 / 风险升级 / 转介工单 |
| community | `GET/POST /api/wellness/community/posts` `GET /api/wellness/community/posts/{id}` `POST .../like` `GET/POST .../comments` | 分区帖子 / 匿名 / 点赞评论 |
| care | `GET /api/wellness/care/plan` `POST /api/wellness/care/plans` `PUT /api/wellness/care/tasks/{id}` | 42 天月子计划 / 任务打卡 |
| parenting | `GET /api/wellness/parenting/courses` `GET .../courses/{id}` | 按月龄课程与课时 |
| mental | `GET /api/wellness/mental/overview` `POST .../checkins` `POST .../scales/epds` `GET .../tickets` | 情绪打卡 / EPDS / 我的工单 |
| partner | `GET /api/wellness/partners` `GET /api/wellness/partners/{id}` `POST .../leads` | 合作机构 / 服务包 / 预约意向 |
| admin | `GET /api/admin/wellness/overview` `GET/PUT .../tickets` `GET/PUT .../posts` `GET/POST/PUT/DELETE .../partners` `GET/PUT .../leads` | 机构端四个工作台（仅 ORG/ADMIN，无健康明细） |

存储配置（不配也能跑）：

```yaml
yuji:
  storage:
    type: local            # local（默认，磁盘）| minio
    local-dir: ./data/uploads
    minio: { endpoint: http://localhost:9000, bucket: yuji-reports }
```

## 设计要点（答辩可讲）

- **错误码分段**：40000 参数 / 40100 鉴权 / 40401 未建档（前端据此跳建档引导）
- **孕周计算器是纯函数**（`GestationalCalculator`），可直接单元测试
- **产检模板来源标注**：`V4__seed_checkup_template.sql` 注明《孕前和孕期保健指南（2018）》，页面也展示免责说明——内容可溯源
- **Flyway 管理数据库版本**：评委现场 `docker compose up` 即可复现完整环境
- **指标语义集中在 `MetricType` 枚举**：单位、合法范围、参考区间一处维护，服务层做硬校验、前端只负责文案；新增指标不改表结构
- **存储抽象 `FileStorage`**：本地磁盘（默认，零依赖）与 MinIO（S3 兼容）同一接口，`yuji.storage.type` 一行切换；报告只存元数据 + `objectKey`，下载必须带 token
- **报告上传的四道闸**：类型白名单 → 10MB 上限 → 扩展名由 contentType 推导 → `objectKey` 服务端生成且校验归属/目录穿越
- **趋势图不判读**：接口返回 `disclaimer`，曲线只做可视化（参考区间仅作底色），规避越界做医学结论的合规风险
- **家庭角色不写进 `user.role`**：JWT 里是签发时快照，加入家庭后改库会让旧 token 与新角色不一致；家庭权限每次实时查 `family_member`，服务端判定，前端只负责展示
- **共享默认最小**：家属默认只见任务与状态摘要；指标要逐项勾选授权，情绪记录与量表得分在枚举层面就不存在授权入口（`privacyNote` 把这条边界明写给家属看）
- **机构端只给聚合，不给明细**：`/api/admin/dashboard` 只返回人数、条数、完成率与孕周分桶，接口里没有任何 userId / 指标值 / 情绪内容；统计 SQL 集中在 `JdbcDashboardStatsQuery`，口径一眼可查（全部带 `deleted_at IS NULL`）
- **趋势图配置只维护一份**：`useTrendChart` 同时服务妈妈端「记录」页与家属端共享页，血压双线、参考区间底色、免责声明三处行为天然一致，不会两边各改一套
- **知识按孕周区间命中，不按发布时间**：`article.min_week / max_week` 决定谁在什么时候看到什么——12 周的人看不到待产包清单；排序用"区间中点离当前孕周的距离"，同一段孕周里先给此刻最相关的那篇
- **内容必须可溯源**：每篇 `source` + `reviewer` 非空，接口在详情页返回 `disclaimer`（不替代医生诊断），审核人字段为后续内容合规留痕
- **AI 服务独立部署、规则可解释**：每个分级结果返回 `contributions`（特征贡献），为二期 SHAP 可解释性预留同一返回结构
- **双档案并存，不强迫所有人填孕产信息**：`profile`（孕周）与 `wellness_profile`（经期/产后/更年期）按 user_id 并存；首页以"至少一份档案"为可用条件，经期用户不会被建档页挡在门外
- **私密咨询不做诊断**：结构化回答来自 9 张审核知识卡（可以先做 / 必须就医信号 / 科室 / 来源），AI 只做共情开场；大出血、晕厥等风险词直接升红档并生成转介工单
- **周期预测与 EPDS 都是纯函数**：`CycleService.phaseOf/buildSummary`、`EpdsScorer.score` 可单测、可答辩解释口径；第 10 题阳性直接红档，避免总分掩盖自伤风险
- **社区匿名是服务端保证**：VO 不返回 user_id，匿名帖只渲染"匿名姐妹"；危机词/导流词自动 `PENDING` 转人工，不直接删求助帖
- **机构端边界写进接口**：`/api/admin/wellness/*` 只返回聚合数、风险摘要与用户主动提交的预约；周期、情绪、聊天全文不进入机构端
