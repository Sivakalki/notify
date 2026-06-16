# ──────────────────────────────────────────────────────────────────────────────
# Notify Backend — Docker helper
#
# Assumes PostgreSQL and pgAdmin are installed locally.
# `make dev`   → starts only Kafka + Kafka-UI + Redis (daily driver)
# `make up`    → starts everything including Postgres + pgAdmin
# ──────────────────────────────────────────────────────────────────────────────

.PHONY: dev up down down-v logs kafka-logs redis-logs verify-redis ps help

# Start only the infra you don't have locally
dev:
	docker compose up -d kafka kafka-ui redis

# Start the full stack (useful for CI or a clean machine)
up:
	docker compose up -d

# Stop all running containers (keeps volumes)
down:
	docker compose down

# Stop and wipe all volumes (full reset)
down-v:
	docker compose down -v

# Tail logs for all running compose services
logs:
	docker compose logs -f

# Tail Kafka logs only
kafka-logs:
	docker compose logs -f kafka

# Tail Redis logs only
redis-logs:
	docker compose logs -f redis

# Verify that the Redis Stack image has Cuckoo Filter (CF) commands available
verify-redis:
	@echo "Starting temporary Redis Stack container..."
	@docker run -d --name notify-redis-verify redis/redis-stack-server:latest > /dev/null
	@sleep 3
	@echo ""
	@echo "=== Loaded Redis modules ==="
	@docker exec notify-redis-verify redis-cli MODULE LIST
	@echo ""
	@echo "=== CF.ADD test (should return 1) ==="
	@docker exec notify-redis-verify redis-cli CF.ADD verify-filter test-user
	@echo ""
	@echo "=== CF.EXISTS test (should return 1) ==="
	@docker exec notify-redis-verify redis-cli CF.EXISTS verify-filter test-user
	@echo ""
	@echo "=== CF.DEL test (should return 1) ==="
	@docker exec notify-redis-verify redis-cli CF.DEL verify-filter test-user
	@echo ""
	@echo "=== CF.EXISTS after delete (should return 0) ==="
	@docker exec notify-redis-verify redis-cli CF.EXISTS verify-filter test-user
	@echo ""
	@docker rm -f notify-redis-verify > /dev/null
	@echo "Verification complete. Container cleaned up."

# Show running compose services
ps:
	docker compose ps

help:
	@echo ""
	@echo "  make dev           Start Kafka + Kafka-UI + Redis (local Postgres assumed)"
	@echo "  make up            Start full stack including Postgres + pgAdmin"
	@echo "  make down          Stop containers (keep volumes)"
	@echo "  make down-v        Stop containers and delete volumes"
	@echo "  make logs          Tail all logs"
	@echo "  make kafka-logs    Tail Kafka logs"
	@echo "  make redis-logs    Tail Redis logs"
	@echo "  make verify-redis  Confirm Cuckoo Filter commands work in redis-stack-server"
	@echo "  make ps            Show running containers"
	@echo ""