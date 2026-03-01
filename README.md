# SkillsFinder

发现优质 AI Agent Skills 的网站。

## 功能特性

- 🔍 搜索和筛选 GitHub 上的 Agent Skills
- ⭐ 按 Star 数排序
- 🌍 支持多种编程语言筛选
- 🤖 自动定时从 GitHub 爬取最新 Skills
- 📦 Docker 一键部署

## 技术栈

- **后端**: Spring Boot 3.2 + Java 17
- **数据库**: PostgreSQL 15
- **缓存**: Redis 7
- **前端**: Vue3 + Vite
- **部署**: Docker + Nginx

## 快速开始

### 前置要求

- Docker & Docker Compose
- GitHub Token (可选，用于提高 API 速率限制)

### 1. 克隆项目

```bash
git clone <your-repo>
cd skillsFinder
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，填入你的 GitHub Token
```

### 3. 启动服务

```bash
docker-compose up -d
```

访问:
- 前端: http://localhost
- 后端 API: http://localhost:8080

### 4. 手动触发爬取

```bash
curl -X POST http://localhost:8080/api/skills/crawl
```

## 项目结构

```
skillsFinder/
├── backend/           # Spring Boot 后端
│   ├── src/
│   │   └── main/
│   │       └── java/com/skillsfinder/
│   │           ├── controller/   # REST API
│   │           ├── service/       # 业务逻辑
│   │           ├── repository/   # 数据访问
│   │           └── entity/       # 实体类
│   └── pom.xml
├── frontend/          # Vue3 前端
│   ├── src/
│   │   ├── views/     # 页面组件
│   │   └── components/
│   └── package.json
├── docker/            # Docker 配置
│   └── nginx.conf
└── docker-compose.yml
```

## API 文档

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/skills | 获取 Skills 列表 |
| GET | /api/skills/{id} | 获取 Skill 详情 |
| GET | /api/skills/languages | 获取所有编程语言 |
| POST | /api/skills/crawl | 手动触发爬取 |

### 查询参数

- `keyword`: 搜索关键词
- `language`: 编程语言筛选
- `page`: 页码 (默认 0)
- `size`: 每页数量 (默认 12)

## 部署

项目已配置 Docker 多阶段构建，直接运行:

```bash
docker-compose up -d --build
```

## License

MIT
