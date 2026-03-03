#!/bin/bash
set -e

echo "================= SkillsFinder 一键部署 ================="

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
DOMAIN="${DOMAIN:-marslander.cloud}"

cd "$PROJECT_ROOT"

# 1) 拉取最新代码
if [ -d ".git" ]; then
  echo "[1/5] 更新代码..."
  git fetch origin
  git reset --hard "origin/main"
else
  echo "[1/5] 克隆仓库..."
  git clone -b main https://github.com/SpreMars/SkillsFinder.git .
fi

# 2) 构建后端 (使用 Maven)
echo "[2/5] 构建后端..."
cd "$PROJECT_ROOT/backend"
mvn clean package -DskipTests

# 3) 构建前端
echo "[3/5] 构建前端..."
cd "$PROJECT_ROOT/frontend"
npm install
npm run build

# 4) 停止旧容器
echo "[4/5] 停止旧容器..."
cd "$PROJECT_ROOT"
docker-compose down 2>/dev/null || true

# 5) 启动服务
echo "[5/5] 启动服务..."
docker-compose up -d --build

echo "============================================================"
echo "部署完成！访问: http://${DOMAIN}"
echo "============================================================"
docker-compose ps
