#!/bin/bash
set -e

echo "================= SkillsFinder 一键部署 ================="

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
DOMAIN="${DOMAIN:-marslander.cloud}"

cd "$PROJECT_ROOT"

# 1) 拉取最新代码 (如果网络通)
if [ -d ".git" ]; then
  echo "[1/6] 更新代码..."
  git fetch origin
  git reset --hard "origin/main" 2>/dev/null || echo "网络不通，跳过代码更新"
fi

# 2) 安装 Java 17
if ! command -v java &> /dev/null || ! java -version 2>&1 | grep -q "17"; then
  echo "[2/6] 安装 Java 17..."
  if command -v yum &> /dev/null; then
    yum install -y java-17-openjdk java-17-openjdk-devel
  elif command -v apt-get &> /dev/null; then
    apt-get update && apt-get install -y openjdk-17-jdk
  fi
fi

# 3) 安装 Maven 3.9
if ! command -v mvn &> /dev/null; then
  echo "[3/6] 安装 Maven..."
  if command -v yum &> /dev/null; then
    cd /tmp
    wget -q https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
    tar -xzf apache-maven-3.9.9-bin.tar.gz -C /opt/
    ln -s /opt/apache-maven-3.9.9/bin/mvn /usr/bin/mvn
  fi
fi

# 设置 Maven PATH
export PATH="/opt/apache-maven-3.9.9/bin:$PATH"

# 4) 构建后端
echo "[4/6] 构建后端..."
cd "$PROJECT_ROOT/backend"
/opt/apache-maven-3.9.9/bin/mvn clean package -DskipTests

# 5) 安装 Nginx
if ! command -v nginx &> /dev/null; then
  echo "[5/6] 安装 Nginx..."
  if command -v yum &> /dev/null; then
    yum install -y nginx
  fi
fi

# 6) 启动服务
echo "[6/6] 启动服务..."

# 停止旧进程
pkill -f "java.*backend" 2>/dev/null || true
pkill nginx 2>/dev/null || true

# 启动后端
cd "$PROJECT_ROOT/backend/target"
nohup java -jar backend-1.0.0.jar > backend.log 2>&1 &
sleep 10

# 配置并启动 Nginx
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
