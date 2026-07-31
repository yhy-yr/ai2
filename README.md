# LocalCare AI 智慧医疗诊疗辅助系统

## 项目定位

基层诊所诊疗流程演示版。

本项目用于课程设计演示，围绕“患者 AI 问诊 -> 预约挂号 -> 医生接诊 -> 病历/检查/处方 -> 患者查看结果 -> 管理员查看业务记录”这一条主线实现。AI 功能支持可选 DeepSeek API 调用，未配置或调用失败时自动回退本地规则版，不使用真实医院级复杂平台能力。

## 技术栈

后端：

- Spring Boot
- Spring Web
- Spring JDBC
- JdbcTemplate
- MySQL

前端：

- Vue 3
- Vite
- Pinia
- Vue Router
- JavaScript

## 已实现功能

- 三角色登录：管理员、医生、患者
- 管理员基础数据管理：科室、药品、医生、患者
- 患者 AI 辅助问诊
- 患者预约挂号
- 医生查看预约
- 医生接诊工作台
- AI 病历草稿生成
- 医生保存电子病历
- 医生录入检查结果
- 医生开具处方
- 患者查看病历、处方、检查结果
- 管理员查看预约、病历、处方、检查结果
- 首页统计

## 数据库配置

数据库配置以 `src/main/resources/application.yml` 为准。

默认连接：

```text
jdbc:mysql://localhost:3306/localcare_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
username: root
password: 200626
```

如需修改，可通过环境变量覆盖：

```bash
export DB_URL="jdbc:mysql://localhost:3306/localcare_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false"
export DB_USERNAME="root"
export DB_PASSWORD="200626"
```

后端启动时会自动创建并幂等初始化演示数据，包括用户、科室、药品、患者、医生、AI 问诊、预约、病历、处方和检查结果。

## DeepSeek API 配置

系统支持调用 DeepSeek API 生成 AI 问诊结果和 AI 病历草稿，并保留本地规则版作为兜底。

macOS / Linux 配置方式：

```bash
export DEEPSEEK_API_KEY="你的 DeepSeek API Key"
mvn spring-boot:run
```

如果不配置 `DEEPSEEK_API_KEY`，系统会自动使用本地规则版 AI，不影响课程演示。

DeepSeek API Key 只在后端通过环境变量读取，不会写入前端、代码或浏览器本地存储。

## 测试账号

```text
管理员：admin / 123456
医生：doctor / 123456
患者：patient / 123456
```

## 启动方式

后端：

```bash
cd 项目根目录
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

## 访问地址

```text
前端页面：http://localhost:5173
后端接口：http://localhost:8081/api
```

前端已使用 hash 路由，常用地址：

```text
http://localhost:5173/#/login
http://localhost:5173/#/workspace
http://localhost:5173/#/patients
http://localhost:5173/#/ai
```

登录后统一进入 `/workspace`，系统会根据管理员、医生或患者身份显示对应工作区。

## 完整演示流程

1. 患者登录。
2. 进入 AI 辅助问诊，输入症状并生成问诊结果。
3. 进入预约挂号，选择科室、医生、日期、时间段并提交预约。
4. 医生登录。
5. 进入我的预约，点击开始接诊。
6. 在接诊工作台生成 AI 病历草稿。
7. 医生确认并保存电子病历。
8. 医生录入检查结果。
9. 医生开具处方。
10. 患者再次登录，查看我的病历、我的处方、检查结果。
11. 管理员登录，查看预约管理、病历管理、处方管理、检查结果管理。

流程简述：

```text
患者登录 -> AI 问诊 -> 预约挂号 -> 医生登录 -> 查看预约 -> 开始接诊 -> 生成 AI 病历草稿 -> 保存病历 -> 录入检查结果 -> 开具处方 -> 患者查看结果 -> 管理员查看业务记录
```
