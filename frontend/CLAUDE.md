# 慕陶心理小程序 — 全栈项目

## 项目概述
心理健康服务平台，包含学生端和老师端。老师端有底部导航栏（主页/后台/预约/我的）。

## 前端（本目录）
- 微信小程序原生框架（TypeScript）
- 路径：`C:\Users\hp\WeChatProjects\miniprogram-2`
- 入口：`miniprogram/app.ts` → 微信登录 → 存储 currentUser
- 自定义 tabBar：`miniprogram/custom-tab-bar/`
- 后端 base URL：`https://botany-refined-pleading.ngrok-free.dev`

## 后端（关联项目）
- Spring Boot 4.0.7 + MyBatis + MySQL
- 路径：`C:\Users\hp\Desktop\school\作业\移动终端\mu\mutao-behind`
- 入口：`src/main/java/com/mutao/mutaobehind/MutaoBehindApplication.java`
- 端口：8081
- 数据库：`mutao_base`，用户名 root，密码 1234
- 表名：counselors, appointments, assessment_records, assessment, users

## 前后端接口对照
| 前端调用 | 后端 Controller | 方法 |
|---------|----------------|------|
| GET /api/counselors | CounselorController | getCounselorList |
| GET /api/counselors/:id | CounselorController | getCounselorDetail |
| POST /api/wx/login | LoginController | wxLogin |
| GET /api/teacher/profile | TeacherProfileController | getProfile |
| PUT /api/teacher/profile | TeacherProfileController | updateProfile |
| GET /api/teacher/students | StudentController | getStudents |
| GET /api/teacher/students/:id/records | StudentController | getStudentRecords |
| GET /api/teacher/records | TeacherController | getAllStudentRecords |
| GET /api/teacher/records/:id | TeacherController | getRecordDetail |
| GET /api/teacher/dashboard/stats | DashboardController | getStats |
| GET /api/appointments | AppointmentController | getAppointments |
| PUT /api/appointments/:id | AppointmentController | updateAppointmentStatus |

## 认证方式
- 前端：`app.ts` 启动时先同步写入默认老师身份（dev 模式），再异步调用 wx.login
- 后端大部分接口需要 `?teacherId=xxx` 或 `?counselorId=xxx` 参数
- 正式环境通过微信登录建立 session，目前开发阶段用 counselorId 兜底

## 已知约定
- 前端使用驼峰字段名（counselorId, shortDesc）
- 数据库使用下划线（counselor_id, short_desc）
- MyBatis SQL 中用别名映射：`counselor_id as counselorId`
