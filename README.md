# Kubernetes com microsserviços — Alura Food

[![CI](https://github.com/KauanDiNubila/kubernetes-microsservicos-alura/actions/workflows/ci.yml/badge.svg)](https://github.com/KauanDiNubila/kubernetes-microsservicos-alura/actions/workflows/ci.yml)

Projeto de estudo desenvolvido durante o curso de Kubernetes com microsserviços Java da [Alura](https://www.alura.com.br). O código das aplicações (Alura Food) é o material base do curso; o foco do repositório é a parte de Kubernetes: empacotar os serviços em imagens Docker e subir tudo em um cluster.

## O que foi praticado

- Build de imagens com Dockerfile multi-stage (Maven + JRE)
- `Deployment` com réplicas para os serviços sem estado
- `StatefulSet` para o Eureka Server e para o MySQL
- `Service` headless para descoberta entre pods e `LoadBalancer` para acesso externo
- `ConfigMap` e `Secret` injetados como variáveis de ambiente
- `PersistentVolumeClaim` para os dados do MySQL
- `startupProbe`, `readinessProbe` e `livenessProbe` com o Spring Boot Actuator, e `requests`/`limits` de CPU e memória

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

### Em outro cluster (kind, por exemplo)

O deploy também foi testado no [kind](https://kind.sigs.k8s.io). As diferenças em relação ao minikube:

- Troque o `storageClassName` em `k8s/volumes.yaml` por uma classe que exista no cluster (no kind, `standard`)
- No lugar do `minikube tunnel`, use `port-forward`:

```bash
kubectl port-forward svc/loadbalancer-gateway 8082:8082
```

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

- `kaauandi/alurafood-server:v2`
- `kaauandi/alurafood-gateway:v2`
- `kaauandi/alurafood-pedidos:v2`
- `kaauandi/alurafood-pagamentos:v2`

Para gerar uma imagem localmente:

```bash
docker build -t alurafood-pedidos pedidos
```

## Testes

Os testes de `pedidos` e `pagamentos` cobrem as regras dos services e as respostas HTTP dos controllers (201, 400 e 404) e não dependem de banco de dados. Para rodar sem instalar Maven nem Java 17:

```bash
docker run --rm -v "$PWD/pedidos:/app" -w /app maven:3.9.6-eclipse-temurin-17-focal mvn test
```

Troque `pedidos` por `pagamentos`, `server` ou `gateway` para os outros serviços.

## O que mudei em relação ao material do curso

**Kubernetes**

- Adicionei `startupProbe`, `readinessProbe` e `livenessProbe` em todos os containers, usando os endpoints de health do Actuator
- Defini `requests` e `limits` de CPU e memória e ajustei o heap da JVM ao limite do container
- Adicionei um `initContainer` em `pedidos` e `pagamentos` que espera o MySQL responder, para os pods não reiniciarem enquanto o banco sobe
- Corrigi o `containerPort` do `gateway` e do `pagamentos`, que não batiam com a porta real das aplicações
- Publiquei as imagens na minha conta do Docker Hub, no lugar das imagens do curso
- Tirei o `Secret` com a senha do banco do versionamento e deixei um modelo (`secrets.example.yaml`)

**Aplicações**

- As validações passaram a valer na entrada da API: antes ficavam só nas entidades e um corpo inválido virava erro 500
- Recurso inexistente agora devolve 404 e erro de validação devolve 400 com a lista de campos
- Atualizar um pagamento sem informar o status mantém o status atual
- Troquei os testes que só subiam o contexto (e exigiam MySQL) por testes de service e de controller
## Créditos

Aplicação Alura Food e roteiro do curso: [Alura](https://www.alura.com.br).
