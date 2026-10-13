# Kubernetes com microsserviços — Alura Food

Projeto de estudo desenvolvido durante o curso de Kubernetes com microsserviços Java da [Alura](https://www.alura.com.br). O código das aplicações (Alura Food) é o material base do curso; o foco do repositório é a parte de Kubernetes: empacotar os serviços em imagens Docker e subir tudo em um cluster.

## O que foi praticado

- Build de imagens com Dockerfile multi-stage (Maven + JRE)
- `Deployment` com réplicas para os serviços sem estado
- `StatefulSet` para o Eureka Server e para o MySQL
- `Service` headless para descoberta entre pods e `LoadBalancer` para acesso externo
- `ConfigMap` e `Secret` injetados como variáveis de ambiente
- `PersistentVolumeClaim` para os dados do MySQL

## Arquitetura

```
                    ┌──────────────────────┐
  cliente ────────▶ │ gateway (8082)       │
                    │ Spring Cloud Gateway │
                    └──────────┬───────────┘
                               │ rotas via Eureka
              ┌────────────────┴────────────────┐
              ▼                                 ▼
   ┌─────────────────────┐   OpenFeign   ┌─────────────────────┐
   │ pagamentos (40000)  │ ────────────▶ │ pedidos (40001)     │
   └──────────┬──────────┘               └──────────┬──────────┘
              │                                     │
              └──────────────┬──────────────────────┘
                             ▼
                      ┌─────────────┐
                      │ MySQL 8     │
                      └─────────────┘

   server (8081): Eureka Server, 2 réplicas, registra todos os serviços
```

| Serviço | Papel | Porta | Recurso no cluster |
|---|---|---|---|
| `server` | Service discovery (Eureka Server) | 8081 | StatefulSet, 2 réplicas |
| `gateway` | API Gateway | 8082 | Deployment, 2 réplicas |
| `pedidos` | API de pedidos | 40001 | Deployment, 2 réplicas |
| `pagamentos` | API de pagamentos, com circuit breaker na chamada a `pedidos` | 40000 | Deployment, 2 réplicas |
| `mysql` | Banco de dados | 3306 | StatefulSet, 1 réplica |

## Tecnologias

Java 17, Spring Boot 2.6, Spring Cloud (Gateway, Eureka, OpenFeign), Resilience4j, Flyway, MySQL 8, Docker e Kubernetes.

## Como rodar no minikube

Pré-requisitos: [minikube](https://minikube.sigs.k8s.io) e `kubectl`.

1. Suba o cluster e habilite o driver de volumes usado pelo PVC (`csi-hostpath-sc`):

```bash
minikube start
```

```bash
minikube addons enable volumesnapshots
```

```bash
minikube addons enable csi-hostpath-driver
```

2. Crie o arquivo de Secret a partir do modelo e defina a senha do banco:

```bash
cp k8s/secrets.example.yaml k8s/secrets.yaml
```

3. Aplique os manifests:

```bash
kubectl apply -f k8s/volumes.yaml -f k8s/secrets.yaml -f k8s/configmap.yaml -f k8s/services.yaml
```

```bash
kubectl apply -f k8s/mysql.yaml -f k8s/app.yaml -f k8s/loadbalancer.yaml
```

4. Acompanhe os pods até ficarem `Running`:

```bash
kubectl get pods -w
```

5. Exponha os LoadBalancers (deixe rodando em outro terminal):

```bash
minikube tunnel
```

Com o túnel ativo, o painel do Eureka fica em `http://localhost:8081` e o gateway em `http://localhost:8082`.

## Endpoints

As rotas do gateway seguem o nome do serviço registrado no Eureka.

| Método | Rota | Descrição |
|---|---|---|
| GET | `/pedidos-ms/pedidos` | Lista os pedidos |
| GET | `/pedidos-ms/pedidos/{id}` | Detalha um pedido |
| POST | `/pedidos-ms/pedidos` | Cria um pedido |
| PUT | `/pedidos-ms/pedidos/{id}/status` | Atualiza o status |
| PUT | `/pedidos-ms/pedidos/{id}/pago` | Marca o pedido como pago |
| GET | `/pagamentos-ms/pagamentos` | Lista os pagamentos (paginado) |
| GET | `/pagamentos-ms/pagamentos/{id}` | Detalha um pagamento |
| POST | `/pagamentos-ms/pagamentos` | Cria um pagamento |
| PUT | `/pagamentos-ms/pagamentos/{id}` | Atualiza um pagamento |
| DELETE | `/pagamentos-ms/pagamentos/{id}` | Remove um pagamento |
| PATCH | `/pagamentos-ms/pagamentos/{id}/confirmar` | Confirma o pagamento e avisa o serviço de pedidos |

## Imagens Docker

As imagens usadas nos manifests foram construídas a partir dos Dockerfiles deste repositório e estão no Docker Hub:

- `kaauandi/alurafood-server:v1`
- `kaauandi/alurafood-gateway:v1`
- `kaauandi/alurafood-pedidos:v1`
- `kaauandi/alurafood-pagamentos:v1`

Para gerar uma imagem localmente:

```bash
docker build -t alurafood-pedidos pedidos
```

## O que mudei em relação ao material do curso

- Corrigi o `containerPort` do `gateway` e do `pagamentos`, que não batiam com a porta real das aplicações
- Publiquei as imagens na minha conta do Docker Hub, no lugar das imagens do curso
- Tirei o `Secret` com a senha do banco do versionamento e deixei um modelo (`secrets.example.yaml`)

## Créditos

Aplicação Alura Food e roteiro do curso: [Alura](https://www.alura.com.br).
