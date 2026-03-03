#!/bin/bash
set -e

echo "================= SkillsFinder 一键部署 ================="

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
DOMAIN="${DOMAIN:-marslander.cloud}"

cd "$PROJECT_ROOT"

# 1) 拉取最新代码
if [ -d ".git" ]; then
  echo "[1/4] 更新代码..."
  git fetch origin
  git reset --hard "origin/main" 2>/dev/null || echo "跳过代码更新"
fi

# 2) 安装 Java 17 (使用国内镜像)
if ! command -v java &> /dev/null || ! java -version 2>&1 | grep -q "17"; then
  echo "[2/4] 安装 Java 17..."
  cd /tmp
  wget -q https://mirrors.tuna.tsinghua.edu.cn/github-release/adoptium/temurin17-bionic/JDK17.0.10%2B9/jdk-17.0.10+9_linux-x64_bin.tar.gz || \
  wget -q https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10%2B9/jdk-17.0.10+9_linux-x64_bin.tar.gz
  tar -xzf jdk-17.0.10+9_linux-x64_bin.tar.gz -C /opt/ 2>/dev/null || true
  if [ -d /opt/jdk-17.0.10+9 ]; then
    export JAVA_HOME=/opt/jdk-17.0.10+9
    export PATH=$JAVA_HOME/bin:$PATH
    ln -sf $JAVA_HOME/bin/java /usr/bin/java
  fi
fi

# 设置 Java 环境
if [ -d /opt/jdk-17.0.10+9 ]; then
  export JAVA_HOME=/opt/jdk-17.0.10+9
  export PATH=$JAVA_HOME/bin:$PATH
fi

# 3) 安装 Nginx
if ! command -v nginx &> /dev/null; then
  echo "[3/4] 安装 Nginx..."
  yum install -y nginx
fi

# 4) 启动服务
echo "[4/4] 启动服务..."

# 停止旧进程
pkill -f "java.*backend" 2>/dev/null || true
pkill nginx 2>/dev/null || true

# 检查 JAR 是否存在
if [ ! -f "$PROJECT_ROOT/backend/target/backend-1.0.0.jar" ]; then
  echo "错误: 后端 JAR 文件不存在，请先构建或手动上传"
  exit 1
fi

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
echo "日志: tail -f $PROJECT_ROOT/backend/target/backend.log"
echo "============================================================"
