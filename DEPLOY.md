# SkillsFinder 部署指南

## 服务器要求
- Alibaba Cloud 阿里云服务器
- 已安装 Docker 和 Docker Compose
- 已安装 PostgreSQL 15+ (端口 5432)
- 已安装 Redis 7+ (端口 6379)
- 域名已配置解析

## 一、服务器初始化

### 1.1 安装 Docker（如未安装）
```bash
curl -fsSL https://get.docker.com | sh
systemctl enable docker
systemctl start docker
```

### 1.2 安装 Docker Compose
```bash
curl -L "https://github.com/docker/compose/releases/download/v2.23.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
```

### 1.3 配置防火墙
```bash
# 开放端口
firewall-cmd --permanent --add-port=80/tcp   # HTTP
firewall-cmd --permanent --add-port=443/tcp  # HTTPS
firewall-cmd --permanent --add-port=5432/tcp  # PostgreSQL
firewall-cmd --permanent --add-port=6379/tcp  # Redis
firewall-cmd --reload
```

## 二、克隆代码

```bash
cd /root
rm -rf skillsFinder
git clone https://github.com/SpreMars/SkillsFinder.git
cd SkillsFinder
```

## 三、配置数据库

### 3.1 修改后端配置
```bash
# 修改 application.yml 中的数据库地址
vi backend/src/main/resources/application.yml
```

修改以下内容：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: postgres
    password: postgres
  
  data:
    redis:
      host: localhost
      port: 6379
      password: redis_EbySRj
```

### 3.2 初始化数据库
```bash
# 连接 PostgreSQL 并创建表
docker run -it --rm \
  -e PGPASSWORD=postgres \
  postgres:15 \
  psql -h localhost -U postgres -c "CREATE TABLE IF NOT EXISTS skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    full_name VARCHAR(255) UNIQUE NOT NULL,
    owner VARCHAR(255),
    repository VARCHAR(255),
    language VARCHAR(100),
    star_count INTEGER DEFAULT 0,
    fork_count INTEGER DEFAULT 0,
    html_url VARCHAR(500),
    readme_content TEXT,
    topics TEXT,
    avatar_url VARCHAR(500),
    last_updated TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    previous_star_count INTEGER,
    star_change INTEGER,
    last_crawled_at VARCHAR(100)
  );"
```

## 四、构建并启动

```bash
cd /root/SkillsFinder
docker-compose up -d --build
```

## 五、验证部署

### 5.1 检查容器状态
```bash
docker-compose ps
```

### 5.2 查看日志
```bash
# 后端日志
docker-compose logs -f backend

# 前端日志
docker-compose logs -f frontend
```

### 5.3 访问测试
- 前端：http://你的域名
- 后端 API：http://你的域名/api/skills

## 六、常用命令

```bash
# 重启服务
docker-compose restart

# 停止服务
docker-compose down

# 重新构建并启动
docker-compose up -d --build

# 查看实时日志
docker-compose logs -f
```

## 七、注意事项

1. **数据库字段更新**：如果数据库表结构有变化，需要删除旧表让 Hibernate 重建：
   ```bash
   docker exec -it <postgres容器名> psql -U postgres -c "DROP TABLE skills;"
   docker-compose restart backend
   ```

2. **翻译 API 限额**：MyMemory 免费版每天 1000 次翻译限额用完后，需要等 24 小时刷新

3. **GitHub Token**：如遇到 GitHub API 限流，可更换 GitHub Token

## 八、目录结构

```
SkillsFinder/
├── backend/           # Spring Boot 后端
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/          # Vue3 前端
│   ├── src/
│   ├── package.json
│   └── Dockerfile
├── docker/
│   ├── init.sql      # 数据库初始化脚本
│   └── nginx.conf    # Nginx 配置
├── docker-compose.yml
└── README.md
```

## 九、面试展示要点

> "这是一个 AI Agent Skills 聚合展示平台，后端使用 Spring Boot + PostgreSQL，前端使用 Vue3 + Vite。设计了 4 维度热榜系统（总榜/飙升/新秀/语言），采用定时任务 + Redis 缓存，节省服务器资源。"

---
*Created by SpreMars*
