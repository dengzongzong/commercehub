#!/bin/bash
# CommerceHub 服务器端部署脚本（自编译模式）
# 由 GitHub Actions 通过 SSH 触发执行：git pull → mvn 编译 → 停旧 → 启动
#
# 前置条件（服务器首次准备，只做一次）：
#   1. 装 JDK 17 + Maven + Git
#        apt update && apt install -y openjdk-17-jdk maven git
#   2. clone 仓库到部署目录
#        git clone https://github.com/dengzongzong/commercehub.git /opt/commercehub
#   3. 配置第三方密钥环境变量到 /etc/commercehub.env（见 SETUP.md）
#
# 用法: DEPLOY_PATH=/opt/commercehub bash scripts/deploy.sh

set -e

DEPLOY_PATH="${DEPLOY_PATH:-/opt/commercehub}"
BRANCH="${BRANCH:-main}"
cd "$DEPLOY_PATH"

echo "=== [1/4] 拉取最新代码 (branch=$BRANCH) ==="
git fetch --all --prune
# 强制同步到远端最新，避免本地改动/中间文件导致 pull 冲突
git reset --hard "origin/$BRANCH"
git log -1 --pretty=format:"%h %s (%an %ad)%n"

echo ""
echo "=== [2/4] 编译构建 (mvn clean package) ==="
# -B 批量模式无进度条；-DskipTests 跳过测试加速；首次会下载依赖，较慢
mvn -B clean package -DskipTests -q

echo "构建产物体积："
du -h target/commercehub-exec.jar | cut -f1 | xargs echo "  exec jar:"

echo "=== [3/4] 停止旧进程 ==="
if [ -f commercehub.pid ]; then
  OLD_PID=$(cat commercehub.pid)
  if kill -0 "$OLD_PID" 2>/dev/null; then
    echo "killing old pid=$OLD_PID"
    kill "$OLD_PID"
    # 等待进程退出，最多 10 秒
    for i in $(seq 1 10); do
      kill -0 "$OLD_PID" 2>/dev/null || break
      sleep 1
    done
    # 还没退出就强杀
    kill -0 "$OLD_PID" 2>/dev/null && kill -9 "$OLD_PID" || true
  fi
  rm -f commercehub.pid
fi

echo "=== [4/4] 启动新进程 ==="
# 加载第三方密钥等环境变量（MYSQL_HOST / ALIPAY_APP_ID ...）
if [ -f /etc/commercehub.env ]; then
  set -a
  . /etc/commercehub.env
  set +a
fi

# 用 exec jar 启动（自带依赖，服务器自编译模式下最省心，无需维护 lib 目录）
nohup java -Xms256m -Xmx512m \
  -jar target/commercehub-exec.jar \
  > commercehub.log 2>&1 &
echo $! > commercehub.pid
echo "started, pid=$(cat commercehub.pid)"

sleep 5
echo ""
echo "=== 启动日志(最后30行) ==="
tail -30 commercehub.log
