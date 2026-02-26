.PHONY: up lite down reset status logs

up:
	docker-compose up -d

lite:
	docker-compose -f docker-compose.yml -f docker-compose.dev-lite.yml up -d

down:
	docker-compose down

reset:
	docker-compose down -v
	docker-compose up -d

status:
	docker-compose ps

logs:
	docker-compose logs -f
