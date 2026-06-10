# 培训机构后端管理系统

基于 Spring Boot 的培训机构后端管理系统，提供课程管理、学习进度跟踪、在线考试、成绩管理和证书发放等完整功能。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.2.5 | 应用框架 |
| Spring Security | 6.x | 安全认证与授权 |
| JWT (JJWT) | 0.12.5 | Token 认证 |
| MyBatis | 3.0.3 | ORM（XML映射） |
| MySQL | 8.x | 数据库 |
| Redis | 7.x | 缓存、考试计时、防重复提交 |
| PageHelper | 2.1.0 | 分页插件 |
| H2 | - | 测试数据库 |
| Lombok | - | 代码简化 |

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+

### 1. 初始化数据库
```bash
mysql -u root -p < sql/init.sql
```

### 2. 修改配置
编辑 `src/main/resources/application.yml`，配置数据库和 Redis 连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/training_db
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
mvn clean compile
mvn spring-boot:run
```

### 4. 运行测试
```bash
mvn test
```

### 默认管理员账号
- 用户名: `admin`
- 密码: `admin123`

## 项目结构

```
training-system/
├── sql/init.sql                          # 数据库初始化脚本
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/training/
    │   │   ├── TrainingApplication.java  # 启动类
    │   │   ├── config/                   # 配置类
    │   │   │   ├── SecurityConfig.java   # Spring Security 配置
    │   │   │   ├── RedisConfig.java      # Redis 配置
    │   │   │   ├── WebMvcConfig.java     # Web MVC 配置
    │   │   │   └── FileStorageConfig.java# 文件存储配置
    │   │   ├── security/                 # 安全认证
    │   │   │   ├── JwtTokenProvider.java # JWT Token 生成/验证
    │   │   │   ├── JwtAuthFilter.java    # JWT 认证过滤器
    │   │   │   ├── CustomUserDetails.java
    │   │   │   └── UserDetailsServiceImpl.java
    │   │   ├── controller/               # REST 控制器（12个）
    │   │   ├── service/impl/             # 业务逻辑（12个服务）
    │   │   ├── mapper/                   # MyBatis Mapper 接口（17个）
    │   │   ├── entity/                   # 实体类（17个）
    │   │   ├── dto/                      # 请求/响应 DTO
    │   │   ├── enums/                    # 枚举类（10个）
    │   │   ├── exception/                # 全局异常处理
    │   │   ├── scheduler/                # 定时任务
    │   │   └── util/                     # 工具类
    │   └── resources/
    │       ├── application.yml           # 应用配置
    │       ├── mapper/                   # MyBatis XML 映射（17个）
    │       └── db/init.sql               # 数据库脚本副本
    └── test/                             # 单元测试
```

## 数据库表结构（17张表）

### 用户与权限
| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表 |
| `sys_role` | 角色表（ADMIN/INSTRUCTOR/STUDENT/AUDITOR） |
| `sys_user_role` | 用户角色关联表 |

### 课程管理
| 表名 | 说明 |
|------|------|
| `course` | 课程表（含完成率阈值） |
| `chapter` | 章节表 |
| `courseware` | 课件表（支持视频/文档/PPT等） |

### 班级管理
| 表名 | 说明 |
|------|------|
| `clazz` | 班级表 |
| `clazz_student` | 班级学员关联表 |

### 学习进度
| 表名 | 说明 |
|------|------|
| `learning_record` | 学习记录表（按课件粒度） |

### 考试系统
| 表名 | 说明 |
|------|------|
| `question` | 题库（单选/多选/判断/填空） |
| `exam` | 考试表（含时长、抽题数、补考限制） |
| `exam_question` | 考试题目表（含题目快照） |
| `exam_submission` | 答卷表 |
| `submission_answer` | 答题记录表（含题目快照） |

### 成绩与证书
| 表名 | 说明 |
|------|------|
| `score` | 成绩表（含审核状态） |
| `certificate` | 证书表（含验证链接） |
| `certificate_revocation` | 证书撤销记录表 |

## 角色权限

| 角色 | 权限范围 |
|------|---------|
| **ADMIN** (管理员) | 全部权限：用户管理、课程管理、考试管理、证书管理 |
| **INSTRUCTOR** (讲师) | 课程/章节/课件管理、题库/考试管理 |
| **STUDENT** (学员) | 学习课程、参加考试、查看成绩和证书 |
| **AUDITOR** (审核员) | 审核成绩、审核和发放/撤销证书 |

## API 接口

### 认证接口 `/api/auth`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/login` | 登录 | 公开 |
| POST | `/register` | 注册（默认学员角色） | 公开 |
| POST | `/refresh` | 刷新 Token | 公开 |

### 用户管理 `/api/users`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/` | 用户列表（分页） | ADMIN |
| GET | `/{id}` | 用户详情 | ADMIN |
| PUT | `/{id}` | 修改用户 | ADMIN |
| DELETE | `/{id}` | 删除用户 | ADMIN |
| POST | `/{id}/roles` | 分配角色 | ADMIN |
| DELETE | `/{id}/roles` | 移除角色 | ADMIN |
| GET | `/me` | 当前用户信息 | 已认证 |

### 课程管理 `/api/courses`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/` | 课程列表 | 已认证 |
| GET | `/{id}` | 课程详情 | 已认证 |
| POST | `/` | 创建课程 | ADMIN/INSTRUCTOR |
| PUT | `/{id}` | 修改课程 | ADMIN/INSTRUCTOR |
| DELETE | `/{id}` | 删除课程 | ADMIN/INSTRUCTOR |
| POST | `/{id}/publish` | 发布课程 | ADMIN/INSTRUCTOR |
| POST | `/{id}/archive` | 归档课程 | ADMIN |

### 章节管理 `/api/chapters`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/?courseId=` | 章节列表 | 已认证 |
| POST | `/` | 创建章节 | ADMIN/INSTRUCTOR |
| PUT | `/{id}` | 修改章节 | ADMIN/INSTRUCTOR |
| DELETE | `/{id}` | 删除章节 | ADMIN/INSTRUCTOR |

### 课件管理 `/api/coursewares`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/?chapterId=` | 课件列表 | 已认证 |
| POST | `/` | 上传课件（multipart） | ADMIN/INSTRUCTOR |
| PUT | `/{id}` | 修改课件信息 | ADMIN/INSTRUCTOR |
| DELETE | `/{id}` | 删除课件 | ADMIN/INSTRUCTOR |
| GET | `/{id}/download` | 下载课件 | 已认证 |

### 班级管理 `/api/clazzes`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/` | 班级列表 | 已认证 |
| POST | `/` | 创建班级 | ADMIN/INSTRUCTOR |
| POST | `/{id}/students` | 添加学员 | ADMIN/INSTRUCTOR |
| DELETE | `/{id}/students/{sid}` | 移除学员 | ADMIN/INSTRUCTOR |
| GET | `/{id}/students` | 学员列表 | ADMIN/INSTRUCTOR |

### 学习进度 `/api/learning`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/progress` | 更新学习进度 | STUDENT |
| GET | `/course/{courseId}` | 课程学习记录 | STUDENT |
| GET | `/progress/{courseId}` | 课程完成进度 | STUDENT |

### 题库管理 `/api/questions`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/` | 题目列表 | ADMIN/INSTRUCTOR |
| POST | `/` | 创建题目 | ADMIN/INSTRUCTOR |
| PUT | `/{id}` | 修改题目 | ADMIN/INSTRUCTOR |
| DELETE | `/{id}` | 删除题目（软删除） | ADMIN/INSTRUCTOR |

### 考试管理 `/api/exams`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/` | 考试列表 | 已认证 |
| POST | `/` | 创建考试 | ADMIN/INSTRUCTOR |
| POST | `/{id}/publish` | 发布考试（随机抽题生成快照） | ADMIN/INSTRUCTOR |
| POST | `/{id}/close` | 关闭考试 | ADMIN/INSTRUCTOR |

### 答卷管理 `/api/exam-submissions`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/start/{examId}` | 开始考试 / 断点续考 | STUDENT |
| POST | `/save-answer` | 保存单题答案（实时） | STUDENT |
| POST | `/submit` | 提交答卷（自动判分） | STUDENT |
| POST | `/heartbeat` | 心跳（切屏检测） | STUDENT |
| GET | `/{id}` | 答卷详情 | 已认证 |
| GET | `/{id}/answers` | 答题记录（含题目快照） | 已认证 |

### 成绩管理 `/api/scores`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/student/{id}` | 学员成绩 | 已认证 |
| GET | `/exam/{id}` | 考试成绩 | ADMIN/INSTRUCTOR/AUDITOR |
| GET | `/my` | 我的成绩 | STUDENT |
| POST | `/{id}/approve` | 审核通过 | ADMIN/AUDITOR |
| POST | `/{id}/reject` | 审核拒绝 | ADMIN/AUDITOR |

### 证书管理 `/api/certificates`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/issue` | 发放证书 | ADMIN/AUDITOR |
| GET | `/{id}` | 证书详情 | 已认证 |
| GET | `/student/{id}` | 学员证书 | 已认证 |
| GET | `/my` | 我的证书 | STUDENT |
| GET | `/verify/cert-no/{no}` | 按编号验证 | 公开 |
| GET | `/verify/token/{token}` | 按链接验证 | 公开 |
| POST | `/revoke` | 撤销证书 | ADMIN/AUDITOR |
| POST | `/{id}/regenerate-token` | 重新生成验证链接 | ADMIN/AUDITOR |

## 核心业务规则

### 考试流程
1. **创建考试** → 设置时长、抽题数量、及格分、补考次数
2. **发布考试** → 从题库随机抽题，生成题目快照冻结
3. **开始考试** → 创建答卷，Redis 记录开始时间
4. **答题过程** → 每次答题实时保存到 Redis + DB（支持断点续考）
5. **提交答卷** → Redis 幂等锁防重复提交，自动判分
6. **超时处理** → 定时任务每 60 秒扫描，超时自动提交已答题目

### 防作弊机制
- 心跳检测切屏次数，超过 5 次自动提交
- 考试限时，后端严格校验时间

### 题目快照
- 考试发布时冻结题目内容到 `exam_question.question_snapshot`
- 每份答卷创建时复制快照到 `submission_answer.question_snapshot`
- 即使题目被软删除，历史答卷仍可完整回显

### 自动判分规则
| 题型 | 判分方式 |
|------|---------|
| 单选题 | 答案完全匹配（忽略大小写和空格） |
| 多选题 | 答案集合完全一致（逗号分隔，排序后比较） |
| 判断题 | 直接匹配 |
| 填空题 | 去除首尾空格后匹配（忽略大小写） |

### 证书发放条件
1. 课程完成率 ≥ 课程设定的阈值（默认 80%）
2. 考试最高成绩 ≥ 及格分数（如指定了考试）
3. 同一学员同一课程不可重复发放

### 证书验证
- 通过证书编号验证：永久有效（除非被撤销）
- 通过验证链接验证：JWT Token 带过期时间（默认 30 天），过期后可重新生成
- 证书撤销后，两种验证方式均返回失败

## 状态流转

### 课程状态
```
DRAFT（草稿）→ PUBLISHED（已发布）→ ARCHIVED（已归档）
```

### 考试状态
```
DRAFT（草稿）→ PUBLISHED（已发布，生成题目快照）→ CLOSED（已关闭）
```

### 答卷状态
```
IN_PROGRESS（进行中）→ SUBMITTED（已提交）
                     → TIMED_OUT（已超时）
                     → AUTO_SUBMITTED（自动提交，切屏过多）
```

### 成绩状态
```
PENDING（待审核）→ APPROVED（已通过）
                → REJECTED（未通过）
```

### 证书状态
```
VALID（有效）→ REVOKED（已撤销，不可恢复）
```

## 边界情况处理

| 场景 | 处理方式 |
|------|---------|
| 重复提交答卷 | Redis SETNX 幂等锁，30秒内第二次提交被拒绝 |
| 考试超时 | 定时任务 60 秒轮询 + 提交时后端二次校验 |
| 断点续考 | 每次答题写 Redis Hash，重新开考时恢复进度和剩余时间 |
| 题目被删除 | 软删除（保留数据），答卷通过快照回显完整题目 |
| 证书编号重复 | 数据库 UNIQUE 约束 + 应用层最多重试 3 次 |
| 证书验证链接过期 | JWT Token 过期后验证失败，可调用接口重新生成 |
| 补考次数限制 | 开考前检查已提交次数，超限拒绝 |
| 切屏过多 | 心跳上报切屏次数，超过 5 次自动提交 |

## 测试覆盖

- **ExamSubmissionServiceTest**: 开考、断点续考、重复提交、自动判分（4种题型）、超时处理
- **CertificateServiceTest**: 证书发放条件验证、证书编号/链接验证、撤销流程
- **LearningRecordServiceTest**: 进度更新、完成率计算
- **ScoreServiceTest**: 成绩创建、审核状态流转
