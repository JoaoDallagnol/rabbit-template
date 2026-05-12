# 🐰 Rabbit Template - RabbitMQ Event-Driven Architecture

Um projeto educacional que demonstra os principais padrões de integração com **RabbitMQ**, incluindo Topic Exchange, Fanout Exchange, Retry com Exponential Backoff, Dead Letter Queue (DLQ) e Idempotência.

## 📋 Índice

- [O que é este projeto?](#o-que-é-este-projeto)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Como Rodar](#como-rodar)
- [Endpoints da API](#endpoints-da-api)
- [Fluxos de Processamento](#fluxos-de-processamento)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Tecnologias](#tecnologias)

---

## 🎯 O que é este projeto?

Este é um **projeto de estudos** que implementa um sistema de processamento de pedidos com diferentes cenários de integração RabbitMQ:

### **Cenários Implementados:**

1. **Topic Exchange** - Roteamento seletivo baseado em routing keys
2. **Fanout Exchange** - Broadcast para múltiplos listeners
3. **Retry com Exponential Backoff** - Tentativas automáticas com delay crescente
4. **Dead Letter Queue (DLQ)** - Armazenamento de mensagens que falharam
5. **Idempotência** - Prevenção de processamento duplicado

### **Caso de Uso:**

Um cliente cria um pedido via API. O sistema publica um evento que é consumido por diferentes serviços (pagamento, notificação, etc.) de forma assíncrona. Se algo falhar, o sistema tenta novamente automaticamente. Se falhar definitivamente, a mensagem vai para uma fila de mensagens mortas para análise.

---

## 🏗️ Arquitetura

### **Fluxo Topic Exchange (Roteamento Seletivo)**

```
POST /orders/topic
    ↓
OrderService.createOrderTopic()
    ↓
Salva Order no banco
    ↓
Publica OrderCreatedEvent
    ↓
orders.exchange (routing key: "orders.created")
    ↓
payment.queue.topic
    ↓
PaymentListener (processa pagamento)
    ↓
IdempotencyService (evita duplicação)
    ↓
ACK (confirma consumo)
```

### **Fluxo Fanout Exchange (Broadcast)**

```
POST /orders/fanout
    ↓
OrderService.createOrderFanout()
    ↓
Salva Order no banco
    ↓
Publica OrderCreatedEvent
    ↓
orders.fanout.exchange (sem routing key)
    ↓
├─ payment.queue.fanout → PaymentListener
└─ notification.queue.fanout → NotificationListener
    ↓
Ambos processam em paralelo
    ↓
IdempotencyService (cada listener tem seu registro)
    ↓
ACK (confirma consumo)
```

### **Fluxo de Retry e DLQ**

```
Listener processa mensagem
    ↓
❌ Falha
    ↓
Aguarda 1s (delay inicial)
    ↓
Tenta novamente (Retry 1)
    ↓
❌ Falha novamente
    ↓
Aguarda 2s (exponential backoff)
    ↓
Tenta novamente (Retry 2)
    ↓
❌ Falha na 3ª tentativa
    ↓
Vai para DLQ (Dead Letter Queue)
    ↓
DLQListener (registra erro para análise)
```

---

## 📦 Pré-requisitos

- **Java 21+**
- **Maven 3.8+**
- **RabbitMQ 3.x** (com Management Plugin)
- **Docker** (opcional, para rodar RabbitMQ)

---

## 🚀 Como Rodar

### **1. Iniciar RabbitMQ com Docker**

Use o docker-compose fornecido:

```bash
version: "3.8"
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: rabbitmq
    restart: no
    ports:
      - "5672:5672"    # Default
      - "15672:15672"  # Web Admin
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin123
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    networks:
      - microservices_net

volumes:
  rabbitmq_data:

networks:
  microservices_net:
    driver: bridge
```

**Salve como `docker-compose.yml` e execute:**

```bash
docker-compose up -d
```

**Verificar se está rodando:**

```bash
docker ps | grep rabbitmq
```

**Acessar Management Console:**

- URL: `http://localhost:15672`
- Usuário: `admin`
- Senha: `admin123`

---

### **2. Compilar o Projeto**

```bash
mvn clean install
```

---

### **3. Rodar a Aplicação**

```bash
mvn spring-boot:run
```

Ou execute a classe principal:

```bash
java -jar target/rabbit-template-0.0.1-SNAPSHOT.jar
```

**Verificar se está rodando:**

```bash
curl http://localhost:8080/orders
```

---

## 📡 Endpoints da API

### **1. Criar Pedido via Topic Exchange**

```bash
POST http://localhost:8080/orders/topic
Content-Type: application/json

{
  "customerId": "CUST-001",
  "amount": 150.50,
  "items": [
    {
      "productId": "PROD-001",
      "quantity": 2
    }
  ]
}
```

**Resposta (201 Created):**

```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-001",
  "amount": 150.50,
  "status": "CREATED",
  "createdAt": "2026-05-12T15:30:00"
}
```

---

### **2. Criar Pedido via Fanout Exchange**

```bash
POST http://localhost:8080/orders/fanout
Content-Type: application/json

{
  "customerId": "CUST-002",
  "amount": 250.00,
  "items": [
    {
      "productId": "PROD-002",
      "quantity": 1
    }
  ]
}
```

**Resposta (201 Created):**

```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440001",
  "customerId": "CUST-002",
  "amount": 250.00,
  "status": "CREATED",
  "createdAt": "2026-05-12T15:35:00"
}
```

---

### **3. Listar Todos os Pedidos**

```bash
GET http://localhost:8080/orders
```

**Resposta (200 OK):**

```json
[
  {
    "orderId": "550e8400-e29b-41d4-a716-446655440000",
    "customerId": "CUST-001",
    "amount": 150.50,
    "status": "CREATED",
    "createdAt": "2026-05-12T15:30:00"
  },
  {
    "orderId": "550e8400-e29b-41d4-a716-446655440001",
    "customerId": "CUST-002",
    "amount": 250.00,
    "status": "CREATED",
    "createdAt": "2026-05-12T15:35:00"
  }
]
```

---

### **4. Obter Pedido por ID**

```bash
GET http://localhost:8080/orders/550e8400-e29b-41d4-a716-446655440000
```

**Resposta (200 OK):**

```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-001",
  "amount": 150.50,
  "status": "CREATED",
  "createdAt": "2026-05-12T15:30:00"
}
```

---

## 🔄 Fluxos de Processamento

### **Topic Exchange - PaymentListener**

1. Recebe evento via `payment.queue.topic`
2. Verifica idempotência (já foi processado?)
3. Se não: processa pagamento e marca como COMPLETED
4. Se sim: ignora (evita duplicação)
5. Se falhar: tenta novamente (retry com backoff)
6. Se falhar 3x: vai para DLQ

### **Fanout Exchange - PaymentListener + NotificationListener**

1. Ambos recebem o mesmo evento simultaneamente
2. Cada um tem seu próprio registro de idempotência
3. PaymentListener processa pagamento
4. NotificationListener envia notificação
5. Se um falhar, o outro continua (independentes)
6. Cada um tem seu próprio retry e DLQ

### **Retry com Exponential Backoff**

- **Tentativa 1:** Aguarda 1 segundo
- **Tentativa 2:** Aguarda 2 segundos
- **Tentativa 3:** Aguarda 4 segundos
- **Máximo:** 10 segundos

Configurado em `application.yaml`:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000
          multiplier: 2.0
          max-interval: 10000
```

### **Idempotência**

Cada listener tem um registro em `processed_events`:

```
eventId: 550e8400-e29b-41d4-a716-446655440000
listenerName: PaymentListener
status: SUCCESS
processedAt: 2026-05-12T15:30:05
```

Se a mesma mensagem chegar 2x, o listener verifica este registro e ignora.

---

## 📁 Estrutura do Projeto

```
src/main/java/com/example/rabbit_template/
├── config/
│   └── RabbitMQConfig.java          # Configuração de exchanges, queues, bindings
├── constants/
│   ├── RabbitConstants.java         # Nomes de exchanges, queues, routing keys
│   ├── EventType.java               # Tipos de eventos
│   └── Status.java                  # Status de pedidos e processamento
├── controller/
│   └── OrderController.java         # Endpoints REST
├── domain/
│   ├── Order.java                   # Entidade de pedido
│   ├── OrderItem.java               # Itens do pedido
│   └── ProcessedEvent.java          # Rastreamento de idempotência
├── dto/
│   ├── OrderRequest.java            # DTO de entrada
│   ├── OrderResponse.java           # DTO de saída
│   └── OrderItemRequest.java        # DTO de item
├── event/
│   └── OrderCreatedEvent.java       # Evento publicado no RabbitMQ
├── exception/
│   ├── GlobalExceptionHandler.java  # Tratamento de exceções
│   └── OrderNotFoundException.java   # Exceção customizada
├── listener/
│   └── PaymentListener.java         # Consome eventos (Topic + Fanout)
│   └── NotificationListener.java    # Consome eventos (Fanout + DLQ)
├── mapper/
│   └── OrderMapper.java             # Conversão entre entidades e DTOs
├── publisher/
│   └── OrderCreatedEventPublisher.java  # Publica eventos no RabbitMQ
├── repository/
│   ├── OrderRepository.java         # Acesso a dados de pedidos
│   └── ProcessedEventRepository.java # Acesso a dados de idempotência
├── scheduler/
│   └── QueueHealthMonitor.java      # Monitora saúde das filas
├── service/
│   ├── OrderService.java            # Lógica de negócio
│   └── IdempotencyService.java      # Gerencia idempotência
└── utils/
    └── JsonUtils.java               # Utilitários de JSON

src/main/resources/
└── application.yaml                 # Configurações da aplicação
```

---

## 🛠️ Tecnologias

| Tecnologia | Versão | Propósito |
|-----------|--------|----------|
| Java | 21+ | Linguagem |
| Spring Boot | 3.x | Framework |
| Spring AMQP | 3.x | Integração RabbitMQ |
| RabbitMQ | 3.x | Message Broker |
| H2 Database | 2.x | Banco de dados em memória |
| Hibernate | 7.x | ORM |
| MapStruct | 1.x | Mapeamento de objetos |
| Lombok | 1.x | Redução de boilerplate |
| Maven | 3.8+ | Build tool |

---

## 📚 Conceitos Aprendidos

### **Topic Exchange**
- Roteamento baseado em padrões de routing key
- Ideal para diferentes tipos de eventos
- Um listener por fila

### **Fanout Exchange**
- Broadcast para todos os listeners
- Sem roteamento (todos recebem)
- Múltiplos listeners processam em paralelo

### **Retry com Exponential Backoff**
- Tentativas automáticas com delay crescente
- Evita sobrecarregar o sistema
- Configurável via `application.yaml`

### **Dead Letter Queue (DLQ)**
- Armazena mensagens que falharam permanentemente
- Permite análise e reprocessamento manual
- Forma 1: Retry volta para exchange original
- Forma 2: Retry vai para DLQ específica

### **Idempotência**
- Cada listener tem seu próprio registro
- Evita processamento duplicado
- Suporta múltiplos listeners no Fanout
- Permite reprocessamento seletivo

---

## 🧪 Testando com Postman

Importe a collection fornecida: `rabbit-template.postman_collection.json`

Ou use os comandos curl:

```bash
# Topic
curl -X POST http://localhost:8080/orders/topic \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","amount":150.50,"items":[{"productId":"PROD-001","quantity":2}]}'

# Fanout
curl -X POST http://localhost:8080/orders/fanout \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-002","amount":250.00,"items":[{"productId":"PROD-002","quantity":1}]}'

# List
curl http://localhost:8080/orders

# Get by ID
curl http://localhost:8080/orders/550e8400-e29b-41d4-a716-446655440000
```

---

## 📊 Monitorando RabbitMQ

Acesse o Management Console: `http://localhost:15672`

Você pode visualizar:
- Exchanges criadas
- Queues e suas mensagens
- Bindings
- Conexões ativas
- Taxa de mensagens