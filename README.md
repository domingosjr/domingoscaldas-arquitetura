# domingoscaldas-arquitetura — BJJ School

Projeto da disciplina **Arquiteturas Avançadas de Software com Microsserviços e Spring Framework [26E3_3]** (Infnet).

**Aluno:** Domingos Caldas de Oliveira Junior

Evolução arquitetural do **BJJ School** (gestão de escola de Jiu-Jitsu) construído na disciplina
anterior ([domingoscaldas-api](https://github.com/domingosjr/domingoscaldas-api)):

- **Etapa 1** — a aplicação foi reorganizada por **módulos de domínio** (uma só aplicação);
- **Etapa 2** — o cadastro de **campeonatos** foi extraído para um serviço independente, o
  **`campeonato-service`**, consumido pela aplicação principal via **OpenFeign**;
- **Etapa 3** — configuração externa (**profiles** e **variáveis de ambiente**), **configuração
  centralizada** (`config-server`), **MySQL** com um banco por aplicação e execução em
  **containers** com **Docker Compose**;
- **Etapa 4** — **mensageria assíncrona** com **RabbitMQ** (o `campeonato-service` avisa a principal
  quando um campeonato é alterado) e **processamento em lote** com **Spring Batch** (importação da
  lista de presença por CSV). É a versão final.

```
domingoscaldas-arquitetura/            (raiz do repositório)
├── domingoscaldas-arquitetura/        aplicação principal — BJJ School (porta 8080) + Dockerfile
├── campeonato-service/                serviço independente de campeonatos (porta 8083) + Dockerfile
├── config-server/                     configuração centralizada (Spring Cloud Config, porta 8888) + Dockerfile
├── compose.yml                        sobe tudo: config-server, 2 aplicações, 2 bancos MySQL, RabbitMQ
├── .env.example                       modelo das credenciais dos bancos e do RabbitMQ (o .env real não é versionado)
├── postman/                           coleção Postman com o roteiro de testes
└── README.md
```

Cada pasta é um projeto Maven completo (pom, Maven Wrapper, `application.properties`) e sobe
sozinha. Não há pom agregador nem dependência de código entre os projetos: os únicos elos são HTTP
e, desde a Etapa 4, a fila do RabbitMQ.

---

## Etapa 4 — Comunicação Assíncrona e Processamento em Lote

### Arquitetura final

```
                                   REST (síncrono, Etapa 2)
 Cliente ──HTTP──► principal (:8080) ──── OpenFeign: GET /campeonatos/{id} ────► campeonato-service (:8083)
                   API REST + regras                                             API REST de campeonatos
                        │                                                              │
                        ▼                                                              ▼
                  principal-db (MySQL)                                          campeonato-db (MySQL)
                        ▲                                                              │
                        │ atualiza o nome e a data                                     │ PUT /campeonatos/{id}
                        │ copiados nas conquistas                                      │ publica a mensagem
                        │                                                              ▼
              CampeonatoAlteradoConsumer ◄──── fila "campeonatos.alterados" ◄──── RabbitMQ (:5672)
                   (consumidor)                (durável)                         painel :15672
                                          MENSAGERIA (assíncrona)

 LOTE:  presencas.csv ──► importarPresencasJob [ItemReader → ItemProcessor → ItemWriter, chunks de 5] ──► tabela presencas
        (69 linhas)        disparado por POST /importacoes/presencas na principal                        (principal-db)
```

As três formas de comunicação convivem, cada uma por uma necessidade real do domínio:

| Forma | Onde está no projeto | Por que essa forma |
|---|---|---|
| **REST** (Etapa 2) | registrar uma conquista: a principal pergunta ao `campeonato-service` se o campeonato existe | a principal precisa da resposta **agora**, antes de gravar; o cliente espera 201 ou 404 |
| **Mensageria** (Etapa 4) | alterar um campeonato: o `campeonato-service` avisa por mensagem quem guarda cópia dos dados | ninguém espera a atualização da cópia; o serviço termina o seu trabalho mesmo com a principal fora do ar |
| **Batch** (Etapa 4) | importar a lista de presença do período: dezenas de registros de uma vez, a partir de um arquivo | é um volume de dados que chega junto, com validação linha a linha, sem ninguém esperando registro por registro |

As partes não precisam estar todas ligadas entre si (o lote não usa a fila, por exemplo), e nenhuma
foi criada só para cumprir o item: a mensagem resolve uma limitação registrada na Etapa 2, e o lote
alimenta a regra de graduação que já existia.

### Mensageria: campeonato alterado

**O problema.** Desde a Etapa 2 a `Conquista` guarda uma **cópia** do nome e da data do campeonato
(a regra de pontos precisa da data mesmo com o serviço fora do ar). A reflexão da Etapa 2 registrou
o custo: *essa cópia pode ficar desatualizada se o campeonato for alterado*. Agora, quando um
campeonato é alterado no `campeonato-service`, ele publica uma mensagem, e a aplicação principal
atualiza as cópias.

**Quem produz e quem consome.** No diagrama do enunciado a aplicação principal é a produtora. Aqui a
direção é a inversa, e isso segue a posse dos dados: o fato ("o campeonato 1 mudou") acontece no
`campeonato-service`, dono dos campeonatos, e quem tem interesse nele é a principal, dona das
conquistas. O serviço não conhece a principal (nem a URL, nem o banco): só publica o fato na fila.

| Peça | Onde | O que faz |
|---|---|---|
| Broker | container `rabbitmq` (`rabbitmq:4-management`) | guarda a fila e as mensagens; painel em http://localhost:15672 (usuário `bjj`, senha `bjj`) |
| Fila | `campeonatos.alterados`, **durável** | declarada pelas duas aplicações (`MensageriaConfig`), então existe mesmo que o consumidor nunca tenha subido |
| Mensagem | record `CampeonatoAlteradoMessage(campeonatoId, nome, data)` | só o necessário para atualizar a cópia; em JSON, persistente (`delivery_mode` 2) |
| Produtor | `CampeonatoAlteradoProducer` no `campeonato-service` | `rabbitTemplate.convertAndSend("campeonatos.alterados", mensagem)` depois de gravar a alteração |
| Consumidor | `CampeonatoAlteradoConsumer` na principal | `@RabbitListener(queues = "campeonatos.alterados")` → `ConquistaService.atualizarDadosDoCampeonato` |
| Conversor | `JacksonJsonMessageConverter` nas duas aplicações | objeto ↔ JSON |

Mensagem como ela fica na fila (lida pelo painel do RabbitMQ):

```json
{"campeonatoId":1,"nome":"Copa Rio de Jiu-Jitsu 2026","data":"2026-05-18"}
```

Como o `CampeonatoResponse` da Etapa 2, o record da mensagem existe nos dois projetos, sem
biblioteca compartilhada. O consumidor converte o JSON pelo tipo do parâmetro do método, então o
cabeçalho `__TypeId__` que o produtor envia (o nome da classe dele) não precisa existir do lado da
principal.

**O que acontece em cada falha** (todas verificadas no Compose, ver a tabela de resultados abaixo):

| Situação | Comportamento |
|---|---|
| **Consumidor fora do ar** (principal parada) | a alteração do campeonato responde 200; a mensagem **fica na fila** (`messages_ready` = 1, `consumers` = 0) e é processada quando a principal volta, sem nenhuma ação manual |
| **Broker fora do ar** | a alteração do campeonato continua valendo (200); o produtor registra `WARN Mensagem NAO publicada (broker indisponivel)` e a mensagem se perde, então a cópia fica desatualizada até a próxima alteração. A principal sobe e funciona sem o broker; o listener tenta reconectar a cada 5 s |
| **Erro ao processar** a mensagem | `spring.rabbitmq.listener.simple.default-requeue-rejected=false`: a mensagem é descartada com erro no log, em vez de voltar para a fila e falhar em loop |
| **Credenciais erradas** | a aplicação não sobe (falha rápida de configuração, como em `prod` sem as variáveis do banco) |

**Processar duas vezes não estraga nada:** o consumidor grava valores absolutos (o nome e a data
novos), não incrementos. **Competição entre consumidores:** durante os testes, uma principal em
`dev` e a do Compose ficaram ligadas na mesma fila, e cada mensagem foi entregue a **uma** delas só.
É o comportamento de fila, que serve para réplicas da mesma aplicação (mesmo banco). Se outra
aplicação também quisesse saber das alterações, ela teria a sua própria fila, ligada a uma
*exchange* do tipo *fanout*.

### Processamento em lote: importação da lista de presença

**O problema.** A presença é o que dá pontos para a graduação (1 ponto por treino desde a última
graduação). Na academia, a lista do período costuma chegar de uma vez (planilha da recepção, catraca,
tablet do tatame). Registrar uma a uma pelo `POST /presencas/alunos/{alunoId}` não é viável, e
precisa de validação: alunos que não existem, datas erradas, grafias diferentes do tipo de treino.

**Fluxo:** `presencas.csv` → **ItemReader** → **ItemProcessor** → **ItemWriter** → tabela `presencas`.

| Componente | Classe / bean | O que faz |
|---|---|---|
| Fonte | `src/main/resources/batch/presencas.csv` | cabeçalho `alunoId;data;tipoTreino` + 69 linhas |
| **Job** | `importarPresencasJob` (`BatchConfig`) | dois passos: importar e depois resumir; `RunIdIncrementer` permite executar de novo |
| **Step** 1 | `importarPresencasStep` | orientado a **chunks de 5**: lê 5 linhas, processa, grava e confirma a transação; 69 linhas = 14 transações |
| **ItemReader** | `FlatFileItemReader<PresencaBatch>` | pula o cabeçalho, separa por `;`, monta o record `PresencaBatch` (a data ainda é texto) |
| **ItemProcessor** | `PresencaProcessor` | valida e normaliza; linha inválida devolve `null` (o Spring Batch conta como *filtrada* e não a envia ao writer) |
| **ItemWriter** | lambda em `BatchConfig` | grava cada presença do chunk pelo `GraduacaoService.registrarPresenca` (a persistência da própria principal) |
| Step 2 | `resumirImportacaoStep` (tasklet) | registra no log o resumo: lidas, ignoradas, gravadas |
| Disparo | `POST /importacoes/presencas` (`ImportacaoController` → `ImportacaoPresencasService`) | o Job não roda ao subir a aplicação (`spring.batch.job.enabled=false`); a resposta traz os totais do Step 1 |

Regras do processor (cada linha ignorada aparece no log com o motivo):

| Regra | Exemplo no CSV | Resultado |
|---|---|---|
| aluno inexistente | `99;2026-09-01;Gi` | ignorada |
| aluno inativo | `3;2026-09-01;Gi` (Carlos) | ignorada |
| data inválida | `2;2026-02-30;Gi` | ignorada |
| data futura | `1;2099-01-10;Gi` | ignorada |
| tipo de treino vazio | `1;2026-09-06; ` | ignorada |
| presença já registrada | `1;2026-01-05;Gi` (já existe no seed) | ignorada — por isso importar de novo não duplica nada |
| tipo de treino com grafia diferente | ` gi `, `GI`, `nogi`, `NO GI` | normalizada para `Gi` / `No-Gi` |

O CSV de exemplo tem 69 linhas: 9 inválidas de propósito, 5 presenças novas do Anderson e 55 da
Beatriz. **Efeito visível na regra:** a Beatriz (faixa azul, que exige 60 pontos) passa a estar
apta à graduação.

| Execução | Lidas | Ignoradas | Gravadas |
|---|---|---|---|
| 1ª | 69 | 9 | 60 |
| 2ª (mesmo arquivo) | 69 | 69 | 0 |

Os metadados das execuções ficam nas tabelas `BATCH_*` **no banco da própria principal** (H2 em
`dev`; MySQL em `prod`, criadas por `spring.batch.jdbc.initialize-schema=always`). Consulta no MySQL
depois das duas execuções:

```
STEP_NAME               READ_COUNT  FILTER_COUNT  WRITE_COUNT  COMMIT_COUNT
importarPresencasStep       69           9            60           14
resumirImportacaoStep        0           0             0            1
importarPresencasStep       69          69             0           14
resumirImportacaoStep        0           0             0            1
```

**Diferenças em relação à aula (Spring Batch 6, que vem com o Spring Boot 4):** as classes mudaram
de pacote (`org.springframework.batch.core.job.Job`, `...core.step.Step`,
`org.springframework.batch.infrastructure.item.ItemWriter`, `...infrastructure.item.file.FlatFileItemReader`);
o chunk é declarado como `.<PresencaBatch, PresencaImportada>chunk(5).transactionManager(tm)`; o
Job é disparado pelo `JobOperator` (`startNextInstance`); e o starter é
`spring-boot-starter-batch-jdbc`, que guarda os metadados no banco. Os conceitos (Job, Step, reader,
processor, writer, chunk, tasklet) são os mesmos.

### Mensageria × Batch

| | Mensageria | Batch |
|---|---|---|
| Gatilho | um fato que acabou de acontecer (um campeonato foi alterado) | um conjunto de dados que chegou ou um horário (a lista do período) |
| Unidade | uma mensagem pequena por fato | muitos registros processados juntos, em chunks |
| Tempo | quase imediato (menos de 1 s no teste), mas sem ninguém esperando | quando for disparado; pode demorar, e o volume importa mais que a latência |
| Acoplamento | produtor e consumidor não se conhecem e não precisam estar no ar ao mesmo tempo | uma só aplicação lê a fonte e grava no próprio banco |
| Falha | a mensagem espera na fila até o consumidor voltar | o chunk com erro é desfeito; os metadados em `BATCH_*` registram o que foi feito |
| Neste projeto | `campeonatos.alterados` | `importarPresencasJob` |

### Como executar

**Tudo em containers** (como na Etapa 3, agora com seis containers: `rabbitmq` a mais):

```bash
(cd config-server && ./mvnw -DskipTests package)
(cd campeonato-service && ./mvnw -DskipTests package)
(cd domingoscaldas-arquitetura && ./mvnw -DskipTests package)
docker compose up --build -d
```

O `rabbitmq` tem healthcheck (`rabbitmq-diagnostics ping`), e as duas aplicações só sobem depois
dele. As credenciais vêm de `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` (padrão `bjj`, ver
`.env.example`); o endereço (`rabbitmq:5672` no Compose, `localhost:5672` em dev) vem do
`config-server`.

**Desenvolvimento local** (H2): só o broker em container, as aplicações pelo Maven:

```bash
docker compose up -d rabbitmq
cd campeonato-service && ./mvnw spring-boot:run
cd domingoscaldas-arquitetura && ./mvnw spring-boot:run
```

Sem o broker as duas aplicações sobem do mesmo jeito: o lote funciona, a alteração de campeonato
funciona (sem publicar a mensagem) e o consumidor fica tentando reconectar.

**Roteiro de demonstração:**

1. **Lote:** `GET /graduacoes/aptos` (só o Anderson) → `POST http://localhost:8080/importacoes/presencas`
   (69 lidas, 9 ignoradas, 60 gravadas; o log mostra cada linha ignorada e cada chunk gravado) →
   `GET /graduacoes/aptos` (Anderson e Beatriz) → repetir o POST (0 gravadas).
2. **Mensagem:** `PUT http://localhost:8083/campeonatos/1` com o nome `Copa Rio de Jiu-Jitsu 2026` →
   log do serviço `Mensagem enviada para a fila campeonatos.alterados`, log da principal
   `Mensagem recebida ... -> 3 conquista(s) atualizada(s)` → `GET http://localhost:8080/conquistas/1`
   mostra o nome novo.
3. **Consumidor indisponível:** `docker compose stop principal` → `PUT` de novo no campeonato 1 (200)
   → painel http://localhost:15672, aba *Queues*: `campeonatos.alterados` com **Ready 1** e 0
   consumidores → `docker compose start principal` → a mensagem é consumida (Ready 0) e a conquista
   mostra o dado novo.

### Testes e demonstração

- **Automatizados** (`./mvnw test`): `campeonato-service` (10: os 7 da Etapa 2, mais o produtor
  publicando na fila certa, o produtor não repassando a falha do broker e a alteração pela API
  publicando a mensagem com o nome e a data novos); aplicação principal (19: os 12 anteriores, mais
  as 5 regras do processor, o Job de ponta a ponta — 69/9/60, Beatriz com 60 pontos e apta, segunda
  execução com 0 gravadas — e o consumidor atualizando as três conquistas); `config-server` (2).
  Nos testes o listener não é iniciado (não há broker na máquina de build).
- **Coleção Postman**: pasta **E** (lote: pontos antes, importação, +55 pontos, Beatriz apta, nova
  execução sem duplicar) e pasta **F** (mensageria: alterar o campeonato e ver a conquista mudar;
  parar a principal, alterar de novo, ver a mensagem esperando na fila pela API do painel e religar).

**Resultado da execução no Compose (30/09/2026, Docker Desktop 4.85, RabbitMQ 4, MySQL 8.4):**

| Verificação | Resultado |
|---|---|
| `docker compose up --build -d` | 6 containers; `rabbitmq`, bancos e `config-server` *healthy*; log da principal: `Created new connection: ... amqp://bjj@...:5672/` |
| Lote no MySQL | 1ª execução `COMPLETED` 69/9/60; Beatriz 15 → 70 pontos (+55), Anderson 68 → 73; aptos: Anderson e Beatriz; 2ª execução 69/69/0; 9 tabelas `BATCH_*` no `bjjschool`; a principal reinicia sem erro com as tabelas já criadas |
| Mensagem com as duas no ar | `PUT /campeonatos/1` → em cerca de 0,4 s o log da principal mostra `3 conquista(s) atualizada(s)`; as conquistas 1, 2 e 3 com o nome e a data novos |
| Consumidor indisponível | `docker stop principal` + `PUT` (200) → fila com `messages_ready` 1 e `consumers` 0 → `docker start principal` → fila em 0, consumidor 1, conquistas atualizadas |
| Broker reiniciado com mensagem parada | principal parada, `PUT`, `docker restart rabbitmq` → a fila volta com `messages_ready` 1 (fila durável, mensagem persistente) → `docker start principal` → processada |
| Broker indisponível | `docker stop rabbitmq` + `PUT /campeonatos/2` → 200 e `WARN Mensagem NAO publicada`; a principal continua 200; `docker start rabbitmq` → o consumidor se reconecta sozinho |

### Reflexão

**1. Qual operação foi escolhida para ser assíncrona?**
A atualização das cópias de um campeonato alterado. Quando o `PUT /campeonatos/{id}` grava a
alteração no `campeonato-service`, ele publica `CampeonatoAlteradoMessage` (id, nome e data) na fila
`campeonatos.alterados`; a aplicação principal consome e atualiza o nome e a data guardados nas
conquistas daquele campeonato.

**2. Por que ela pode ser assíncrona?**
Porque ninguém precisa do resultado dentro da requisição. Quem altera o campeonato quer ver o
campeonato alterado, e isso já está garantido no banco do serviço. A cópia na principal só precisa
ficar certa logo depois, e alguns milissegundos (ou minutos, se a principal estiver fora) de
diferença não quebram nenhuma regra. Se fosse síncrona (o serviço chamando a principal por REST),
o serviço passaria a conhecer a principal e a depender dela: com a principal fora do ar, alterar um
campeonato falharia, ou a mudança se perderia. Com a fila, as duas aplicações não precisam estar no
ar ao mesmo tempo.

**3. O que acontece com a mensagem se o consumidor estiver indisponível?**
Ela fica guardada na fila do broker. A fila é durável e a mensagem é persistente, então ela espera
(no teste: `messages_ready` = 1, `consumers` = 0) até a principal voltar. Ao subir, o listener se
registra na fila, recebe a mensagem, atualiza as conquistas e confirma; só então a mensagem sai da
fila. Nada é perdido e não é preciso reenviar nada. O limite dessa garantia é o broker: se ele
estiver fora do ar no momento da publicação, a mensagem não chega a existir (o produtor registra o
aviso), e isso está nos limites abaixo.

**4. Qual funcionalidade foi escolhida para processamento em lote?**
A importação da lista de presença: um CSV com as presenças do período (aluno, data, tipo de treino)
é lido, validado e normalizado linha a linha, e as presenças válidas são gravadas em chunks de 5 na
tabela `presencas` da aplicação principal, alimentando a regra de pontos da graduação.

**5. Por que ela é adequada para Batch?**
Porque é um conjunto grande de registros que chega de uma vez, sem ninguém esperando cada um. O
Batch dá o que esse caso precisa: leitura estruturada do arquivo, uma etapa própria para validar e
descartar linhas ruins sem derrubar a importação, gravação em chunks (um erro desfaz só aquele
chunk; os anteriores continuam gravados) e o registro de cada execução (`BATCH_*`: quantas linhas foram lidas,
filtradas e gravadas). Por REST seriam 69 requisições feitas por alguém ou por um script, sem
controle do conjunto. Por mensageria seriam 69 mensagens para um dado que já chegou junto.

**6. Quando usar REST, mensageria ou Batch?**
- **REST** quando quem chama precisa da resposta para continuar: registrar uma conquista exige saber,
  naquele momento, se o campeonato existe (201 ou 404). É o mais simples, mas acopla no tempo: se o
  outro lado cai, a operação falha (os 503 da Etapa 2).
- **Mensageria** quando algo aconteceu e outros precisam saber, mas não precisam responder: alterar
  um campeonato e atualizar as cópias. O produtor termina o seu trabalho, e o consumidor processa
  quando puder, mesmo depois de ficar fora do ar.
- **Batch** quando o trabalho é um volume de dados tratado em conjunto, agendado ou disparado sob
  demanda: importar a lista de presença. O importante é processar tudo com controle, validação e
  registro, não responder rápido.

A mesma funcionalidade pode combinar as três. Registrar presença é REST quando o professor marca na
hora, e é Batch quando chega a lista do mês. As três formas de comunicação somam; nenhuma substitui
as outras.

### Limites conhecidos

- **Broker fora do ar na publicação:** a alteração do campeonato é gravada e a mensagem se perde
  (fica só o aviso no log). A solução completa seria o padrão *outbox*: gravar a mensagem numa tabela
  na mesma transação da alteração e publicar depois. Ficou fora por simplicidade.
- **Sem volume nomeado para o RabbitMQ:** mensagens paradas na fila sobrevivem ao restart do
  container (verificado), mas não a `docker compose down`, que remove o container.
- **Sem fila de mensagens mortas (DLQ):** a mensagem que falhar no consumidor é descartada com erro no
  log; o certo seria desviá-la para uma fila de análise.
- **Contrato duplicado:** o record da mensagem existe nos dois projetos e precisa coincidir.
- **Lote:** o arquivo é fixo no classpath (num caso real viria de um upload ou de uma pasta); o Job
  roda dentro da requisição HTTP, o que serve para 69 linhas mas não para arquivos grandes; e duas
  linhas iguais no mesmo chunk não são detectadas como repetidas, porque a verificação consulta o
  banco antes de o chunk ser gravado.

---

## Etapa 3 — Configuração e Execução dos Serviços

### Visão geral

```
docker compose up
┌──────────────────────────────── rede bjj-rede ─────────────────────────────────────┐
│                                                                                     │
│   config-server (:8888)  ◄──── GET /domingoscaldas-arquitetura/prod ────────┐       │
│   arquivos config/*.properties                                              │       │
│          ▲                                                                  │       │
│          │ GET /campeonato-service/prod                                     │       │
│   campeonato-service (:8083) ◄─── HTTP http://campeonato-service:8083 ─── principal (:8080)
│          │                                                                  │       │
│          ▼                                                                  ▼       │
│   campeonato-db (MySQL)                                           principal-db (MySQL)
│   volume campeonato-dados                                         volume principal-dados
└─────────────────────────────────────────────────────────────────────────────────────┘
      portas publicadas para a máquina: 8080, 8083, 8888   (os bancos não são publicados)
```

O código Java das duas aplicações **não mudou** nesta etapa. O que mudou foi onde as
configurações vivem e como a solução é empacotada e executada.

### Configurações: o que varia entre ambientes e o que foi externalizado

| Configuração | Varia? | Onde fica agora |
|---|---|---|
| Porta da aplicação (`server.port`) | sim | `config-server` (por profile); variável `SERVER_PORT` sobrescreve |
| Endereço do `campeonato-service` (`campeonato.service.url`) | sim: `localhost:8083` em dev, `campeonato-service:8083` no Compose | `config-server` (por profile); variável `CAMPEONATO_SERVICE_URL` sobrescreve |
| Endereço do ViaCEP (`viacep.url`) | pode variar (ambiente sem internet, mock) | `config-server`; variável `VIACEP_URL` |
| URL, usuário e senha do banco | sim, e a senha é segredo | variáveis `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (profile `prod`); H2 fixo no profile `dev` |
| Timeouts do Feign | raramente | `config-server` / `application-*.properties`, padrão 2000/3000 |
| Profile ativo | sim | variável `SPRING_PROFILES_ACTIVE` (padrão `dev`) |
| Endereço do `config-server` | sim | variável `CONFIG_SERVER_URL` (padrão `http://localhost:8888`) |
| Regras de negócio, mapeamentos, rotas, tratamento de erros | **não** | código Java — igual em todos os ambientes |
| `show-sql`, console H2, `ddl-auto` | sim | por profile: ligados em `dev`, desligados em `prod` |

**Profiles** (o mesmo esquema nas duas aplicações, igual ao exemplo da aula 5):

| Arquivo | Conteúdo |
|---|---|
| `application.properties` | o que é comum: nome da aplicação, `spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}`, `spring.config.import=optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}`, runner, Swagger, Jackson, log do Feign |
| `application-dev.properties` | desenvolvimento: H2 em memória, console H2, `show-sql`, **valores padrão** para tudo (`${SERVER_PORT:8080}`, `${CAMPEONATO_SERVICE_URL:http://localhost:8083}`) — roda sem configurar nada |
| `application-prod.properties` | produção: MySQL por `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`; `${SERVER_PORT}` e `${CAMPEONATO_SERVICE_URL}` **sem valor padrão**; `show-sql=false`; console H2 desligado |

Em `prod` não existe valor padrão de propósito. Se faltar configuração, a aplicação **falha ao
subir** em vez de rodar com um valor errado. Verificado: `SPRING_PROFILES_ACTIVE=prod` sem as
variáveis de banco encerra o processo com a mensagem `'url' must start with "jdbc"` (o
`${DB_URL}` não foi resolvido), antes de atender qualquer requisição.

**Variáveis de ambiente** usadas pela solução:

| Variável | Quem usa | Exemplo no Compose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | as duas aplicações | `prod` |
| `CONFIG_SERVER_URL` | as duas aplicações | `http://config-server:8888` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | as duas aplicações (profile `prod`) | `jdbc:mysql://principal-db:3306/bjjschool` |
| `SERVER_PORT` | qualquer aplicação (opcional) | — |
| `CAMPEONATO_SERVICE_URL`, `VIACEP_URL` | opcionais, sobrescrevem o `config-server` | — |
| `PRINCIPAL_DB_*`, `CAMPEONATO_DB_*` | o próprio `compose.yml` (credenciais dos containers de banco) | ver `.env.example` |

Precedência, verificada na prática: **variável de ambiente > `config-server` > arquivo local do
profile**. Exemplo: com o `config-server` servindo `server.port=8080`, `SERVER_PORT=18080` fez a
aplicação subir na 18080.

### Configuração centralizada: `config-server`

Projeto próprio e mínimo (`config-server/`): uma classe com `@EnableConfigServer`, backend
`native` (arquivos no classpath, sem repositório git) e um arquivo por aplicação e profile em
`src/main/resources/config/`:

```
domingoscaldas-arquitetura-dev.properties    server.port=8080  campeonato.service.url=http://localhost:8083
domingoscaldas-arquitetura-prod.properties   server.port=8080  campeonato.service.url=http://campeonato-service:8083
campeonato-service-dev.properties            server.port=8083  viacep.url=https://viacep.com.br/ws
campeonato-service-prod.properties           server.port=8083  viacep.url=https://viacep.com.br/ws
```

- Ele serve **portas e URLs**, o que é "ambiente da solução"; **segredos ficam fora dele**, nas
  variáveis do Compose.
- As aplicações são clientes por `spring-cloud-starter-config` + `spring.config.import=optional:configserver:...`.
  O `optional` significa: sem o servidor, a aplicação sobe com os valores locais do profile
  (em `dev` isso basta; em `prod` a falta da URL do serviço faz a aplicação falhar cedo).
- Conferência: `GET http://localhost:8888/domingoscaldas-arquitetura/prod` devolve o JSON com as
  propriedades servidas (também está na pasta D da coleção Postman).

### Banco de dados: um por aplicação

MySQL 8.4 (versão LTS), o mesmo banco usado na disciplina anterior; o enunciado aceita
PostgreSQL, MySQL ou equivalente. Cada container de banco recebe também uma senha de `root`
(`PRINCIPAL_DB_ROOT_PASSWORD`, `CAMPEONATO_DB_ROOT_PASSWORD`), usada só pelo healthcheck.

| Aplicação | Container | Banco | Tabelas |
|---|---|---|---|
| principal | `principal-db` (MySQL 8.4) | `bjjschool` | `alunos`, `instrutores`, `turmas`, `turmas_alunos`, `conquistas`, `presencas`, `graduacoes` |
| campeonato-service | `campeonato-db` (MySQL 8.4) | `campeonato` | `campeonatos` |

Nenhuma aplicação conhece o banco da outra: a `Conquista` guarda só o `campeonato_id` (mais o
nome e a data copiados na Etapa 2), sem chave estrangeira entre bancos, e os dados de campeonato
chegam pela API do serviço. Cada banco também tem o seu próprio usuário: o usuário da principal nem existe no
`campeonato-db` (`Access denied for user 'bjj'`). O H2 continua existindo apenas no profile `dev`. As consultas são
todas derivadas do Spring Data, então a troca de H2 para MySQL não mudou nenhuma linha de
código. Cada banco tem o seu **volume** (`principal-dados`, `campeonato-dados`): remover e
recriar os containers não apaga os dados; só `docker compose down -v` apaga.

### Docker: uma imagem por aplicação

Os três Dockerfiles são iguais e têm só o necessário (a receita da aula 6):

```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Sequência: código Java → `./mvnw package` gera o JAR → `docker build` copia o JAR para a imagem
→ `docker run` cria o container. O Maven continua compilando; o Docker só empacota e executa.
A mesma imagem serve para qualquer ambiente: o que muda é a configuração recebida na execução.

### Docker Compose: a solução inteira em um comando

O `compose.yml` na raiz declara cinco serviços, uma rede e dois volumes:

| Serviço | Imagem | Porta publicada | Recebe |
|---|---|---|---|
| `config-server` | build `./config-server` | 8888 | — |
| `principal-db` | `mysql:8.4` | não | `MYSQL_DATABASE/USER/PASSWORD/ROOT_PASSWORD`, volume `principal-dados` |
| `campeonato-db` | `mysql:8.4` | não | idem, volume `campeonato-dados` |
| `campeonato-service` | build `./campeonato-service` | 8083 | `SPRING_PROFILES_ACTIVE=prod`, `CONFIG_SERVER_URL`, `DB_*` |
| `principal` | build `./domingoscaldas-arquitetura` | 8080 | `SPRING_PROFILES_ACTIVE=prod`, `CONFIG_SERVER_URL`, `DB_*` |

- **Rede:** os containers se encontram pelo **nome do serviço** (`campeonato-service`,
  `principal-db`, `config-server`). Nenhuma URL entre containers usa `localhost` — dentro de um
  container, `localhost` é o próprio container.
- **Ordem de subida:** `depends_on` com `condition: service_healthy` — as aplicações só sobem
  depois que o `config-server` responde e que o seu banco aceita conexões (`mysqladmin ping` por TCP, que só responde depois que a inicialização do MySQL termina).
- **Senhas:** o `compose.yml` usa `${PRINCIPAL_DB_PASSWORD:-bjj}`: há um valor padrão só para a
  demonstração local, e um arquivo `.env` (não versionado, modelo em `.env.example`) ou variáveis
  do terminal o substituem.

### Como executar

**1. Desenvolvimento local, sem nada além do JDK** (profile `dev`, H2, valores padrão):

```bash
cd campeonato-service && ./mvnw spring-boot:run
cd domingoscaldas-arquitetura && ./mvnw spring-boot:run
```

**2. Desenvolvimento local com o `config-server`** (opcional; as aplicações passam a buscar porta
e URLs nele):

```bash
cd config-server && ./mvnw spring-boot:run          # http://localhost:8888/domingoscaldas-arquitetura/dev
```

**3. Sobrescrevendo uma configuração por variável de ambiente** (Git Bash, só para a sessão do
terminal — como na aula 5):

```bash
export SERVER_PORT=9090
export CAMPEONATO_SERVICE_URL=http://localhost:9091
cd domingoscaldas-arquitetura && ./mvnw spring-boot:run
```

**4. Tudo em containers** (profile `prod`, MySQL, `config-server`) — requer Docker Desktop:

```bash
# 1) gera os JARs (o Dockerfile copia target/*.jar)
(cd config-server && ./mvnw -DskipTests package)
(cd campeonato-service && ./mvnw -DskipTests package)
(cd domingoscaldas-arquitetura && ./mvnw -DskipTests package)

# 2) constrói as imagens e sobe os cinco containers
docker compose up --build -d

# acompanhar / parar (os dados dos bancos ficam nos volumes)
docker compose logs -f principal
docker compose down
```

Depois de subir: Swagger em http://localhost:8080/swagger-ui.html e
http://localhost:8083/swagger-ui.html; configuração servida em
http://localhost:8888/domingoscaldas-arquitetura/prod.

### Testes e demonstração

- **Automatizados** (`./mvnw test` em cada pasta): `config-server` (2 testes: serve a configuração
  da principal em `prod` com a URL pelo nome do serviço, e a do `campeonato-service` em `dev`);
  `campeonato-service` (7) e aplicação principal (12) continuam os da Etapa 2, agora rodando no
  profile `dev` sem o `config-server` no ar (o `optional` garante isso).
- **Coleção Postman** (`postman/`): as pastas A, B e C da Etapa 2 rodam sem alteração contra a
  solução no Compose (mesmas portas); a pasta **D** consulta o `config-server`.
- **Roteiro no Compose:** (1) `docker compose up --build -d` e `docker compose ps` mostram os
  cinco containers saudáveis; (2) `GET /conquistas/1/detalhes` na 8080 responde com os dados do
  campeonato e o log da `principal` mostra `---> GET http://campeonato-service:8083/campeonatos/1`
  (nome do serviço, não `localhost`); (3) `POST /campeonatos` na 8083 cria um campeonato,
  `docker compose down` + `docker compose up -d` e o registro continua lá (volume); (4)
  `docker compose exec principal-db mysql -ubjj -pbjj bjjschool -e 'show tables'` lista só as tabelas da
  principal e `docker compose exec campeonato-db mysql -ucampeonato -pcampeonato campeonato -e 'show tables'` só a
  tabela `campeonatos`; (5) `docker compose stop campeonato-service` → 503 na principal, `start` →
  volta a 200.

**Resultado da execução no Compose (25/09/2026, Docker Desktop 4.85, engine 29.6.2, MySQL 8.4):**

| Verificação | Resultado |
|---|---|
| `docker compose up --build -d` | 5 containers; bancos e `config-server` *healthy*; as duas aplicações no profile `prod` |
| Configuração centralizada | log das duas aplicações: `Fetching config from server at : http://config-server:8888` e `Located environment: ... profiles=[prod]` |
| Comunicação entre containers | `GET /conquistas/1/detalhes` na 8080 = 200; log: `---> GET http://campeonato-service:8083/campeonatos/1` |
| Regra de pontos no MySQL | `/graduacoes/alunos/1/pontos` = 68; aptos = Anderson |
| Escritas | nova conquista 201 (validada no serviço); novo campeonato com CEP 201, cidade `Rio de Janeiro - RJ` vinda do ViaCEP de dentro do container |
| Um banco por aplicação | `bjjschool`: alunos, conquistas, graduacoes, instrutores, presencas, turmas, turmas_alunos; `campeonato`: campeonatos; o usuário da principal recebe `Access denied` no banco do serviço |
| Persistência | `docker compose down` + `up -d`: o campeonato criado e a conquista nova continuam lá (3 campeonatos, 4 conquistas); o seed não duplicou |
| Serviço parado | `docker compose stop campeonato-service`: detalhes = 503 com `ErroResponse`; pontos continuam 200; `start`: serviço e principal voltam a responder em cerca de 6 s, sem reiniciar a principal |

### Reflexão

**1. Quais configurações variam entre ambientes?**
Porta de cada aplicação, endereço do serviço consumido (`localhost` em desenvolvimento, nome do
container no Compose), endereço da API externa, URL/usuário/senha do banco, o próprio profile
ativo, o endereço do `config-server`, e ajustes de execução como `show-sql`, console H2 e
`ddl-auto`. As regras de negócio (por exemplo, "conquista exige campeonato existente") não variam.

**2. Quais foram externalizadas?**
Todas as da lista acima. Nenhuma porta, endereço ou credencial existe no código Java: portas e
URLs vêm do `config-server` (com padrão local no profile `dev`), credenciais do banco vêm só de
variáveis de ambiente e o profile vem de `SPRING_PROFILES_ACTIVE`. O H2 continua fixo no profile
`dev` por ser um banco descartável de desenvolvimento.

**3. Por que um serviço não deve acessar o banco de outro?**
Porque o banco é um detalhe interno de quem é dono dos dados. Se a principal lesse a tabela
`campeonatos` diretamente, qualquer mudança de esquema no `campeonato-service` quebraria a
principal em silêncio, a validação e as regras do serviço seriam contornadas e os dois deixariam
de evoluir e de ser implantados de forma independente. A comunicação pela API (`GET
/campeonatos/{id}`) é o contrato; o banco não é.

**4. Que problema o Docker resolve?**
A diferença entre máquinas. Sem ele, cada pessoa precisaria instalar o JDK certo, o MySQL,
configurar portas e iniciar processos na ordem certa, e o mesmo código se comportaria diferente
em cada ambiente. A imagem fixa o runtime (`eclipse-temurin:17-jre`), o artefato e o comando de
início; o container é a execução isolada dessa imagem, igual em qualquer máquina.

**5. Qual a função do Docker Compose?**
Descrever a solução inteira em um arquivo e subi-la com um comando: as cinco partes, as
variáveis que cada uma recebe, as portas publicadas, os volumes dos bancos, a rede em que os
containers se encontram pelo nome e a ordem de subida (`depends_on` com healthcheck). Sem ele
seriam cinco `docker run` com dezenas de parâmetros fáceis de esquecer.

**6. Que problema a configuração centralizada resolve?**
Configuração espalhada e duplicada. Com duas aplicações (e mais na sequência), cada uma teria
seus próprios arquivos com as mesmas decisões de ambiente — a porta do serviço, a URL do ViaCEP —
e uma mudança exigiria editar e reimplantar cada projeto. O `config-server` guarda essas decisões
em um só lugar, por aplicação e por profile, e as aplicações as buscam ao subir. Nesta etapa ele
serve portas e URLs; segredos continuam fora dele, em variáveis do ambiente.

### Limites conhecidos

- Em `prod` usamos `ddl-auto=update` para o Compose criar o esquema no primeiro start. Em produção
  real o correto seria `validate` com uma ferramenta de migração (Flyway/Liquibase).
- O `config-server` não tem autenticação nem HTTPS; serve só configurações não sensíveis.
- Os JARs são gerados fora do Docker (`./mvnw package`) antes do `docker compose up --build`,
  como na aula; um build multi-stage faria isso dentro da imagem.

---

## Etapa 2 — Separação e comunicação entre serviços

### Visão geral

```
Cliente HTTP (Postman / Swagger)
   │
   ▼
Aplicação principal — BJJ School  (:8080, H2 domingoscaldasarquiteturadb)
   ├── aluno, instrutor, turma, graduacao        (como na Etapa 1)
   ├── conquista
   │     ConquistaController → ConquistaService ──┐
   └── campeonato                                 │
         client/CampeonatoGateway  ◄──────────────┘
         client/CampeonatoClient  (@FeignClient, url = ${campeonato.service.url})
                  │  HTTP/JSON  GET /campeonatos/{id}
                  ▼
campeonato-service  (:8083, H2 campeonatodb)
   ├── campeonato/   Campeonato, CampeonatoController, CampeonatoService, CampeonatoRepository, dto/
   ├── endereco/     EnderecoController, EnderecoService, client/ViaCepClient + ViaCepGateway ──► ViaCEP
   └── exception/    GlobalExceptionHandler, ErroResponse      + Swagger (/swagger-ui.html)
```

### Serviço independente

- **Nome:** `campeonato-service`
- **Responsabilidade principal:** manter o **cadastro dos campeonatos** em que a escola compete
  (nome, cidade, CEP e data), preenchendo a cidade a partir do CEP pela API pública ViaCEP, e informar
  os dados de um campeonato a quem precisar deles.
- **Funcionalidade removida da aplicação principal:** os módulos `campeonato` (entidade `Campeonato`,
  repository, service, controller e a tabela `campeonatos`) e `endereco` (consulta de CEP no ViaCEP)
  saíram da aplicação principal. As rotas `/campeonatos` e `/enderecos/{cep}` agora existem só no
  serviço. Na principal ficou apenas o lado consumidor: `campeonato/client` (client Feign, gateway e
  o DTO de resposta) e `campeonato/exception`.
- **Motivo da separação (por que pode ser executada separadamente):** o cadastro de campeonatos é um
  catálogo com dados próprios, que não depende de nenhum outro módulo da escola — sua única
  dependência era o módulo `endereco`, que foi junto. Do lado de quem usa, havia **um único
  consumidor** (o módulo `conquista`) e ele só precisa **ler** um campeonato por id. Não existe
  operação que grave nos dois lados ao mesmo tempo. É a fronteira mais limpa do sistema.

### Por que o candidato mudou em relação à Etapa 1

Na Etapa 1 a funcionalidade escolhida foi **graduação**, com **endereço (ViaCEP)** como segundo
candidato analisado. Ao partir para a implementação, a análise foi revista:

- A extração de **graduação** chegou a ser prototipada. Ela exigia gravar em dois bancos na mesma
  operação (o histórico de graduações no serviço e a faixa atual do aluno na aplicação principal),
  com compensação em caso de falha, além de enviar faixa e conquistas a cada cálculo de pontos. É
  complexidade de consistência distribuída que o domínio, no tamanho atual, não justifica.
- O **endereço** sozinho não tem dados nem regra: seria um serviço que só repassa uma API externa,
  criado apenas para cumprir o requisito.
- O endereço, porém, existia para servir a **um único módulo: campeonato**. Extraindo os dois
  juntos, o serviço passa a ter responsabilidade de negócio, dados e banco próprios, e a consulta de
  CEP continua ao lado de quem a usa. É a evolução do segundo candidato.

Graduação continua como módulo bem isolado dentro da aplicação principal.

### Responsabilidade de cada aplicação

| | `campeonato-service` | Aplicação principal |
|---|---|---|
| Responsabilidade | Cadastro de campeonatos e consulta de CEP | Alunos, instrutores, turmas, graduação (presenças, histórico, regra de pontos) e conquistas |
| Dados | Tabela `campeonatos` no H2 `campeonatodb` | Demais tabelas no H2 `domingoscaldasarquiteturadb`; de campeonato guarda só `campeonato_id` e uma cópia do nome e da data dentro de `conquistas` |
| Integrações | ViaCEP (API externa, via OpenFeign) | `campeonato-service` (via OpenFeign) |

### API REST do `campeonato-service` (Swagger: http://localhost:8083/swagger-ui.html)

| Verbo | Path | Descrição | Respostas |
|---|---|---|---|
| `GET` | `/campeonatos` | lista, do mais recente para o mais antigo | 200 |
| `GET` | `/campeonatos/{id}` | **operação consumida pela aplicação principal** | 200 · 404 |
| `POST` | `/campeonatos` | cadastra; com CEP, a cidade vem do ViaCEP | 201 · 400 · 404 (CEP inexistente) · 503 (ViaCEP fora) |
| `PUT` | `/campeonatos/{id}` | altera | 200 · 400 · 404 · 503 |
| `DELETE` | `/campeonatos/{id}` | exclui | 204 · 404 |
| `GET` | `/enderecos/{cep}` | consulta de endereço por CEP | 200 · 400 · 404 · 503 |

Entrada e saída são **DTOs** (`CampeonatoRequest`, `CampeonatoResponse`, records) — a entidade JPA
`Campeonato` nunca é exposta. Erros usam sempre o corpo `ErroResponse {status, erro, mensagem, dataHora}`.
Cada operação documenta suas estruturas e respostas HTTP com `@Operation`, `@ApiResponses` e `@Schema`.

### Comunicação via OpenFeign (aplicação principal)

```java
@FeignClient(name = "campeonato-service", url = "${campeonato.service.url}")
public interface CampeonatoClient {

    @GetMapping("/campeonatos/{id}")
    CampeonatoResponse obterPorId(@PathVariable("id") Long id);
}
```

- **Caminho da chamada:** `ConquistaController → ConquistaService → CampeonatoGateway → CampeonatoClient`.
  O controller não conhece o Feign. O **gateway é o único lugar com `try/catch` da comunicação**.
- **Endereço fora do código Java:** `campeonato.service.url=http://localhost:8083` no
  `application.properties` (pode ser sobrescrito pela variável de ambiente `CAMPEONATO_SERVICE_URL`).
  O mesmo vale para o ViaCEP no serviço (`viacep.url`).
- **Timeouts:** `connectTimeout=2000` e `readTimeout=3000`. Sem eles o Feign esperaria 10 s e 60 s
  para perceber que o serviço não responde.
- **Evidência no console** (`loggerLevel=basic`):
  `[CampeonatoClient#obterPorId] ---> GET http://localhost:8083/campeonatos/1 HTTP/1.1` /
  `<--- HTTP/1.1 200 (12ms)`.

**Onde a principal usa o serviço:**

| Operação da principal | O que acontece |
|---|---|
| `POST /conquistas/alunos/{alunoId}/campeonatos/{campeonatoId}` | valida o aluno localmente; consulta o campeonato no serviço; grava a conquista com `campeonatoId` e uma cópia do nome e da data do evento |
| `GET /conquistas/{id}/detalhes` | devolve a conquista com os dados **atuais** do campeonato (inclusive a cidade), consultados no serviço |

**Por que copiar nome e data na conquista:** a regra de pontos da graduação só conta medalhas de
campeonatos posteriores à última graduação do aluno, então precisa da data do evento. Com a cópia, a
regra e a lista de conquistas **não dependem do serviço estar no ar**. O custo: se a data de um
campeonato for corrigida depois, as conquistas já registradas mantêm a data antiga. Para um evento que
já aconteceu esse risco é pequeno e foi aceito. Sem FK entre os bancos, excluir um campeonato no
serviço também não apaga as conquistas — elas preservam o nome e a data copiados.

### Tratamento de falhas

| Situação | Onde é detectada | Exceção | HTTP | Mensagem ao cliente |
|---|---|---|---|---|
| Aluno inexistente | `ConquistaService`, antes de qualquer chamada remota | `AlunoNaoEncontradoException` | 404 | "Aluno não encontrado: 999" |
| Campeonato inexistente no serviço (404 remoto) | `CampeonatoGateway` (`FeignException.NotFound`) | `CampeonatoRemotoNaoEncontradoException` | 404 | "O campeonato de ID 999 não foi encontrado no campeonato-service." |
| Serviço desligado, conexão recusada ou timeout | `CampeonatoGateway` (`RetryableException`) | `CampeonatoServiceIndisponivelException` | **503** | "O serviço de campeonatos está temporariamente indisponível. Tente novamente em instantes." |

As respostas usam o `ErroResponse` padrão e **não expõem detalhes internos** (URL, porta,
"Connection refused", stack trace). A causa técnica fica apenas no log (`WARN`). O
`GlobalExceptionHandler` não conhece o Feign: só o gateway. No serviço, o `ViaCepGateway` segue o
mesmo padrão para a API externa (ViaCEP fora do ar → 503).

### O que mudou em relação à Etapa 1

- `/campeonatos` e `/enderecos/{cep}` saíram da porta 8080 e existem só no serviço (8083).
- `Conquista` trocou a relação `@ManyToOne Campeonato` por `campeonatoId` + `campeonatoNome` +
  `campeonatoData`. No JSON, o objeto `campeonato` deu lugar a esses três campos.
- Nova operação `GET /conquistas/{id}/detalhes`.
- A regra de pontos (`GraduacaoService.calcularPontos`) passou a ler a data do campeonato da própria
  conquista. Os números não mudaram.
- O `ProjectRunner` da principal não faz chamada remota: as conquistas de demonstração já trazem os
  dados da Copa Rio, que é o campeonato de id 1 no seed do serviço.

### Como executar

Requisitos: JDK 17+ (o Maven Wrapper baixa o Maven). A ordem de subida é indiferente.

```bash
# terminal 1 — serviço independente (porta 8083)
cd campeonato-service
./mvnw spring-boot:run

# terminal 2 — aplicação principal (porta 8080)
cd domingoscaldas-arquitetura
./mvnw spring-boot:run
```

| | Aplicação principal | campeonato-service |
|---|---|---|
| API | http://localhost:8080 | http://localhost:8083 |
| Swagger | http://localhost:8080/swagger-ui.html | http://localhost:8083/swagger-ui.html |
| Console H2 | http://localhost:8080/h2-console (`jdbc:h2:mem:domingoscaldasarquiteturadb`) | http://localhost:8083/h2-console (`jdbc:h2:mem:campeonatodb`) |

Usuário `sa`, senha em branco nos dois consoles.

### Testes

**Automatizados** (`./mvnw test` em cada pasta):

- `campeonato-service` (7 testes): `contextLoads`; `CampeonatoControllerTest` — campeonato do seed por
  id, 404 com `ErroResponse`, 400 com as mensagens de validação, cadastro com CEP preenchendo a cidade,
  CEP inexistente → 404 e ViaCEP fora do ar → 503. O ViaCEP é substituído por um mock, então os testes
  não dependem de internet.
- Aplicação principal (12 testes): `contextLoads` **com o serviço desligado** (prova de que sobe
  sozinha); `CampeonatoGatewayTest` — resposta normal, 404 remoto e serviço fora do ar;
  `ConquistaServiceTest` — copia nome e data do campeonato, não chama o serviço quando o aluno não
  existe e não grava nada quando o campeonato não existe ou o serviço está fora;
  `CampeonatoIndisponivelIntegrationTest` — OpenFeign de verdade apontado para uma porta fechada:
  503 com `ErroResponse`, nenhuma conquista gravada, e alunos, turmas, conquistas, pontos (68) e
  aptos continuam respondendo 200.

**Coleção Postman** — `postman/BJJ-School-Etapa-2.postman_collection.json`:

| Pasta | Cenário | O que mostra |
|---|---|---|
| **A** — serviço isolado | só o `campeonato-service` no ar | documentação OpenAPI, CRUD de campeonatos, 404, 400, cidade pelo CEP, consulta de CEP |
| **B** — via aplicação principal | as duas no ar | conquista registrada com o campeonato validado no serviço; **404 remoto** (campeonato 999) × **404 local** (aluno 999); detalhes com dados atuais do serviço; pontos 68 |
| **C** — serviço desligado | parar o serviço | 503 sem detalhes técnicos; nada gravado; conquistas, pontos e turmas continuam 200; **religar o serviço → 200 sem reiniciar a principal** |

### Reflexão arquitetural

**1. Qual funcionalidade foi separada da aplicação principal?**
O cadastro de campeonatos, junto com a consulta de endereço por CEP que o alimenta. Os módulos
`campeonato` e `endereco`, a tabela `campeonatos` e a integração com o ViaCEP foram para o
`campeonato-service`. A aplicação principal ficou com alunos, instrutores, turmas, graduação e
conquistas, e consulta o serviço por OpenFeign quando registra ou detalha uma conquista.

**2. Por que ela foi escolhida?**
Porque tem a fronteira mais limpa do sistema: dados próprios, nenhuma dependência de outros módulos
(a única, endereço, foi junto), um único consumidor e consumo somente de leitura. O candidato da
Etapa 1, graduação, foi prototipado e descartado porque exigia gravar em dois bancos na mesma
operação. O segundo candidato, endereço, não tinha dados nem regra para justificar um serviço
sozinho; extraído junto com o módulo que o usa, passou a fazer sentido.

**3. O que ficou mais complexo depois da separação?**
- O que era uma chamada em memória (`campeonatoService.obterPorId`) virou uma chamada de rede que
  pode falhar: foi preciso client, gateway, duas exceções novas, timeouts e uma resposta 503.
- A chave estrangeira `campeonato_id` deixou de existir. A conquista guarda um identificador externo
  e uma cópia do nome e da data; essa cópia pode ficar desatualizada se o campeonato for alterado.
  *(Resolvido na Etapa 4: a alteração publica uma mensagem e a principal atualiza as cópias.)*
- O contrato (`CampeonatoResponse`) existe nos dois projetos e precisa coincidir; uma divergência só
  aparece em tempo de execução.
- A operação passou a ter dois processos, duas portas, dois bancos, dois logs e dados de
  demonstração combinados por id entre as duas aplicações.

**4. O que aconteceria com a funcionalidade principal caso o novo serviço ficasse indisponível?**
A disponibilidade passa a ser parcial. A aplicação principal sobe e continua atendendo alunos,
turmas, presenças, graduações, a regra de pontos e a lista de conquistas — a regra não é afetada
porque a data do campeonato fica copiada na conquista. Só duas operações respondem 503, com uma
mensagem sem detalhes técnicos: registrar uma nova conquista e consultar os detalhes de uma conquista.
Nada é gravado pela metade, porque a consulta ao serviço acontece antes da gravação. Quando o serviço
volta, a aplicação principal se recupera sem precisar ser reiniciada.

**5. A funcionalidade realmente precisa permanecer como um serviço independente ou poderia continuar dentro da aplicação?**
Para uma única escola poderia continuar dentro da aplicação: o monólito modular da Etapa 1 já
isolava bem o módulo, sem custo de rede. Como serviço ela faz sentido se o catálogo de campeonatos
passar a ser compartilhado (outras unidades da academia, um site público com o calendário de
eventos) ou alimentado por outra fonte, como a importação do calendário de uma federação. Mantivemos
como serviço porque a fronteira é estável e o acoplamento é mínimo: uma operação de leitura. Se esses
cenários não se confirmarem, voltar a ser um módulo é simples. É uma decisão revisável, não uma
obrigação tecnológica.

### Preparo para as próximas etapas

- **Etapa 3:** URL e porta já estão em propriedades que aceitam variável de ambiente; os bancos já são
  separados, sem FK nem tabela compartilhada; as consultas são derivadas do Spring Data, sem SQL
  específico do H2; cada pasta recebe o seu Dockerfile.
- **Etapa 4:** candidatos naturais — mensagem `CONQUISTA_REGISTRADA` para notificação, e importação do
  calendário de campeonatos a partir de um CSV com Spring Batch no `campeonato-service`.
  *(Na Etapa 4 a escolha mudou para o que resolvia problemas reais: a mensagem de campeonato alterado,
  que corrige a cópia desatualizada do item 3 acima, e a importação da lista de presença, que alimenta
  a regra de graduação.)*

---

## Etapa 1 — Organização Arquitetural da Aplicação

> Estado registrado na tag `etapa-1`. Os módulos `campeonato` e `endereco` descritos aqui foram
> extraídos para o `campeonato-service` na Etapa 2.

Uma única aplicação Spring Boot (mesmo projeto, mesma porta, mesmo deploy). Em vez da estrutura
técnica anterior (`controller/`, `service/`, `repository/`, `domain/`), cada módulo de domínio
reúne suas próprias classes:

```
br.edu.infnet.domingoscaldas_arquitetura
├── aluno        → Aluno, Faixa, AlunoController, AlunoService, AlunoRepository, AlunoNaoEncontradoException
├── instrutor    → Instrutor, InstrutorController, InstrutorService, InstrutorRepository, ...
├── turma        → Turma, TurmaController, TurmaService, TurmaRepository, AlunoJaMatriculadoException, dto/ (TurmaResponse, AlunoResumoResponse)
├── graduacao    → Presenca, Graduacao, GraduacaoService (regra de pontos), PresencaController, GraduacaoController, ...
├── campeonato   → Campeonato, CampeonatoController, CampeonatoService, CampeonatoRepository, ...
├── conquista    → Conquista, Medalha, ConquistaController, ConquistaService, ConquistaRepository, ...
├── endereco     → ViaCepClient (OpenFeign), Endereco, EnderecoService, EnderecoController
├── comum        → Pessoa (@MappedSuperclass: dados comuns de aluno e instrutor)
└── exception    → GlobalExceptionHandler, ErroResponse, RecursoNaoEncontradoException, ConflitoException (transversal)
```

Em cada módulo: `Cliente HTTP → Controller → Service → Repository → Banco de dados (H2)`.
Controllers só traduzem HTTP; regras ficam nos services; repositories são detalhe interno de
persistência de cada módulo e **nunca são acessados por outro módulo**.

### Módulos e responsabilidades

| Módulo | Responsabilidade |
|---|---|
| **aluno** | Cadastro e manutenção dos alunos (dados pessoais, faixa e graus atuais, situação ativa). Oferece `obterPorId`, consultas e a operação `graduar` aos outros módulos. |
| **instrutor** | Cadastro dos instrutores da escola (faixa, registro na federação, situação ativa). |
| **turma** | Turmas de treino (Kids, Adulto, Competição...) e a matrícula de alunos nas turmas. |
| **graduacao** | Frequência (presenças nos treinos), histórico de graduações e a **regra central do negócio**: pontos acumulados desde a última graduação (presenças + medalhas) e alunos aptos a novo grau/faixa. |
| **campeonato** | Cadastro dos campeonatos (eventos) em que a escola compete, com cidade preenchida pelo CEP. |
| **conquista** | Resultados dos alunos nos campeonatos (medalha e categoria) e o quadro de medalhas da escola. |
| **endereco** | Consulta de endereço por CEP na API pública ViaCEP (via OpenFeign). |

### Análise de dependências

Todas as dependências entre módulos passam pelo **service** do módulo consumido (nunca pelo
repository) e apontam numa única direção — não há ciclos:

```
turma      → aluno
conquista  → aluno, campeonato
graduacao  → aluno, conquista
campeonato → endereco
instrutor, aluno, endereco → (não dependem de ninguém)
```

- **turma → aluno**: para matricular, a turma confirma que o aluno existe e obtém seus dados pelo
  `AlunoService`; o `AlunoRepository` é detalhe interno do módulo aluno.
- **conquista → aluno, campeonato**: registrar uma conquista exige um aluno e um campeonato
  existentes (`AlunoService.obterPorId`, `CampeonatoService.obterPorId`).
- **graduacao → aluno**: o cálculo de pontos consulta o aluno (`obterPorId`, `obterAtivos`) e, ao
  registrar uma graduação, pede ao módulo aluno que atualize faixa/graus (`graduar`).
- **graduacao → conquista**: as medalhas do aluno (`ConquistaService.obterPorAluno`) entram na
  pontuação; o peso de cada medalha vive no enum `Medalha`, do módulo conquista.
- **campeonato → endereco**: ao cadastrar um campeonato com CEP, a cidade é preenchida pelo
  `EnderecoService`.

**Decisão revisada — herança de `Pessoa`:** no projeto anterior, `Pessoa` era uma entidade mapeada
com `@Inheritance(strategy = JOINED)` (tabela `pessoas` compartilhada por `alunos` e
`instrutores`), estratégia adequada para um monólito em camadas técnicas e exigida pela etapa de
persistência daquela disciplina. Ao organizar por módulos, essa tabela compartilhada passou a ser
um **acoplamento de dados entre os módulos aluno e instrutor** — incompatível com módulos donos
dos próprios dados e com a futura separação em serviços com bancos independentes. `Pessoa` foi
então remapeada como `@MappedSuperclass` (pacote `comum`): a herança continua no **código**
(validações, atributos e acessores herdados), mas cada módulo mantém a **sua própria tabela** e
o seu próprio identificador. O custo é perder a consulta polimórfica "todas as pessoas", que, se
necessária, passa a ser composta a partir dos dois módulos.

Critério de agrupamento: **responsabilidade**, não entidade. A presença fica em `graduacao` porque
existe para alimentar a regra de pontos (se o módulo for extraído, leva suas presenças junto); a
conquista ganhou módulo próprio porque é um fato do aluno tanto quanto do campeonato — assim
`campeonato` fica só com o cadastro de eventos.

Mudança estrutural em relação ao projeto anterior: o `Aluno` **não carrega mais** as listas de
presenças, conquistas e graduações. Quem precisa delas pergunta ao módulo dono
(`findByAlunoId`), o que mantém a dependência numa direção só e permite extrair um módulo sem
arrastar os demais.

### Candidato a serviço independente

- **Funcionalidade escolhida: graduação** (frequência + regra de pontos + histórico de
  graduações).
- **Responsabilidade:** registrar presenças e graduações e decidir, a partir da frequência e das
  medalhas, quando um aluno está apto a um novo grau ou faixa.
- **Por que pode ser executada separadamente:** é uma responsabilidade de negócio completa e
  autocontida, com dados próprios (presenças, graduações) e uma regra que só precisa de
  informações **públicas** dos outros módulos (aluno por id, conquistas por aluno) — hoje obtidas
  por chamada interna ao service, amanhã por HTTP (OpenFeign). É também a funcionalidade que mais
  se beneficia de evolução independente: recálculo assíncrono de pontos ao registrar presença
  (mensageria) e processamento em lote das presenças de todos os alunos (batch).
- **Quem depende dela hoje:** a API de graduações/presenças consumida pelos clientes da escola; o
  módulo aluno é **consumido** por ela (`obterPorId`, `graduar`), assim como o módulo conquista
  (`obterPorAluno`).
- **Segundo candidato analisado — endereço (ViaCEP):** zero dependência do domínio e do banco,
  contrato trivial; seria a extração mais simples, mas com pouco valor de negócio. Foi mantido como
  alternativa, e a escolha recaiu sobre graduação pelo valor para o domínio.

Nesta etapa **nenhuma funcionalidade foi separada** — esta análise foi o ponto de partida da Etapa 2,
onde o candidato foi revisto (ver "Por que o candidato mudou em relação à Etapa 1").

### API da Etapa 1

| Recurso | Operações |
|---|---|
| `/alunos` | CRUD (`POST` 201, `PUT`, `DELETE` 204) · `GET /ativos` · `?nome=` · `?faixa=AZUL` |
| `/instrutores` | CRUD · `GET /ativos` |
| `/turmas` | CRUD · `GET /ativas` · `GET /{id}/detalhes` (DTO) · `POST /{turmaId}/alunos/{alunoId}` matricula |
| `/presencas` | `GET` lista/id · `?inicio=&fim=` · `GET /alunos/{alunoId}` · `POST /alunos/{alunoId}` · `PUT` · `DELETE` |
| `/graduacoes` | `GET` lista/id · `GET /alunos/{alunoId}` · `GET /alunos/{alunoId}/pontos` · `GET /aptos` · `POST /alunos/{alunoId}` · `PUT` · `DELETE` |
| `/campeonatos` | CRUD (cidade preenchida pelo CEP quando informado) — *Etapa 2: no `campeonato-service`* |
| `/conquistas` | `GET` lista/id · `?medalha=OURO` · `GET /quadro-medalhas` · `GET /alunos/{alunoId}` · `POST /alunos/{alunoId}/campeonatos/{campeonatoId}` · `PUT` · `DELETE` |
| `/enderecos/{cep}` | Consulta de endereço no ViaCEP — *Etapa 2: no `campeonato-service`* |

- **Validação:** Bean Validation nas entidades (`@NotBlank`, `@Email`, `@Past`, `@NotNull`,
  `@PositiveOrZero`, `@Pattern`, `@Size`) com `@Valid` nos controllers.
- **Exceções:** cada módulo tem as suas exceções de domínio (`AlunoNaoEncontradoException`,
  `TurmaNaoEncontradaException`, `AlunoJaMatriculadoException`, ...), derivadas das bases
  transversais `RecursoNaoEncontradoException` e `ConflitoException`; o `GlobalExceptionHandler`
  (`@RestControllerAdvice`) traduz para `404` (não encontrado), `409` (conflito com o estado
  atual, ex.: matrícula duplicada) e `400` (dados inválidos: Bean Validation,
  `IllegalArgumentException`), sempre com o corpo `ErroResponse` (status, erro, mensagem, dataHora).
- **Consultas Spring Data:** `findByAtivoTrue`, `findByNomeContainingIgnoreCase`, `findByFaixa`,
  `findAllByOrderByNomeAsc`, `findByDataBetween`, `findByAlunoId`, `countByAlunoIdAndDataAfter`,
  `findTopByAlunoIdOrderByDataDesc`, `findByMedalha`, `findAllByOrderByDataDesc`.
- **Documentação:** Swagger/OpenAPI (springdoc) em `/swagger-ui.html`.

## Marcos (tags git)

- **etapa-1** — versão organizada da aplicação, antes da separação de qualquer funcionalidade em
  serviço independente.
- **etapa-2** — `campeonato-service` separado e consumido pela aplicação principal via OpenFeign, com
  tratamento de indisponibilidade.
- **etapa-3** — profiles e variáveis de ambiente, `config-server`, MySQL com um banco por
  aplicação, Dockerfiles e `docker compose up`.
- **etapa-4** — versão final: mensageria com RabbitMQ (produtor no `campeonato-service`, fila
  `campeonatos.alterados`, consumidor na principal) e Spring Batch (importação da lista de presença
  em chunks).
