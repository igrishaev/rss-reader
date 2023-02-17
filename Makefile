
repl:
	lein repl

migration ?= $(error Please specify the migration=... argument)
migration_id = $(shell date +'%Y%m%d%H%M%S')

create-migration:
	touch resources/migrations/${migration_id}-${migration}.up.sql
	touch resources/migrations/${migration_id}-${migration}.down.sql


PG_BASE = .docker/postgres
PG_INIT = ${PG_BASE}/initdb.d

docker-prepare:
	-rm -rf ${PG_BASE}
	mkdir -p ${PG_INIT}
	cp resources/migrations/*.up.sql ${PG_INIT}/

docker-up: docker-prepare
	docker-compose up

docker-down:
	docker-compose down

docker-rm:
	docker-compose rm --force

docker-psql:
	psql --port 15432 --host localhost -U user rss
