# domingoscaldas-arquitetura — BJJ School

Projeto da disciplina **Arquiteturas Avançadas de Software com Microsserviços e Spring Framework [26E3_3]** (Infnet).

**Aluno:** Domingos Caldas de Oliveira Junior

Evolução arquitetural do **BJJ School** (gestão de escola de Jiu-Jitsu) construído na disciplina
anterior ([domingoscaldas-api](https://github.com/domingosjr/domingoscaldas-api)): a aplicação
foi **reorganizada por módulos de domínio** antes de ser distribuída em serviços.

## Etapa 1 — Organização Arquitetural da Aplicação

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

Nesta etapa **nenhuma funcionalidade foi separada** — esta análise é o ponto de partida da Etapa 2.

## API da Etapa 1

| Recurso | Operações |
|---|---|
| `/alunos` | CRUD (`POST` 201, `PUT`, `DELETE` 204) · `GET /ativos` · `?nome=` · `?faixa=AZUL` |
| `/instrutores` | CRUD · `GET /ativos` |
| `/turmas` | CRUD · `GET /ativas` · `GET /{id}/detalhes` (DTO) · `POST /{turmaId}/alunos/{alunoId}` matricula |
| `/presencas` | `GET` lista/id · `?inicio=&fim=` · `GET /alunos/{alunoId}` · `POST /alunos/{alunoId}` · `PUT` · `DELETE` |
| `/graduacoes` | `GET` lista/id · `GET /alunos/{alunoId}` · `GET /alunos/{alunoId}/pontos` · `GET /aptos` · `POST /alunos/{alunoId}` · `PUT` · `DELETE` |
| `/campeonatos` | CRUD (cidade preenchida pelo CEP quando informado) |
| `/conquistas` | `GET` lista/id · `?medalha=OURO` · `GET /quadro-medalhas` · `GET /alunos/{alunoId}` · `POST /alunos/{alunoId}/campeonatos/{campeonatoId}` · `PUT` · `DELETE` |
| `/enderecos/{cep}` | Consulta de endereço no ViaCEP |

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

## Como executar

Requisitos: JDK 17+ (o Maven Wrapper baixa o Maven sozinho).

```bash
cd domingoscaldas-arquitetura
./mvnw spring-boot:run
```

- API: http://localhost:8080 · **Swagger:** http://localhost:8080/swagger-ui.html
- **Console H2:** http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:domingoscaldasarquiteturadb`, usuário `sa`, senha em branco)
- A rotina `ProjectRunner` carrega dados de demonstração (alunos, turmas, presenças, campeonato,
  conquistas); desative com `app.runner.habilitado=false`.

## Marcos (tags git)

- **etapa-1** — versão organizada da aplicação, antes da separação de qualquer funcionalidade em
  serviço independente.
