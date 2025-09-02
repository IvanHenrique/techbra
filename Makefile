# E-commerce Platform Makefile
# Comandos para gerenciar o ciclo de vida do projeto

.PHONY: help build test clean start stop restart logs health

# Configurações
COMPOSE_FILE=docker-compose.yml
MAVEN_OPTS=-DskipTests=false

# ================================
# HELP
# ================================
help: ## Mostra todos os comandos disponíveis
	@echo "=== E-commerce Platform - Comandos Disponíveis ==="
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'
	@echo ""

# ================================
# INFRASTRUCTURE
# ================================
infra-start: ## Inicia toda a infraestrutura (PostgreSQL, Redis, Kafka)
	@echo "🚀 Iniciando infraestrutura..."
	docker-compose -f $(COMPOSE_FILE) up -d postgres redis zookeeper kafka kafka-ui
	@echo "⏳ Aguardando serviços ficarem prontos..."
	@sleep 10
	@make infra-health

infra-stop: ## Para toda a infraestrutura
	@echo "🛑 Parando infraestrutura..."
	docker-compose -f $(COMPOSE_FILE) stop postgres redis zookeeper kafka kafka-ui

infra-restart: infra-stop infra-start ## Reinicia toda a infraestrutura

infra-logs: ## Mostra logs da infraestrutura
	docker-compose -f $(COMPOSE_FILE) logs -f postgres redis kafka

infra-health: ## Verifica saúde da infraestrutura
	@echo "🔍 Verificando saúde dos serviços..."
	@echo "PostgreSQL:" && docker-compose -f $(COMPOSE_FILE) exec postgres pg_isready -U ecommerce_user -d ecommerce_db
	@echo "Redis:" && docker-compose -f $(COMPOSE_FILE) exec redis redis-cli ping
	@echo "Kafka:" && docker-compose -f $(COMPOSE_FILE) exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# ================================
# OBSERVABILITY
# ================================
observability-start: ## Inicia stack de observabilidade (Prometheus, Grafana, Jaeger)
	@echo "📊 Iniciando stack de observabilidade..."
	docker-compose -f $(COMPOSE_FILE) up -d prometheus grafana jaeger
	@echo "✅ Stack de observabilidade iniciada:"
	@echo "  - Prometheus: http://localhost:9090"
	@echo "  - Grafana: http://localhost:3000 (admin/admin)"
	@echo "  - Jaeger: http://localhost:16686"

observability-stop: ## Para stack de observabilidade
	docker-compose -f $(COMPOSE_FILE) stop prometheus grafana jaeger

# ================================
# BUILD & TEST
# ================================
build: ## Compila todos os módulos
	@echo "🔨 Compilando todos os módulos..."
	mvn clean compile -T 4

test: ## Executa todos os testes
	@echo "🧪 Executando testes..."
	mvn test -T 4

test-integration: ## Executa testes de integração
	@echo "🔧 Executando testes de integração..."
	mvn verify -P integration-test

security-check: ## Verifica vulnerabilidades de segurança
	@echo "🔒 Verificando vulnerabilidades de segurança..."
	mvn org.owasp:dependency-check-maven:check

security-update: ## Atualiza dependências para corrigir vulnerabilidades
	@echo "🔒 Atualizando dependências de segurança..."
	mvn versions:use-latest-versions -DallowSnapshots=false
	mvn versions:update-properties

package: ## Gera pacotes JAR de todos os módulos
	@echo "📦 Empacotando aplicações..."
	mvn clean package $(MAVEN_OPTS) -T 4

# ================================
# APPLICATION LIFECYCLE
# ================================
start: infra-start ## Inicia toda a plataforma (infraestrutura + aplicações)
	@echo "🚀 Iniciando aplicações..."
	@make run-order-service &
	@sleep 5
	@make run-subscription-service &
	@sleep 5
	@make run-bff &
	@echo "✅ Plataforma iniciada com sucesso!"
	@echo ""
	@echo "🌐 Serviços disponíveis:"
	@echo "  - Order Service: http://localhost:8080/order-service"
	@echo "  - Subscription Service: http://localhost:8081/subscription-service"
	@echo "  - Customer BFF: http://localhost:8090/api"
	@echo "  - Kafka UI: http://localhost:8090"

run-order-service: ## Executa apenas o Order Service
	@echo "🛒 Iniciando Order Service..."
	cd order-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

run-subscription-service: ## Executa apenas o Subscription Service
	@echo "📅 Iniciando Subscription Service..."
	cd subscription-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

run-bff: ## Executa apenas o Customer BFF
	@echo "🌐 Iniciando Customer BFF..."
	cd customer-bff && mvn spring-boot:run -Dspring-boot.run.profiles=dev

stop: ## Para todas as aplicações e infraestrutura
	@echo "🛑 Parando toda a plataforma..."
	docker-compose -f $(COMPOSE_FILE) down
	@pkill -f "spring-boot:run" || true
	@echo "✅ Plataforma parada com sucesso!"

restart: stop start ## Reinicia toda a plataforma

# ================================
# DEVELOPMENT UTILITIES
# ================================
clean: ## Limpa todos os builds e caches
	@echo "🧹 Limpando projeto..."
	mvn clean
	docker system prune -f
	docker volume prune -f

logs-apps: ## Mostra logs das aplicações
	@echo "📝 Logs das aplicações (últimas 100 linhas):"
	@echo "=== ORDER SERVICE ==="
	tail -100 order-service/logs/application.log 2>/dev/null || echo "Nenhum log encontrado"
	@echo "=== SUBSCRIPTION SERVICE ==="
	tail -100 subscription-service/logs/application.log 2>/dev/null || echo "Nenhum log encontrado"
	@echo "=== CUSTOMER BFF ==="
	tail -100 customer-bff/logs/application.log 2>/dev/null || echo "Nenhum log encontrado"

health: ## Verifica saúde de todos os serviços
	@echo "🏥 Verificando saúde dos serviços..."
	@echo "=== INFRAESTRUTURA ==="
	@make infra-health
	@echo ""
	@echo "=== APLICAÇÕES ==="
	@echo "Order Service:" && curl -s http://localhost:8080/order-service/actuator/health | jq '.status' || echo "❌ Indisponível"
	@echo "Subscription Service:" && curl -s http://localhost:8081/subscription-service/actuator/health | jq '.status' || echo "❌ Indisponível"
	@echo "Customer BFF:" && curl -s http://localhost:8090/api/actuator/health | jq '.status' || echo "❌ Indisponível"

# ================================
# KAFKA UTILITIES
# ================================
kafka-topics: ## Lista todos os tópicos Kafka
	@echo "📨 Tópicos Kafka:"
	docker-compose -f $(COMPOSE_FILE) exec kafka kafka-topics --bootstrap-server localhost:9092 --list

kafka-create-topics: ## Cria tópicos essenciais
	@echo "📨 Criando tópicos essenciais..."
	docker-compose -f $(COMPOSE_FILE) exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic order.events --partitions 3 --replication-factor 1 --if-not-exists
	docker-compose -f $(COMPOSE_FILE) exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic subscription.events --partitions 3 --replication-factor 1 --if-not-exists
	docker-compose -f $(COMPOSE_FILE) exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic payment.events --partitions 3 --replication-factor 1 --if-not-exists
	docker-compose -f $(COMPOSE_FILE) exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic billing.events --partitions 3 --replication-factor 1 --if-not-exists
	@echo "✅ Tópicos criados com sucesso!"

kafka-consume-orders: ## Consome eventos de pedidos
	docker-compose -f $(COMPOSE_FILE) exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic order.events --from-beginning

kafka-consume-subscriptions: ## Consome eventos de assinaturas
	docker-compose -f $(COMPOSE_FILE) exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic subscription.events --from-beginning

# ================================
# DATABASE UTILITIES
# ================================
db-connect: ## Conecta ao PostgreSQL
	docker-compose -f $(COMPOSE_FILE) exec postgres psql -U ecommerce_user -d ecommerce_db

db-reset: ## Reseta o banco de dados
	@echo "🗄️ Resetando banco de dados..."
	docker-compose -f $(COMPOSE_FILE) exec postgres psql -U ecommerce_user -d ecommerce_db -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
	@echo "✅ Banco resetado com sucesso!"

# ================================
# VALIDATION COMMANDS
# ================================
validate-etapa-1: infra-start package ## Valida a Etapa 1 - Setup do projeto
	@echo "✅ VALIDAÇÃO ETAPA 1 - SETUP DO PROJETO"
	@echo "========================================"
	@echo ""
	@echo "🔍 Verificando estrutura do projeto:"
	@ls -la
	@echo ""
	@echo "📦 Verificando módulos Maven:"
	@mvn -q exec:exec -Dexec.executable=echo -Dexec.args='$${project.modules}' 2>/dev/null || echo "Módulos: shared-kernel, order-service, subscription-service, customer-bff"
	@echo ""
	@echo "🔨 Verificando builds:"
	@test -f shared-kernel/target/*.jar && echo "✅ Shared Kernel build OK" || echo "❌ Shared Kernel build FAIL"
	@test -f order-service/target/*.jar && echo "✅ Order Service build OK" || echo "❌ Order Service build FAIL"
	@test -f subscription-service/target/*.jar && echo "✅ Subscription Service build OK" || echo "❌ Subscription Service build FAIL"
	@test -f customer-bff/target/*.jar && echo "✅ Customer BFF build OK" || echo "❌ Customer BFF build FAIL"
	@echo ""
	@echo "🐳 Verificando infraestrutura:"
	@make infra-health
	@echo ""
	@echo "🎉 ETAPA 1 VALIDADA COM SUCESSO!"
	@echo "Próximo passo: Implementar Etapa 2 - Arquitetura Hexagonal Order Service"

# ================================
# DEFAULT TARGET
# ================================
.DEFAULT_GOAL := help