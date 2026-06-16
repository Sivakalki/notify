# ──────────────────────────────────────────────────────────────────────────────
# Notify — Root Makefile
#
# Delegates infra to notify-backend/Makefile (Docker Compose).
# Backend and frontend run in separate terminals.
#
# Common workflows:
#   make infra      → start Kafka + Redis + Postgres (and optional UIs)
#   make backend    → run Spring Boot API (requires infra up)
#   make frontend   → run Vite dev server
#   make down       → stop all Docker containers
# ──────────────────────────────────────────────────────────────────────────────

.PHONY: infra infra-full backend frontend down down-v logs ps help

# Start core infra: Kafka + Kafka-UI + Redis (local Postgres assumed)
infra:
	$(MAKE) -C notify-backend dev

# Start full infra including Postgres + pgAdmin
infra-full:
	$(MAKE) -C notify-backend up

# Run the Spring Boot backend (assumes infra is up)
backend:
	cd notify-backend && ./gradlew bootRun

# Run the React frontend dev server
frontend:
	cd notify-frontend && npm run dev

# Stop all Docker containers (keep volumes)
down:
	$(MAKE) -C notify-backend down

# Stop all Docker containers and wipe volumes
down-v:
	$(MAKE) -C notify-backend down-v

# Tail all Docker Compose logs
logs:
	$(MAKE) -C notify-backend logs

# Show running containers
ps:
	$(MAKE) -C notify-backend ps

help:
	@echo ""
	@echo "  make infra        Start Kafka + Kafka-UI + Redis (local Postgres assumed)"
	@echo "  make infra-full   Start full stack including Postgres + pgAdmin"
	@echo "  make backend      Run Spring Boot API (in a separate terminal)"
	@echo "  make frontend     Run Vite dev server  (in a separate terminal)"
	@echo "  make down         Stop Docker containers (keep volumes)"
	@echo "  make down-v       Stop Docker containers and delete volumes"
	@echo "  make logs         Tail all compose logs"
	@echo "  make ps           Show running containers"
	@echo ""
