#!/bin/bash
# CommerceHub 启动脚本（thin jar 模式）
# 用法: DEPLOY_PATH=/opt/commercehub bash start.sh

set -e

DEPLOY_PATH="${DEPLOY_PATH:-/opt/commercehub}"
cd "$DEPLOY_PATH"

echo "=== 停止旧进程 ==="
if [ -f commercehub.pid ]; then
  OLD_PID=$(cat commercehub.pid)
  if kill -0 "$OLD_PID" 2>/dev/null; then
    echo "killing old pid=$OLD_PID"
    kill "$OLD_PID"
    sleep 3
  fi
  rm -f commercehub.pid
fi

echo "=== 启动新进程 ==="
nohup java -Xms256m -Xmx512m \
  -cp "commercehub.jar:lib/*" \
  com.example.commerce.CommerceApplication \
  > commercehub.log 2>&1 &
echo $! > commercehub.pid
echo "started, pid=$(cat commercehub.pid)"

sleep 3
echo "=== 启动日志(最后20行) ==="
tail -20 commercehub.log
