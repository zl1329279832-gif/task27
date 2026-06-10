# 培训管理后端系统 (Training Management System)

基于 Spring Boot 3.2 的培训管理后端服务，支持课程管理、学习进度追踪、在线考试（限时/随机抽题/自动判分/防作弊）、证书发放与验证。

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.2.5 |
| JDK | OpenJDK | 17+ |
| 安全 | Spring Security + JWT (jjwt) | 6.x / 0.12.5 |
| ORM | MyBatis-Plus | 3.5.6 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 6.0+ |
| 构建 | Maven | 3.8+ |

## 快速启动

### 前置条件
- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### 1. 初始化数据库
```bash
mysql -u root -p < sql/init.sql
```

### 2. 配置
编辑 `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/training_system
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
```

### 3. 编译运行
```bash
cd training-system
mvn clean package -DskipTests
java -jar target/training-system-1.0.0.jar
```

### 4. 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | password123 | 管理员 |
| instructor1 | password123 | 讲师 |
| student1 | password123 | 学员 |
| auditor1 | password123 | 审核员 |

## 项目结构

```
training-system/
├── sql/init.sql                    # 建表 + 初始数据
├── src/main/java/com/training/
│   ├── config/                     # 配置类 (Security/Redis/MyBatis/WebMvc)
│   ├── security/                   # JWT + Spring Security
│   ├── common/                     # Result/异常处理/防重复提交
│   ├── entity/                     # 数据库实体 + DTO
│   ├── mapper/                     # MyBatis Mapper
│   ├── service/                    # 业务接口
│   │   └── impl/                   # 业务实现
│   ├── controller/                 # REST API
│   ├── scheduler/                  # 定时任务 (考试超时)
│   └── util/                       # 工具类
├── src/main/resources/
│   ├── application.yml
│   └── mapper/                     # MyBatis XML
└── src/test/                       # 测试
```

## API 文档

### 认证
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录获取JWT | 公开 |
| POST | `/api/auth/refresh` | 刷新令牌 | 公开 |
| POST | `/api/auth/logout` | 注销 | 认证 |

请求头: `Authorization: Bearer {token}`

### 用户管理
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/users` | 用户列表 | ADMIN |
| POST | `/api/users` | 创建用户 | ADMIN |
| PUT | `/api/users/{id}` | 更新用户 | ADMIN |
| DELETE | `/api/users/{id}` | 删除用户 | ADMIN |
| PUT | `/api/users/{id}/reset-password` | 重置密码 | ADMIN |

### 课程管理
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/courses` | 课程列表 | 所有角色 |
| GET | `/api/courses/{id}` | 课程详情 | 所有角色 |
| POST | `/api/courses` | 创建课程 | ADMIN/INSTRUCTOR |
| PUT | `/api/courses/{id}` | 更新课程 | ADMIN/INSTRUCTOR |
| DELETE | `/api/courses/{id}` | 删除课程 | ADMIN/INSTRUCTOR |
| POST | `/api/courses/{id}/publish` | 发布课程 | ADMIN/INSTRUCTOR |

### 章节管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/courses/{courseId}/chapters` | 章节列表 |
| POST | `/api/courses/{courseId}/chapters` | 创建章节 |
| PUT | `/api/courses/{courseId}/chapters/{id}` | 更新章节 |
| DELETE | `/api/courses/{courseId}/chapters/{id}` | 删除章节 |
| PUT | `/api/courses/{courseId}/chapters/sort` | 排序 |

### 课件管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chapters/{chapterId}/coursewares` | 课件列表 |
| POST | `/api/chapters/{chapterId}/coursewares` | 上传课件 (multipart) |
| GET | `/api/coursewares/{id}/download` | 下载课件 |

### 班级管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | `/api/classes` | 班级列表/创建 |
| POST | `/api/classes/{id}/students` | 添加学员 |
| DELETE | `/api/classes/{id}/students/{studentId}` | 移除学员 |
| POST | `/api/classes/{id}/courses` | 关联课程 |
| GET | `/api/classes/{id}/students` | 班级学员列表 |

### 学习记录
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/learning/heartbeat` | 上报学习进度 | STUDENT |
| GET | `/api/learning/course/{courseId}/progress` | 课程进度 | 认证 |
| GET | `/api/learning/course/{courseId}/chapters` | 章节级进度 | 认证 |

### 题库管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/questions` | 题目列表 (支持 courseId/type/difficulty 筛选) |
| POST | `/api/questions` | 创建题目 |
| POST | `/api/questions/batch` | 批量导入 |
| PUT | `/api/questions/{id}` | 更新题目 |
| DELETE | `/api/questions/{id}` | 删除题目 (软删除) |

### 考试管理
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/exams` | 考试列表 | 所有角色 |
| POST | `/api/exams` | 创建考试 | ADMIN/INSTRUCTOR |
| POST | `/api/exams/{id}/publish` | 发布考试 | ADMIN/INSTRUCTOR |
| POST | `/api/exams/{id}/start` | 开始考试 | STUDENT |
| POST | `/api/exams/submit` | 提交答卷 | STUDENT |
| POST | `/api/exams/{id}/report-tab-switch` | 上报切屏 | STUDENT |

### 答卷管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/answer-sheets` | 答卷列表 |
| GET | `/api/answer-sheets/{id}` | 答卷详情 (含已删题目回显) |
| POST | `/api/answer-sheets/{id}/regrade` | 重新批改 |

### 成绩管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/grades/student/{studentId}` | 学员成绩 |
| GET | `/api/grades/exam/{examId}` | 考试成绩 |
| GET | `/api/grades/course/{courseId}/stats` | 成绩统计 |

### 证书管理
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/certificates/issue` | 发放证书 | ADMIN |
| GET | `/api/certificates/student/{studentId}` | 学员证书 | 认证 |
| POST | `/api/certificates/{id}/revoke` | 撤销证书 | ADMIN/AUDITOR |
| GET | `/api/certificates/verify/{certNo}` | 编号验证 | **公开** |
| GET | `/api/certificates/verify-link/{token}` | 链接验证 | **公开** |
| POST | `/api/certificates/{id}/regenerate-token` | 重新生成验证链接 | ADMIN |

### 审核
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/audit/certificates` | 证书列表 | AUDITOR |
| GET | `/api/audit/certificates/{id}` | 证书详情 | AUDITOR |

## 状态流转规则

### 课程状态
```
DRAFT → PUBLISHED → ARCHIVED
```
- 只有 DRAFT 状态的课程可以删除和修改题目
- PUBLISHED 后才能被学员学习

### 考试状态
```
DRAFT → PUBLISHED → CLOSED
```
- DRAFT: 可编辑题目和配置
- PUBLISHED: 学员可参加考试
- CLOSED: 考试结束，不可再参加

### 答卷状态
```
IN_PROGRESS → SUBMITTED      (正常提交)
IN_PROGRESS → TIMED_OUT      (超时自动提交)
IN_PROGRESS → AUTO_SUBMITTED (切屏超限自动提交)
```

### 证书状态
```
VALID → REVOKED  (管理员/审核员撤销)
VALID → EXPIRED  (过期自动标记)
```

### 学习记录状态
```
NOT_STARTED → IN_PROGRESS → COMPLETED
```

## 核心业务规则

### 学习进度
- 学员通过心跳接口 (`POST /api/learning/heartbeat`) 每 30 秒上报一次播放位置
- 进度计算: `学习时长 / 课件总时长 × 100%` (上限 100%)
- 章节完成: 该章节下所有课件进度达 100%
- 课程完成率: 已完成章节数 / 总章节数 × 100%

### 考试机制
- **限时**: 创建考试时设置 `durationMinutes`，超时自动交卷
- **随机抽题**: `randomize=true` 时从题库中随机抽取 `questionCount` 道题
- **自动判分**: 客观题 (单选/多选/判断/填空) 自动批改，主观题待人工批改
- **补考限制**: `maxAttempts` 控制最大考试次数
- **断点续考**: 中途退出后重新进入，恢复上次答题进度和剩余时间
- **防作弊**: 前端上报切屏事件，超过 `maxTabSwitches` 次自动交卷

### 防重复提交
- 答卷提交使用 Redis `SET NX` 原子操作，同一答卷只能提交一次
- 接口级别支持 `@Idempotent` 注解防重复请求

### 题目删除与历史回显
- 题目采用软删除 (`status=DELETED`)，不物理删除
- 查看历史答卷时，已删题目显示 "[该题目已删除]"，保留学生原始作答记录
- 已删题目自动给满分

### 证书发放条件
- 课程完成率 = 100%
- 至少一次考试成绩 ≥ 及格分
- 同一学员同一课程只能发放一张有效证书

### 证书编号
- 格式: `CERT-{courseId}-{yyyyMMdd}-{4位序号}`
- 使用 Redis INCR 保证编号唯一性

### 证书验证
- 编号验证: `GET /api/certificates/verify/{certNo}` — 公开接口
- 链接验证: `GET /api/certificates/verify-link/{token}` — 公开接口
- 验证链接有效期 7 天 (可配置)，过期后可重新生成
- 已撤销证书验证返回撤销原因

## 权限矩阵

| 功能 | ADMIN | INSTRUCTOR | STUDENT | AUDITOR |
|------|:-----:|:----------:|:-------:|:-------:|
| 用户管理 | ✓ | | | |
| 课程 CRUD | ✓ | ✓ (自己的) | 查看 | 查看 |
| 班级管理 | ✓ | ✓ (自己的) | | |
| 题库管理 | ✓ | ✓ | | 查看 |
| 考试管理 | ✓ | ✓ (自己的) | 参加 | 查看 |
| 学习记录 | 全部 | 自己班级 | 自己的 | 查看 |
| 证书发放 | ✓ | | | |
| 证书撤销 | ✓ | | | ✓ |
| 证书审核 | | | | ✓ |

## 数据库表 (18 张)

| 表名 | 说明 |
|------|------|
| sys_user | 用户 |
| course | 课程 |
| chapter | 章节 |
| courseware | 课件 |
| clazz | 班级 |
| clazz_student | 班级-学员 |
| clazz_course | 班级-课程 |
| learning_record | 学习记录 |
| question | 题库 |
| exam | 考试 |
| exam_question | 考试-题目 |
| answer_sheet | 答卷 |
| answer_detail | 答题明细 |
| grade | 成绩 |
| certificate | 证书 |
| certificate_revocation | 证书撤销记录 |

## 运行测试

```bash
mvn test
```

## 许可证

MIT License
