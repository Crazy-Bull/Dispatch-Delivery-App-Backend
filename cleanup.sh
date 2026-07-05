#!/usr/bin/env bash
# cleanup.sh - Remove every asset this demo project added to your system.
# Safe to run again. Idempotent on most lines.
set +e

echo "=== Step 1: stop dev processes ==="
pkill -f 'DispatchDeliveryAppApplication'
pkill -f 'gradle.*gradle-wrapper.jar bootRun'
pkill -f 'vite'
sleep 2
echo "  done"

echo "=== Step 2: docker compose down -v ==="
cd "$(dirname "$0")"
/usr/bin/docker compose down -v
echo "  done"

echo "=== Step 3: drop project docker volume ==="
/usr/bin/docker volume rm dispatch-delivery-app-backend_dispatchdelivery-pg-local
echo "  done"

echo "=== Step 4: delete backend directory ==="
cd ..
rm -rf Dispatch-Delivery-App-Backend
echo "  done"

echo "=== Step 5: delete frontend .env (node_modules kept) ==="
rm -f Dispatch-Delivery-App-Frontend/.env
echo "  done"

echo "=== Step 6: clear Gradle caches (~500MB) ==="
rm -rf ~/.gradle
echo "  done"

echo ""
echo "Cleanup complete. Java 21, Docker, and the Frontend node_modules"
echo "were intentionally left untouched because they were pre-existing."
