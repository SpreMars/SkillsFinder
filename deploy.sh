#!/usr/bin/env bash
set -euo pipefail

echo "================= SkillsFinder 一键部署 ================="

PROJECT_ROOT="${PROJECT_ROOT:-/root/SkillsFinder}"
DOMAIN="${DOMAIN:-marslander.cloud}"

cd "$PROJECT_ROOT"

# 1) 拉取最新代码
if [ -d ".git" ]; then
  echo "[1/3] 更新代码..."
  git fetch origin
  git reset --hard "origin/main"
else
  echo "[1/3] 克隆仓库..."
  git clone -b main https://github.com/SpreMars/SkillsFinder.git .
fi

# 2) 删除旧容器和数据，重新部署
echo "[2/3] 停止旧容器..."
docker-compose down -v 2>/dev/null || true

echo "[3/3] 构建并启动服务..."
docker-compose up -d --build

# 等待启动
sleep 30

echo "============================================================"
echo "部署完成！访问: http://${DOMAIN}"
echo "============================================================"
docker-compose ps
