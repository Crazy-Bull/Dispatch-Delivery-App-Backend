# Dispatch Delivery App — 本地项目集成与清理指南

把这个文档打印出来,演示完照着 cleanup.sh 跑一遍就能恢复到对接前的状态。

---

## 你本地现在装了什么 / 创建了什么

逐项,都是演示期间被装/创建的东西,逐一对接到它的清理方式:

| 资产 | 位置 | 体积 | 性质 |
|------|------|------|------|
| Java 21 | 你已有 | - | 不动 |
| Docker 引擎 | 你已有 | - | 不动 |
| 后端目录 | `~/projects/demo/Dispatch-Delivery-App-Backend/` | ~5MB 源码 + build | 你新建的 |
| Gradle 9.5.1 | `~/.gradle/wrapper/dists/...` | ~150MB | 自动下载 |
| Spring/Maven 依赖 | `~/.gradle/caches/modules-2/` | ~300MB | 自动下载 |
| Postgres+PostGIS+pgRouting 镜像 | `pgrouting/pgrouting:16-3.5-4.0` | ~500MB | docker 自动拉 |
| 运行容器 | `dispatch-delivery-app-backend-db-1` | - | docker |
| 数据卷 | `dispatch-delivery-app-backend_dispatchdelivery-pg-local` | 几十 MB | docker volume |
| 种子数据 (表 + 9 商品 + 3 站点 + 6 无人机 + 4 用户) | 上面 volume 内 | - | 跟着 volume 删除 |
| osm2pgsql 二进制 | **不装** (走 dev profile 跳过) | 0 | - |
| SF 地图 PBF 文件 | **不下载** (dev profile 不需要) | 0 | - |
| 前端 `.env` | `Dispatch-Delivery-App-Frontend/.env` | 几字节 | 你新建的 |
| 前端 `node_modules` | `Dispatch-Delivery-App-Frontend/node_modules/` | ~200MB | 自动安装,先已存在 |
| `mynginx` 容器 | (我们为腾出 8080 停掉的,不影响) | 镜像已有 | 不动 |

---

## 一键清理(推荐)

```bash
# 1. 先停前后端
pkill -f 'DispatchDeliveryAppApplication' 2>/dev/null
pkill -f 'gradle.*gradle-wrapper.jar bootRun' 2>/dev/null
pkill -f 'vite' 2>/dev/null

# 2. 进后端目录,down 容器 + 删数据卷
cd ~/projects/demo/Dispatch-Delivery-App-Backend
/usr/bin/docker compose down -v
/usr/bin/docker volume rm dispatch-delivery-app-backend_dispatchdelivery-pg-local 2>/dev/null

# 3. 删后端目录
cd ~/projects/demo
rm -rf Dispatch-Delivery-App-Backend

# 4. 删前端 .env(若想完全移除 .env)
cd Dispatch-Delivery-App-Frontend
rm -f .env

# 5. 清 Gradle 缓存(可选,占 ~500MB)
rm -rf ~/.gradle

# 6. Java / Docker 是你本来就有的,不动
```

如果嫌 `rm -rf ~/.gradle` 激进,跳过它也行 — Gradle 在别处也能用。

---

## 一键重启(演示再开,通常 5 分钟搞定)

```bash
# 1. 起数据库
cd ~/projects/demo/Dispatch-Delivery-App-Backend
/usr/bin/docker compose up -d db
sleep 6

# 2. 首次启动灌种子(只第一次,之后改 INIT_DB=never 让数据留着)
SPRING_PROFILES_ACTIVE=dev INIT_DB=always ./gradlew bootRun

# 3. 另开终端启前端
cd ~/projects/demo/Dispatch-Delivery-App-Frontend
npx vite
```

打开 http://localhost:5173,种子账号 `alice.chen@example.com / password123`。

---

## 后端改动摘要(我对 Crazy-Bull/... repo 打了哪些 patch)

| 文件 | 改动 |
|------|------|
| `src/main/java/com/laioffer/dispatchdeliveryapp/config/MapDataInitializer.java` | 加 `@Profile("!dev")`,让 dev 跳过 osm2pgsql 下载 |
| `src/main/resources/application-dev.yaml` | 新建 profile:`mock-delivery.tick-interval-ms: 2000` 让 demo 时无人机动得快;`INIT_DB` 默认 never 不重复灌种子 |

源码改动只这两处。如需提 PR 给后端,直接这两个 patch 即可。

---

## 前端改动摘要(我对 Dispatch-Delivery-App-Frontend 的修改)

新增 11 个 / 修改 12 个 / 删除 2 个,详见:
- 接入文档见项目内 `src/api/` 和 `src/data/presets.js`
- 鉴权状态: `src/store/useAuthStore.js`
- 路由守卫: `src/components/RequireAuth.jsx`
- vite proxy 配置: 根目录 `vite.config.js`
