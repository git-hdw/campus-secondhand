# 项目 Agent 协作约定

## 核心原则

清理冗余 UI 元素，布局不清就重构；每次迭代同时兼顾用户、开发者、AI Agent 三者体验，不破坏现有功能。

- 用户体验：突出主要任务，减少重复标题、说明、装饰与入口；统一视觉层级、间距、控件和反馈。保留必要的状态、错误提示、权限说明、键盘操作与无障碍标签。
- 开发体验：优先复用已有组件、主题变量和接口。布局职责不清时整理结构，而不是继续叠加覆盖；清理失效 DOM、未使用样式和重复定义。避免为简单页面引入不必要的抽象或依赖。
- Agent 体验：保持明确的文件职责、语义化结构、可访问名称和稳定的业务入口；使用可搜索的命名，说明非显而易见的兼容约束，维护测试命令与验收记录，使后续修改容易定位、验证和接手。
- 功能兼容：清理前检查引用、交互和动态状态。不得以精简 UI 为由删除有效功能、校验、权限、加载/失败/空状态；保留工作区已有修改，不擅自回退或改写业务数据。

## 工作入口与边界

- 交接文档：工作区 `docs/00-项目交接.md`。
- 前端：`frontend/campus-secondhand`；后端：`afterend/campussecondhand`。
- 导航和正文偏移由 `src/App.vue` 与 `SiteHeader.vue` 管理，页面不要自行添加侧栏左边距。
- 共享正文宽度、标题和页脚由 `workspace.css`、`PageHeading.vue`、`SiteFooter.vue` 管理；商品卡片由 `catalog.css` 管理。
- 首页样式入口为 `src/styles/home-market.css`；发布/编辑及其传送层样式入口为 `src/styles/post-product.css`，由对应页面局部导入。
- 详情页样式保留在 `ProductDetail.vue`，使用页面专有类名，避免通用商品标题样式误覆盖。
- 登录、注册共用 `src/styles/auth.css` 管理卡片宽度、控件尺寸和间距；表单高度随字段数量变化，不再分别覆盖认证页面布局。
- 新增页面样式先确定归属，避免在页面、`layout.css`、`workspace.css` 间重复定义同一规则。修改共享样式需要回归其使用页面。
- UI 改动保留真实统计和数据，不虚构商品、销量或学校认证；保留待见面限制、自购限制及现有订单规则。
- 下拉菜单使用现有 Element Plus；历史地点原值必须在编辑和恢复草稿时保留，不开放任意新建地点。

## 验证与交付

在前端目录执行以下命令；不要使用带自动修复的全仓库 lint 覆盖无关修改：

```powershell
$node = 'C:\Users\Administrator\.kitool\node\current\node.exe'
& $node node_modules/vitest/vitest.mjs run
& $node node_modules/eslint/bin/eslint.js <本次改动的 Vue/JS 文件>
& $node node_modules/vite/bin/vite.js build
```

测试临时目录受限时，将 TEMP/TMP 指向仓库 `.review/vitest-tmp`。测试应覆盖实际行为和风险，不为纯样式删除编写实现细节快照。

完成布局变更后核对桌面实际页面，记录测试结果、截图及未覆盖范围，更新工作区 `docs/` 下的相关验收文档。按当前用户要求，不进行 Edge 实测和窄屏专项检查；不能因此宣称已通过所有浏览器认证。

## 后端一致性与安全边界

- 当前包名为 `com.campus.secondhand`。认证由 `ApiGateFilter` 统一解析 Token，业务通过 `CurrentUser` 获取身份；不要在 Controller 重复按用户名解析 JWT。
- 用户 ID 是身份主键；修改密码必须使旧会话失效。更新资料只写白名单字段，不使用包含旧角色、旧状态的整实体覆盖数据库。
- 交易状态统一交给 `OrderTradeService`，相关写入遵守商品行锁和事务约定；修改确认、取消、删除逻辑时验证失败回滚及并发竞争。
- 发布和消息的自动/手动重试沿用原 `Idempotency-Key`，变更提交内容后使用新标识；业务写入与去重响应在同一数据库事务中提交。
- 数据库口令与 JWT 密钥只通过环境变量或忽略的 `.local/runtime.properties` 提供；不可写入源码、测试输出、报告或提交内容。启动后端时以其目录为工作目录，使本地配置正确加载。
- 普通后端单元测试不使用业务库。真实 MySQL 测试仅在显式设置 `-Dreview.integration=true` 时运行，在 `campus_review_<随机值>` 独立库验证；任何清理先验证目标，禁止删除业务库或用户数据。
- 维护入口 `tools/restart-reviewed-backend.ps1` 先执行 24 项单元测试并构建成功，再重启本项目服务。并发与隔离库的完整命令见 `docs/review-2026-10-28/后端查漏补缺与验证.md`。
- 新增或变更数据库结构时同步 `schema.sql` 和增量迁移说明；`CREATE TABLE IF NOT EXISTS` 不是旧表迁移工具。报告必须区分历史设计、当前实现、已验证结果和未覆盖范围。

- 2026-10-29 当前基线：Controller 只进行 HTTP 适配，禁止直接访问持久层；通过 LayeringTest 验证。数据库迁移见 SchemaMigration，九张表采用 InnoDB，历史失联订单必须保留 legacy_product_id。个人订单、收藏和消息读取 data.items，不能恢复全量循环拉取。最新验收见 docs/review-2026-10-29/五项问题整改验收.md。

- 后续优化基线：我的发布同样读取 data.items，统计取 data.stats；订单商品信息优先使用下单快照，不能用当前商品内容回填旧订单。SessionMaintenance 仅清理到期/撤销超过 30 天的会话，不清理请求去重记录。完整验证当前为后端 41 项、前端 81 项，详见 docs/review-2026-10-29/分页、订单快照与运维优化验收.md。
