#!/bin/bash
set -e

echo "================= SkillsFinder 一键部署 ================="

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
DOMAIN="${DOMAIN:-marslander.cloud}"

cd "$PROJECT_ROOT"

# 设置 Java 环境
if [ -d /opt/jdk-17.0.2 ]; then
  export JAVA_HOME=/opt/jdk-17.0.2
  export PATH=$JAVA_HOME/bin:$PATH
fi

# 1) 拉取最新代码
if [ -d ".git" ]; then
  echo "[1/4] 更新代码..."
  git fetch origin
  git reset --hard "origin/main" 2>/dev/null || echo "跳过代码更新"
fi

# 2) 安装 Maven
if ! command -v mvn &> /dev/null; then
  echo "[2/4] 安装 Maven..."
  cd /tmp
  wget -q https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
  tar -xzf apache-maven-3.9.6-bin.tar.gz -C /opt/
  export PATH=/opt/apache-maven-3.9.6/bin:$PATH
fi

export PATH=/opt/apache-maven-3.9.6/bin:$PATH

# 3) 构建后端
echo "[3/4] 构建后端..."
cd "$PROJECT_ROOT/backend"
mvn clean package -DskipTests

# 4) 启动服务
echo "[4/4] 启动服务..."

# 停止旧进程
pkill -f "java.*backend" 2>/dev/null || true
pkill nginx 2>/dev/null || true

# 启动后端
cd "$PROJECT_ROOT/backend/target"
nohup java -jar backend-1.0.0.jar > backend.log 2>&1 &
sleep 10

# 配置 Nginx
cd "$PROJECT_ROOT/frontend"
rm -rf /usr/share/nginx/html
cp -r dist /usr/share/nginx/html
cp nginx.conf /etc/nginx/nginx.conf
nginx -t && nginx

echo "============================================================"
echo "部署完成！"
echo "前端: http://${DOMAIN}"
echo "后端: http://${DOMAIN}:8080"
echo "============================================================"
