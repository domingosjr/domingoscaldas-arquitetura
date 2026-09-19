# domingoscaldas-arquitetura — BJJ School

Projeto da disciplina **Arquiteturas Avançadas de Software com Microsserviços e Spring Framework [26E3_3]** (Infnet).

**Aluno:** Domingos Caldas de Oliveira Junior

Evolução arquitetural do **BJJ School** (gestão de escola de Jiu-Jitsu) construído na disciplina
anterior ([domingoscaldas-api](https://github.com/domingosjr/domingoscaldas-api)):

- **Etapa 1** — a aplicação foi reorganizada por **módulos de domínio** (uma só aplicação);
- **Etapa 2** — o cadastro de **campeonatos** foi extraído para um serviço independente, o
  **`campeonato-service`**, consumido pela aplicação principal via **OpenFeign**.

```
domingoscaldas-arquitetura/            (raiz do repositório)
├── domingoscaldas-arquitetura/        aplicação principal — BJJ School (porta 8080)
├── campeonato-service/                serviço independente de campeonatos (porta 8083)
├── postman/                           coleção Postman com o roteiro de testes da Etapa 2
└── README.md
```

Cada pasta é um projeto Maven completo (pom, Maven Wrapper, `application.properties`, H2 próprio) e
sobe sozinha. Não há pom agregador nem dependência de código entre os dois projetos: o único elo é
HTTP.

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
