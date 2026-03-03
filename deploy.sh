#!/bin/bash
set -e

echo "================= SkillsFinder 一键部署 ================="

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

# 设置 Java 环境
if [ -d /opt/jdk-17.0.2 ]; then
  export JAVA_HOME=/opt/jdk-17.0.2
  export PATH=$JAVA_HOME/bin:$PATH
fi

# 设置 Maven 环境
if [ -d /opt/apache-maven-3.9.6 ]; then
  export PATH=/opt/apache-maven-3.9.6/bin:$PATH
fi

# 1) 拉取代码
echo "[1/4] 更新代码..."
git fetch origin
git reset --hard "origin/main" 2>/dev/null || echo "跳过"

# 2) 构建后端
echo "[2/4] 构建后端..."
cd $PROJECT_ROOT/backend
mvn clean package -DskipTests

# 3) 启动后端
echo "[3/4] 启动后端..."
pkill -f "java.*backend" 2>/dev/null || true
cd $PROJECT_ROOT/backend/target
nohup java -jar backend-1.0.0.jar > backend.log 2>&1 &
sleep 10

# 4) 配置 Nginx
echo "[4/4] 配置 Nginx..."
pkill nginx 2>/dev/null || true
cd $PROJECT_ROOT/frontend
rm -rf /usr/share/nginx/html
cp -r dist /usr/share/nginx/html
cp nginx.conf /etc/nginx/nginx.conf
nginx -t && nginx

echo "============================================================"
echo "部署完成！访问: http://marslander.cloud"
echo "============================================================"
