# 🎯 ROADMAP COMPLETO - Rabbit Template com Topic, Fanout, Retry e DLQ

## 📌 REGRA DE NEGÓCIO DO PROJETO

**Contexto:** Sistema de processamento de pedidos com múltiplos cenários de integração RabbitMQ.

### **Fluxo de Pedidos:**
1. Cliente cria um pedido via POST `/orders`
2. Pedido é salvo no banco com status `CREATED`
3. Evento `OrderCreatedEvent` é publicado
4. Diferentes serviços consomem e processam o evento
5. Se falhar, retry automático com backoff exponencial
6. Se falhar após N tentativas, vai para DLQ
7. Idempotência garante que mesmo evento processado 2x não causa duplicação

---

## 🏗️ ARQUITETURA - 3 IMPLEMENTAÇÕES PARALELAS

### **IMPLEMENTAÇÃO 1: TOPIC EXCHANGE (Roteamento Seletivo)**

**Cenário de Negócio:**
- Um pedido criado dispara diferentes ações baseado no **tipo de evento**
- Diferentes listeners se interessam por diferentes eventos
- Exemplo: `orders.created` → PaymentService, `orders.shipped` → NotificationService

**Fluxo:**
```
OrderService.createOrder()
    ↓
OrderCreatedEventPublisher.publishToTopic()
    ↓
orders.topic.exchange (routing key: "orders.created")
    ↓
payment.queue.topic (binding: "orders.created")
    ↓
PaymentListener (consome apenas pedidos criados)
```

**Estrutura Necessária:**
- 1 TopicExchange: `orders.topic.exchange`
- 1 Queue: `payment.queue.topic`
- 1 Binding: fila vinculada à exchange com routing key `orders.created`
- 1 Listener: `PaymentTopicListener` que consome de `payment.queue.topic`
- 1 Publisher method: `publishToTopic(OrderCreatedEvent)` que envia com routing key específico

**Características:**
- Publisher define o routing key
- Listener recebe apenas mensagens que combinam com o padrão de binding
- 1 listener por fila (pode ter múltiplas filas, mas cada uma tem seu listener)

---

### **IMPLEMENTAÇÃO 2: FANOUT EXCHANGE (Broadcast)**

**Cenário de Negócio:**
- Um pedido criado precisa ser processado por **múltiplos serviços simultaneamente**
- Todos os serviços recebem a **mesma mensagem**
- Exemplo: PaymentService, NotificationService, AnalyticsService - todos recebem `OrderCreatedEvent`

**Fluxo:**
```
OrderService.createOrder()
    ↓
OrderCreatedEventPublisher.publishToFanout()
    ↓
orders.fanout.exchange (ignora routing key)
    ↓
├─ payment.queue.fanout → PaymentListener
├─ notification.queue.fanout → NotificationListener
└─ analytics.queue.fanout → AnalyticsListener
```

**Estrutura Necessária:**
- 1 FanoutExchange: `orders.fanout.exchange`
- 3 Queues: `payment.queue.fanout`, `notification.queue.fanout`, `analytics.queue.fanout`
- 3 Bindings: cada fila vinculada à exchange (sem routing key)
- 3 Listeners: `PaymentFanoutListener`, `NotificationFanoutListener`, `AnalyticsFanoutListener`
- 1 Publisher method: `publishToFanout(OrderCreatedEvent)` que envia sem routing key

**Características:**
- Publisher não define routing key (ou deixa vazio)
- Todos os listeners recebem a mesma mensagem
- Múltiplos listeners processam em paralelo
- Cada listener tem sua própria fila

---

### **IMPLEMENTAÇÃO 3: RETRY COM EXPONENTIAL BACKOFF**

**Cenário de Negócio:**
- Se um listener falhar (ex: serviço de pagamento indisponível), não perder a mensagem
- Tentar novamente com delay crescente (1s, 2s, 4s, 8s...)
- Após N tentativas, enviar para DLQ

**Estrutura Necessária:**

**Para TOPIC:**
- 1 TopicExchange: `orders.topic.exchange` (já existe)
- 1 Queue Principal: `payment.queue.topic`
- 1 Queue de Retry: `payment.queue.topic.retry`
- 1 TopicExchange de Retry: `orders.topic.retry.exchange`
- 1 Binding: `payment.queue.topic.retry` → `orders.topic.retry.exchange` com routing key `orders.created`

**Para FANOUT:**
- 1 FanoutExchange: `orders.fanout.exchange` (já existe)
- 1 Queue Principal: `payment.queue.fanout`
- 1 Queue de Retry: `payment.queue.fanout.retry`
- 1 FanoutExchange de Retry: `orders.fanout.retry.exchange`
- 1 Binding: `payment.queue.fanout.retry` → `orders.fanout.retry.exchange`

**Configuração de Retry:**
- Max attempts: 3
- Initial delay: 1000ms
- Multiplier: 2 (exponential backoff)
- Max delay: 10000ms
- Retry interval: configurado no `application.yaml`

**Fluxo de Retry:**
```
payment.queue.topic
    ↓
PaymentListener processa
    ↓
❌ Falha (Exception)
    ↓
Aguarda 1s (delay inicial)
    ↓
payment.queue.topic.retry (requeue)
    ↓
PaymentListener tenta novamente
    ↓
❌ Falha novamente
    ↓
Aguarda 2s (exponential backoff)
    ↓
payment.queue.topic.retry (requeue)
    ↓
... (até 3 tentativas)
    ↓
❌ Falha na 3ª tentativa
    ↓
Vai para DLQ
```

---

### **IMPLEMENTAÇÃO 4: DEAD LETTER QUEUE (DLQ)**

**Cenário de Negócio:**
- Mensagens que falharam após todas as tentativas de retry
- Armazenar para análise posterior
- Permitir reprocessamento manual

**Estrutura Necessária:**

**Para TOPIC:**
- 1 Queue DLQ: `payment.queue.topic.dlq`
- 1 TopicExchange DLQ: `orders.topic.dlq.exchange`
- 1 Binding: `payment.queue.topic.dlq` → `orders.topic.dlq.exchange` com routing key `orders.created.dlq`
- 1 Listener: `PaymentDLQListener` que consome de `payment.queue.topic.dlq`

**Para FANOUT:**
- 1 Queue DLQ: `payment.queue.fanout.dlq`
- 1 FanoutExchange DLQ: `orders.fanout.dlq.exchange`
- 1 Binding: `payment.queue.fanout.dlq` → `orders.fanout.dlq.exchange`
- 1 Listener: `PaymentDLQListener` que consome de `payment.queue.fanout.dlq`

**Configuração de DLQ:**
- Quando uma mensagem falha após max attempts, é enviada para DLQ
- DLQ armazena a mensagem com metadados (tentativas, erro, timestamp)
- Listener de DLQ registra em log e pode alertar admin

**Fluxo de DLQ:**
```
payment.queue.topic (falha 3x)
    ↓
payment.queue.topic.retry (falha 3x)
    ↓
❌ Max attempts atingido
    ↓
payment.queue.topic.dlq (Dead Letter Queue)
    ↓
PaymentDLQListener (registra erro, alerta)
    ↓
Admin pode reprocessar manualmente
```

---

### **IMPLEMENTAÇÃO 5: IDEMPOTÊNCIA**

**Cenário de Negócio:**
- Mesma mensagem pode chegar 2x (rede instável, redelivery)
- Não pode cobrar cliente 2x, não pode criar 2 pagamentos
- Usar `eventId` como chave única para detectar duplicatas

**Estrutura Necessária:**

**Tabela de Controle:**
- Criar tabela `processed_events` com colunas:
  - `event_id` (UUID, primary key)
  - `listener_name` (String: "PaymentListener", "NotificationListener")
  - `processed_at` (LocalDateTime)
  - `status` (String: "SUCCESS", "FAILED")

**Fluxo de Idempotência:**
```
PaymentListener recebe OrderCreatedEvent
    ↓
Extrai eventId da mensagem
    ↓
Consulta tabela processed_events
    ↓
eventId já existe?
    ├─ SIM: Log "Already processed" e retorna (ignora)
    └─ NÃO: Continua processamento
    ↓
Processa pagamento
    ↓
Insere eventId na tabela processed_events com status SUCCESS
    ↓
Confirma consumo da mensagem (ACK)
```

**Implementação:**
- 1 Entity: `ProcessedEvent` (mapeada para tabela `processed_events`)
- 1 Repository: `ProcessedEventRepository` com método `findByEventIdAndListenerName()`
- 1 Service: `IdempotencyService` com método `isAlreadyProcessed()` e `markAsProcessed()`
- Cada listener chama `IdempotencyService.isAlreadyProcessed()` no início
- Se já processado, listener retorna sem fazer nada
- Se não processado, listener processa e chama `IdempotencyService.markAsProcessed()`

---

## 📊 RESUMO DO ROADMAP

| Fase | O Quê | Onde | Por Quê |
|------|-------|------|--------|
| **1** | Criar constantes para Topic | `RabbitConstants` | Organizar nomes de exchanges/queues |
| **2** | Criar TopicExchange + Queue + Binding | `RabbitMQConfig` | Infraestrutura Topic |
| **3** | Criar método `publishToTopic()` | `OrderCreatedEventPublisher` | Publicar com routing key |
| **4** | Criar `PaymentTopicListener` | `listener/PaymentTopicListener.java` | Consumir eventos Topic |
| **5** | Criar constantes para Fanout | `RabbitConstants` | Organizar nomes |
| **6** | Criar FanoutExchange + Queues + Bindings | `RabbitMQConfig` | Infraestrutura Fanout |
| **7** | Criar método `publishToFanout()` | `OrderCreatedEventPublisher` | Publicar sem routing key |
| **8** | Criar `PaymentFanoutListener`, `NotificationFanoutListener` | `listener/` | Consumir eventos Fanout |
| **9** | Configurar retry no `application.yaml` | `application.yaml` | Exponential backoff |
| **10** | Criar Retry Exchanges + Queues | `RabbitMQConfig` | Infraestrutura de retry |
| **11** | Configurar DLQ no `application.yaml` | `application.yaml` | Enviar falhas para DLQ |
| **12** | Criar DLQ Exchanges + Queues | `RabbitMQConfig` | Infraestrutura de DLQ |
| **13** | Criar `PaymentDLQListener` | `listener/PaymentDLQListener.java` | Processar mensagens mortas |
| **14** | Criar Entity `ProcessedEvent` | `domain/ProcessedEvent.java` | Armazenar eventos processados |
| **15** | Criar Repository `ProcessedEventRepository` | `repository/ProcessedEventRepository.java` | Consultar eventos processados |
| **16** | Criar Service `IdempotencyService` | `service/IdempotencyService.java` | Verificar duplicatas |
| **17** | Integrar idempotência nos listeners | `listener/` | Chamar `isAlreadyProcessed()` |
| **18** | Atualizar `OrderService` para usar ambos publishers | `OrderService.java` | Publicar em Topic e Fanout |

---

## 🎯 ESTRUTURA FINAL DO PROJETO

```
src/main/java/com/example/rabbit_template/
├── config/
│   └── RabbitMQConfig.java (Topic + Fanout + Retry + DLQ)
├── constants/
│   └── RabbitConstants.java (todas as constantes)
├── controller/
│   └── OrderController.java (já existe)
├── domain/
│   ├── Order.java (já existe)
│   ├── OrderItem.java (já existe)
│   └── ProcessedEvent.java (NOVO - para idempotência)
├── dto/
│   ├── OrderRequest.java (já existe)
│   ├── OrderResponse.java (já existe)
│   └── OrderItemRequest.java (já existe)
├── event/
│   └── OrderCreatedEvent.java (já existe)
├── exception/
│   └── (já existe)
├── listener/
│   ├── PaymentTopicListener.java (NOVO - Topic)
│   ├── PaymentFanoutListener.java (NOVO - Fanout)
│   ├── NotificationFanoutListener.java (NOVO - Fanout)
│   ├── PaymentDLQListener.java (NOVO - DLQ)
│   └── NotificationDLQListener.java (NOVO - DLQ)
├── mapper/
│   └── OrderMapper.java (já existe)
├── publisher/
│   └── OrderCreatedEventPublisher.java (adicionar publishToTopic + publishToFanout)
├── repository/
│   ├── OrderRepository.java (já existe)
│   └── ProcessedEventRepository.java (NOVO - para idempotência)
├── service/
│   ├── OrderService.java (atualizar para usar ambos publishers)
│   └── IdempotencyService.java (NOVO - verificar duplicatas)
└── utils/
    └── JsonUtils.java (já existe)
```

---

## 🔄 FLUXO COMPLETO DE UM PEDIDO

```
1. POST /orders (OrderRequest)
   ↓
2. OrderService.createOrder()
   ├─ Salva Order no banco
   ├─ Cria OrderCreatedEvent
   ├─ Publica em Topic (publishToTopic)
   └─ Publica em Fanout (publishToFanout)
   ↓
3. TOPIC FLOW:
   ├─ orders.topic.exchange (routing key: "orders.created")
   ├─ payment.queue.topic
   ├─ PaymentTopicListener
   ├─ IdempotencyService.isAlreadyProcessed() → NÃO
   ├─ Processa pagamento
   ├─ IdempotencyService.markAsProcessed()
   └─ ACK (confirma consumo)
   ↓
4. FANOUT FLOW (paralelo):
   ├─ orders.fanout.exchange
   ├─ payment.queue.fanout → PaymentFanoutListener
   ├─ notification.queue.fanout → NotificationFanoutListener
   ├─ analytics.queue.fanout → AnalyticsFanoutListener
   └─ Todos processam em paralelo
   ↓
5. SE FALHAR (em qualquer listener):
   ├─ Retry 1: aguarda 1s, tenta novamente
   ├─ Retry 2: aguarda 2s, tenta novamente
   ├─ Retry 3: aguarda 4s, tenta novamente
   └─ Falha definitiva → vai para DLQ
   ↓
6. DLQ FLOW:
   ├─ payment.queue.topic.dlq
   ├─ PaymentDLQListener
   ├─ Registra erro em log
   ├─ Alerta admin
   └─ Aguarda reprocessamento manual
```

---

## ✅ PRONTO PARA IMPLEMENTAR!

Agora você tem:
- ✅ Regra de negócio clara
- ✅ 5 implementações paralelas (Topic, Fanout, Retry, DLQ, Idempotência)
- ✅ Estrutura de arquivos definida
- ✅ Fluxo completo mapeado
- ✅ 18 fases de implementação ordenadas
