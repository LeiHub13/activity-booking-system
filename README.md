# 预约报名管理系统

## 项目简介

预约报名管理系统是一个前后端分离的活动预约报名平台，主要用于活动发布、用户报名、管理员审核、站内通知和数据统计等场景。

系统支持普通用户和管理员两类角色：

| 角色 | 说明 |
| --- | --- |
| 普通用户 | 浏览活动、报名活动、取消报名、查看报名记录、接收站内通知 |
| 管理员 | 发布活动、编辑活动、下线活动、审核报名、查看报名统计和热门活动排行 |

项目重点实现了 JWT 登录认证、Redis 登录态管理、角色权限拦截、报名并发控制、站内通知和活动统计分析。

## 技术栈

后端技术：

| 技术 | 说明 |
| --- | --- |
| Java 17 | 后端开发语言 |
| Spring Boot 3.3.5 | 后端基础框架 |
| Spring Web | RESTful API 开发 |
| Spring Security | 登录认证和角色权限控制 |
| JWT | Token 登录凭证 |
| Redis | 登录 token 存储和失效控制 |
| MyBatis Plus | 数据访问和分页 |
| MySQL | 业务数据存储 |
| Lombok | 简化实体类和 VO/DTO 编写 |
| Maven | 后端依赖管理和构建 |

前端技术：

| 技术 | 说明 |
| --- | --- |
| Vue 3 | 前端页面开发 |
| Vue Router | 前端路由和页面跳转 |
| Vite | 前端构建工具 |
| TypeScript | 前端类型约束 |
| CSS | 页面样式和响应式布局 |

## 功能模块

| 模块 | 功能 |
| --- | --- |
| 用户认证 | 用户注册、登录、退出登录、JWT 鉴权 |
| 角色权限 | 普通用户和管理员角色区分，后端接口权限拦截 |
| 用户资料 | 查看当前用户信息，修改真实姓名、手机号、邮箱 |
| 活动管理 | 管理员发布活动、编辑活动、下线活动 |
| 活动浏览 | 用户分页查看已发布活动，查看活动详情 |
| 活动报名 | 用户报名活动、取消报名、查看我的报名记录 |
| 报名审核 | 管理员查询报名列表，审核通过或拒绝 |
| 站内通知 | 审核通过或拒绝后自动生成通知，用户可查看和标记已读 |
| 数据统计 | 统计各活动报名人数，查看热门活动排行 |
| 并发控制 | 通过数据库条件更新和事务控制防止重复报名、超额报名和重复扣减人数 |

## 启动方式

### 1. 环境准备

需要提前安装：

| 环境 | 建议版本 |
| --- | --- |
| JDK | 17 |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.x |
| Redis | 6.x+ |

后端默认配置见 `src/main/resources/application.yaml`：

```yaml
server:
  address: 127.0.0.1
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/activity_booking_system
    username: root
    password: 1234
  data:
    redis:
      host: localhost
      port: 8989
```

如果你的 MySQL 密码或 Redis 端口不同，需要先修改 `application.yaml`。

### 2. 启动后端

在后端项目目录执行：

```powershell
cd D:\CodexFile\预约报名管理系统\activity-booking-system\activity-booking-system
mvn spring-boot:run
```

也可以使用 Maven Wrapper：

```powershell
.\mvnw.cmd spring-boot:run
```

后端启动地址：

```text
http://127.0.0.1:8080
```

### 3. 启动前端

在前端项目目录执行：

```powershell
cd D:\CodexFile\预约报名管理系统\activity-booking-system\frontend
npm install
npm run dev
```

前端访问地址：

```text
http://localhost:5173
```

## 数据库初始化方式

项目提供了初始化脚本：

```text
sql/init.sql
```

初始化方式一：在命令行执行：

```powershell
mysql -uroot -p1234 < sql/init.sql
```

初始化方式二：进入 MySQL 客户端后执行：

```sql
source D:/CodexFile/预约报名管理系统/activity-booking-system/activity-booking-system/sql/init.sql;
```

初始化脚本会完成：

| 内容 | 说明 |
| --- | --- |
| 创建数据库 | `activity_booking_system` |
| 创建用户表 | `user` |
| 创建角色表 | `role` |
| 创建用户角色关联表 | `user_role` |
| 创建活动表 | `activity` |
| 创建报名记录表 | `registration` |
| 创建通知表 | `notice` |
| 初始化角色 | `ADMIN` 管理员、`USER` 普通用户 |
| 创建关键索引 | 活动状态、报名状态、用户报名、活动报名统计等索引 |

## 接口文档地址

接口文档单独保存在：

```text
接口文档.md
```

## 测试账号

项目初始化脚本只初始化角色，不内置固定用户账号。推荐按下面方式创建测试账号。

### 普通用户账号

通过前端注册页或注册接口创建：

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| user | 123456 | 普通用户 |

注册后系统会自动分配 `USER` 角色。

### 管理员账号

先通过前端注册页或注册接口创建：

| 账号 | 密码 | 初始角色 |
| --- | --- | --- |
| admin | 123456 | 普通用户 |

然后执行下面 SQL，将该账号绑定为管理员：

```sql
INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM user u
JOIN role r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin';
```

管理员重新登录后即可访问 `/api/admin/**` 接口和前端管理员页面。
