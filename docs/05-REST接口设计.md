# REST 接口设计

版本：2026-10-29 当前实现。Base URL：`http://localhost:8080/api`。下表由现有 Controller 映射生成，旧规划路径不再作为当前契约。

## 1. 响应与认证

统一响应为 `{code,message,data}`；成功 code=200；常见错误为 400、401、403、404、409、500，HTTP 状态同步。历史注册冲突保留 code=70、HTTP 409。需要登录的请求携带 Authorization: Bearer JWT。

公开：POST 登录、注册、密码说明；GET 首页统计、分类、在售商品列表/搜索/详情、用户公开资料。管理路径需要数据库当前管理员角色。商品归属、订单参与者、消息收件人和会话所有者继续由 Service 检查。

JWT 版本 3，主体为用户 ID，包含 jti 会话标识、凭据标记、签发与到期时间；校验签名、当前账号、凭据、服务端会话是否有效。POST /auth/logout 撤销当前会话；POST /auth/logout-all 撤销本人所有会话；DELETE /auth/sessions/{id} 只能撤销本人会话。

## 2. 查询与写入契约

我的发布、个人订单、收藏、收发消息及会话摘要使用分页 envelope：items、total、page、pageSize，默认 20，最大 50。订单支持 status、productId 筛选；下单恢复必须使用商品筛选，不能只查首页后认为没有已有订单。

GET /messages/conversations 支持 page、pageSize、search、unread，额外返回全局 unreadTotal。search 匹配联系人名称、历史消息内容或关联商品标题。GET /messages/conversation/{otherUserId} 每次最多 50 条，before/after 为互斥的正整数消息 ID 游标，返回 items、hasMore、readIds；默认最新一批，before 获取更早记录，after 获取后续新增记录。

商品列表使用 page/pageSize/category/keyword/condition/minPrice/maxPrice/sort；data 为 items/total/page/pageSize。管理端分页仍沿用各管理页面契约。GET /products/my 使用 page/pageSize，额外返回 stats：listings（全量在售数）、sold（全量已售数）、views（全量浏览次数），不以当前页代替账号统计。前端已同步分页响应。

新订单详情包含 snapshotJson（JSON 字符串）；个人订单列表的 productTitle/productImage 优先取快照。结果页直接展示快照，避免再次读取已变更的商品；旧订单没有快照时兼容原商品查询。买卖双方与成交金额仍以订单字段为准。

POST /products/create 和 /messages/send 支持 Idempotency-Key（16–80 位字母、数字、下划线、连字符）。相同用户、范围、标识、内容返回原结果；内容不同返回 409。请求重试复用标识，新提交生成新标识。

注册字段：username、email、studentNo、password、confirmPassword。登录字段：username、password。下单只提交 productId，身份与金额由服务端决定。商品字段及约束见需求 FR03；资料更新仅接受白名单字段，改密码需 oldPassword/newPassword。

## 3. 路由清单

| 方法 | 路径 | Controller |
| --- | --- | --- |
| GET | `/api/admin/users` | AdminController |
| GET | `/api/admin/users/{id}` | AdminController |
| PUT | `/api/admin/users/{id}/status` | AdminController |
| GET | `/api/admin/products` | AdminController |
| GET | `/api/admin/products/{id}` | AdminController |
| PUT | `/api/admin/products/{id}/status` | AdminController |
| PUT | `/api/admin/products/batch/status` | AdminController |
| GET | `/api/admin/orders` | AdminController |
| GET | `/api/admin/orders/{id}` | AdminController |
| PUT | `/api/admin/orders/{id}/status` | AdminController |
| DELETE | `/api/admin/orders/{id}` | AdminController |
| GET | `/api/admin/messages` | AdminController |
| DELETE | `/api/admin/messages/{id}` | AdminController |
| GET | `/api/admin/categories` | AdminController |
| POST | `/api/admin/categories` | AdminController |
| PUT | `/api/admin/categories/{id}` | AdminController |
| DELETE | `/api/admin/categories/{id}` | AdminController |
| GET | `/api/admin/stats` | AdminController |
| POST | `/api/auth/login` | AuthController |
| POST | `/api/auth/register` | AuthController |
| POST | `/api/auth/forgot-password` | AuthController |
| GET | `/api/categories` | CategoryController |
| GET | `/api/categories/{id}` | CategoryController |
| POST | `/api/categories` | CategoryController |
| PUT | `/api/categories/{id}` | CategoryController |
| DELETE | `/api/categories/{id}` | CategoryController |
| GET | `/api/categories/parent/{parentId}` | CategoryController |
| GET | `/api/favorites/my` | FavoriteController |
| GET | `/api/favorites/{id}` | FavoriteController |
| POST | `/api/favorites` | FavoriteController |
| DELETE | `/api/favorites/{id}` | FavoriteController |
| GET | `/api/favorites/product/{productId}` | FavoriteController |
| GET | `/api/messages/received` | MessageController |
| GET | `/api/messages/sent` | MessageController |
| GET | `/api/messages/conversations` | MessageController |
| GET | `/api/messages/conversation/{otherUserId}` | MessageController |
| PUT | `/api/messages/conversation/{otherUserId}/read` | MessageController |
| GET | `/api/messages/unread-count` | MessageController |
| POST | `/api/messages/send` | MessageController |
| PUT | `/api/messages/read/{id}` | MessageController |
| PUT | `/api/messages/{id}/read` | MessageController |
| PUT | `/api/messages/read-all` | MessageController |
| DELETE | `/api/messages/delete/{id}` | MessageController |
| GET | `/api/orders/my` | OrderController |
| GET | `/api/orders/my/buyer` | OrderController |
| GET | `/api/orders/my/seller` | OrderController |
| GET | `/api/orders/{id}` | OrderController |
| POST | `/api/orders` | OrderController |
| PUT | `/api/orders/{id}/confirm` | OrderController |
| PUT | `/api/orders/{id}/cancel` | OrderController |
| PUT | `/api/orders/{id}` | OrderController |
| DELETE | `/api/orders/{id}` | OrderController |
| GET | `/api/products/list` | ProductController |
| GET | `/api/products/my` | ProductController |
| GET | `/api/products/category/{category}` | ProductController |
| GET | `/api/products/search` | ProductController |
| GET | `/api/products/{id}` | ProductController |
| POST | `/api/products/create` | ProductController |
| PUT | `/api/products/off-shelf/{id}` | ProductController |
| PUT | `/api/products/update/{id}` | ProductController |
| DELETE | `/api/products/delete/{id}` | ProductController |
| PUT | `/api/products/sold/{id}` | ProductController |
| GET | `/api/auth/sessions` | SessionController |
| DELETE | `/api/auth/sessions/{id}` | SessionController |
| POST | `/api/auth/logout` | SessionController |
| POST | `/api/auth/logout-all` | SessionController |
| GET | `/api/stats` | StatsController |
| GET | `/api/user/profile` | UserController |
| PUT | `/api/user/profile` | UserController |
| GET | `/api/user/user/{id}` | UserController |

## 4. 兼容说明

本轮个人列表由数组改为分页对象，仓库内前端已同步；外部调用方需读取 data.items。旧 JWT 版本失效，需要重新登录。历史失联订单返回空 productId 及 legacyProductId，界面展示商品已删除，不能继续购买或确认完成。保留原有历史接口别名，但不允许绕过交易 Service。
