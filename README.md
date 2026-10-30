# 校园二手交易平台

基于 Spring Boot 与 Vue 3 的毕业设计项目，面向校园闲置物品的线下当面交易。

支持注册登录、商品发布与审核、搜索筛选、收藏、站内咨询、下单约见、取消订单、买家确认收货，以及管理后台。消息使用轮询，草稿按账号保存在当前浏览器。平台不收款、不发货；学号由用户登记，确认收货记录买家声明。

## 技术栈

- 后端：Java 17、Spring Boot 3.1.2、Spring Security、JWT、MyBatis-Plus 3.5.3.1。
- 前端：Vue 3、Vue Router、Pinia、Vite 7、Element Plus、Vitest。
- 数据库：MySQL 8，数据库名 `campus_secondhand`；初始化与增量迁移由应用执行。

## 目录

```text
afterend/campussecondhand/       Spring Boot 后端
frontend/campus-secondhand/      Vue 前端
docs/                           当前需求、数据库、接口及交接说明
tools/                          数据核对、备份与本机验收辅助工具
AGENTS.md                       项目协作约定
```

## 本地启动

准备 JDK 17、Maven 3.9、MySQL 8，以及 Node.js `^20.19.0` 或 `>=22.12.0`。以下为 PowerShell 示例，其他终端请使用对应的环境变量语法。

1. 启动 MySQL。默认连接 `localhost:3306/campus_secondhand`，用户名 `root`。可以通过 `SPRING_DATASOURCE_URL` 和 `SPRING_DATASOURCE_USERNAME` 覆盖。
2. 在后端目录配置本机凭据并启动：

```powershell
cd afterend/campussecondhand
$env:DB_PASSWORD = '<你的数据库密码>'
# 每个部署使用自己的随机密钥，至少 64 个 ASCII 字符。不要提交密钥。
$env:JWT_SECRET = '<自行生成的随机密钥，至少64字符>'
mvn spring-boot:run
```

也可以在后端目录创建被 Git 忽略的 `.local/runtime.properties`，填写 `DB_PASSWORD` 和 `JWT_SECRET`。数据库账号需要创建数据库/表及执行结构迁移的权限；已有数据请先备份。就绪检查：`http://localhost:8080/api/stats`。

3. 在另一个终端启动前端：

```powershell
cd frontend/campus-secondhand
npm ci
npm run dev -- --port 5173 --strictPort
```

打开 `http://localhost:5173`。Vite 将 `/api` 请求代理到后端 8080 端口。

公开版本仅包含空表结构和分类初始值，不包含业务数据或预设管理员口令。先通过注册页面创建自己的账号，再由数据库维护者将该账号设置为管理员：

```sql
UPDATE users SET role = 1 WHERE username = '<你刚注册的用户名>';
```

重新登录后访问 `/admin`。新发布商品须经管理员审核才会公开。

## 验证

前端目录：

```powershell
npx vitest run
npm run build
```

后端目录的普通单元测试不依赖业务库：

```powershell
mvn -Dtest=AuthControllerTest,LayeringTest,MoneyAmountTest,OrderTradeServiceTest,JwtUtilTest,RequestValueTest,FavoriteServiceTest,ImageValueTest package
```

真实 MySQL 集成测试需显式设置 `-Dreview.integration=true` 并选择 `TradeDatabaseTest`，仅使用随机命名的 `campus_review_*` 隔离库。不要将业务库用于测试。

交接记录中的最近验收基线为后端 41 项、前端 81 项；它们是历史验收结果。当前公开快照未在全新环境完成端到端复现。

## 文档与边界

- [项目交接](docs/00-项目交接.md)
- [需求规格](docs/02-需求规格说明书.md)
- [数据库设计](docs/04-数据库设计.md)
- [REST 接口](docs/05-REST接口设计.md)
- [协作约定](AGENTS.md)

交接中的绝对路径、历史验收证据和辅助脚本的运行路径属于原开发环境。公开仓库未包含私密配置、数据库备份、依赖缓存和历史 Git 提交。管理端订单快照、交付复现及后续优化的状态见交接文档。
