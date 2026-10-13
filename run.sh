#!/usr/bin/env bash
# Chạy backend (Spring Boot :8080) và frontend (Vite :5173) cùng lúc. Nhấn Ctrl+C để tắt cả hai.
# Cần MySQL và MQTT broker (cổng 1884) chạy sẵn.
cd "$(dirname "$0")" || exit 1

(cd backend && mvn spring-boot:run) &
BACK=$!
(cd frontendIOT && npm run dev) &
FRONT=$!

trap 'kill $BACK $FRONT 2>/dev/null; pkill -P $BACK 2>/dev/null; pkill -P $FRONT 2>/dev/null; exit' INT TERM
wait
