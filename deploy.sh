#!/usr/bin/env bash
set -euo pipefail

# 配置区，请按需修改
REPO_URL="https://github.com/SpreMars/SkillsFinder.git"
BRANCH="${BRANCH:-main}"
PROJECT_ROOT="${PROJECT_ROOT:-/root/SkillsFinder}"
DOMAIN="${DOMAIN:-marslander.cloud}"
GITHUB_TOKEN="${GITHUB_TOKEN:-}"

echo "================= SkillsFinder 一键部署 ================="

# 1) 准备工作
mkdir -p "$PROJECT_ROOT"
cd "$PROJECT_ROOT"

# 2) 拉取代码
if [ -d ".git" ]; then
  echo "[1/6] 更新代码..."
  git fetch origin
  git reset --hard "origin/${BRANCH}"
else
  echo "[1/6] 克隆仓库..."
  git clone -b "$BRANCH" "$REPO_URL" .
fi

# 3) 写入 docker-compose.yml
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  postgres:
    image: postgres:18.3-alpine
    container_name: skillsfinder-postgres
    environment:
      POSTGRES_USER: agent1223
      POSTGRES_PASSWORD: agent1223
      POSTGRES_DB: postgres
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - app-network

  redis:
    image: redis:8-alpine
    container_name: skillsfinder-redis
    command: redis-server --requirepass ""
    volumes:
      - redis-data:/data
    networks:
      - app-network

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: skillsfinder-backend
    depends_on:
      - postgres
      - redis
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/postgres
      - SPRING_DATASOURCE_USERNAME=agent1223
      - SPRING_DATASOURCE_PASSWORD=agent1223
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PORT=6379
      - SPRING_DATA_REDIS_PASSWORD=
    networks:
      - app-network
    ports:
      - "8080:8080"

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: skillsfinder-frontend
    depends_on:
      - backend
    networks:
      - app-network
    ports:
      - "80:80"

volumes:
  postgres-data:
  redis-data:

networks:
  app-network:
    driver: bridge
EOF

# 4) 修改后端配置
mkdir -p backend/src/main/resources
cat > backend/src/main/resources/application.yml << 'EOF'
server:
  port: 8080

spring:
  application:
    name: skills-finder

  datasource:
    url: jdbc:postgresql://postgres:5432/postgres
    username: agent1223
    password: agent1223
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  data:
    redis:
      host: redis
      port: 6379
      password: ""

github:
  api:
    token: "${GITHUB_TOKEN:-}"
    base-url: https://api.github.com
    search-url: https://api.github.com/search/repositories

logging:
  level:
    com.skillsfinder: INFO
    org.springframework.web: INFO
EOF

# 5) 构建并启动
echo "[Step] 重新构建并启动服务..."
docker-compose down -v
docker-compose up -d --build

# 6) 验证
echo "------------------------------------------------------------"
echo "请等待容器完成启动后访问：http://${DOMAIN}"
echo "------------------------------------------------------------"

sleep 20
echo "容器状态："
docker-compose ps
echo "后端日志："
docker-compose logs -n 50 backend

echo "部署完成！"
