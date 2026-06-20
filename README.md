# 心理问卷

心理健康服务平台，包含**学生端**和**老师端**。学生可预约心理咨询，老师可管理学生、查看记录、处理预约。

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | 微信小程序原生框架（TypeScript） |
| 后端 | Spring Boot 4.0.7 + MyBatis + MySQL 8.0 |
| 数据库 | MySQL（mutao_base） |

## 项目结构

```
mutao-psychology/
├── frontend/          # 微信小程序前端
│   ├── miniprogram/   # 小程序源码
│   │   ├── pages/     # 页面
│   │   ├── utils/     # 工具函数
│   │   ├── app.ts     # 入口文件
│   │   └── custom-tab-bar/  # 自定义 TabBar
│   ├── package.json
│   └── project.config.json
├── backend/           # Spring Boot 后端
│   └── src/main/
│       ├── java/com/mutao/mutaobehind/
│       │   ├── controller/   # API 控制器
│       │   ├── service/      # 业务逻辑
│       │   ├── mapper/       # MyBatis 映射
│       │   └── model/        # 数据模型
│       └── resources/
│           ├── init.sql              # 数据库初始化脚本
│           ├── application-template.yml  # 配置模板
│           └── application.yml       # 本地配置（不提交）
└── docs/
```

## 快速开始

### 1. 数据库设置

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS mutao_base DEFAULT CHARSET utf8mb4;"

# 2. 执行初始化脚本（建表 + 种子数据）
mysql -u root -p mutao_base < backend/src/main/resources/init.sql
```

### 2. 后端配置

```bash
cd backend

# 复制配置模板，填入你的真实数据库密码和微信小程序密钥
cp src/main/resources/application-template.yml src/main/resources/application.yml

# 编辑 application.yml，修改以下三项：
#   spring.datasource.username  → 你的 MySQL 用户名
#   spring.datasource.password  → 你的 MySQL 密码
#   wechat.appid                → 你的微信 AppID
#   wechat.secret               → 你的微信 AppSecret
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
# 后端运行在 http://localhost:8081
```

### 4. 打开前端

1. 下载[微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)
2. 导入项目，选择 `frontend/` 目录
3. 填写你自己的微信小程序 AppID
4. 开始开发

## 认证说明

- 开发阶段使用 `counselorId` 参数兜底认证
- 正式环境通过 `wx.login` 建立微信 session
- 后端大部分接口需要 `?teacherId=xxx` 或 `?counselorId=xxx` 参数

## 注意事项

- **不要提交 `application.yml`**：该文件已加入 `.gitignore`，真实密码仅保留在本地
- **不要提交 `project.private.config.json`**：微信小程序的本地配置文件
- 数据库密码和微信 AppSecret 属于敏感信息，泄露会导致安全风险
