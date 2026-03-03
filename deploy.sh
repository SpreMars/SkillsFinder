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

# 2) 安装 Maven (如果不存在)
if ! command -v mvn &> /dev/null; then
  echo "[2/5] 安装 Maven..."
  if command -v yum &> /dev/null; then
    yum install -y maven
  elif command -v apt-get &> /dev/null; then
    apt-get update && apt-get install -y maven
  fi
fi

# 3) 构建后端
if command -v mvn &> /dev/null; then
  echo "[3/5] 构建后端..."
  cd "$PROJECT_ROOT/backend"
  mvn clean package -DskipTests
else
  echo "[3/5] 跳过构建 (Maven 未安装)"
fi

# 4) 构建前端
if command -v npm &> /dev/null; then
  echo "[4/5] 构建前端..."
  cd "$PROJECT_ROOT/frontend"
  npm install
  npm run build
else
  echo "[4/5] 跳过构建 (npm 未安装)"
fi

# 5) 停止旧容器并启动
echo "[5/5] 启动服务..."
cd "$PROJECT_ROOT"
docker-compose down 2>/dev/null || true
docker-compose up -d --build

echo "============================================================"
echo "部署完成！访问: http://${DOMAIN}"
echo "============================================================"
docker-compose ps
