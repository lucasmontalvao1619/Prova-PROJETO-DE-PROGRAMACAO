# Guia de estudo: Java e Spring Boot do projeto Orcamento

Este guia descreve o código desta pasta, sem analisar o frontend. A leitura foi feita em 07/10/2026. Foram lidas todas as classes Java, a configuração e o `pom.xml`; os oito testes existentes passaram. A existência do arquivo do banco foi confirmada, mas seus registros não foram consultados e a aplicação não foi iniciada nesta análise.

## 1. Primeiro entenda o que o sistema faz

É uma API REST de orçamento pessoal. Ela cadastra, lista, busca, atualiza e apaga transações financeiras. Também lista as categorias disponíveis e calcula receitas, despesas, saldo e gastos por categoria.

Uma transação é um lançamento como "Mercado, R$ 200, ALIMENTACAO". A categoria determina se o lançamento é uma receita ou uma despesa. O cliente não envia um campo `tipo`: o Java o calcula a partir da categoria.

Exemplo: `ALIMENTACAO` significa despesa; `SALARIO` significa receita. Todos os valores enviados são positivos. Uma despesa de R$ 200 é enviada como `200`, e o sistema a subtrai ao calcular o saldo.

É uma aplicação única, com arquitetura em camadas e pacotes organizados por responsabilidade. Controller, service e repository rodam no mesmo processo Java. Não são três servidores nem microsserviços.

```text
Requisição HTTP com JSON
        |
        v
Controller: recebe a requisição e define a resposta HTTP
        |
        v
Service: executa as regras e organiza as operações
        |
        v
Repository: solicita leitura/gravação de entidades
        |
        v
Hibernate/JPA -> JDBC -> H2 -> arquivo no disco

Na volta: entidade -> DTO de resposta -> JSON -> resposta HTTP
```

## 2. Mapa das pastas

```text
Prova-PROJETO-DE-PROGRAMACAO/
|-- pom.xml
|-- mvnw
|-- mvnw.cmd
|-- .mvn/wrapper/maven-wrapper.properties
|-- src/
|   |-- main/
|   |   |-- java/com/lucdev/orcamento/
|   |   |   |-- OrcamentoApplication.java
|   |   |   |-- controller/TransacaoController.java
|   |   |   |-- service/TransacaoService.java
|   |   |   |-- repository/TransacaoRepository.java
|   |   |   |-- model/
|   |   |   |   |-- Transacao.java
|   |   |   |   |-- Categoria.java
|   |   |   |   |-- TipoTransacao.java
|   |   |   |-- dto/
|   |   |   |   |-- TransacaoRequest.java
|   |   |   |   |-- TransacaoResponse.java
|   |   |   |   |-- ResumoResponse.java
|   |   |   |-- exception/
|   |   |       |-- RecursoNaoEncontradoException.java
|   |   |       |-- ApiExceptionHandler.java
|   |   |-- resources/application.properties
|   |-- test/java/com/lucdev/orcamento/service/TransacaoServiceTest.java
|-- target/
```

`src/main/resources/static` contém o frontend e fica fora deste guia.

| Pasta ou arquivo | Para que serve | Você precisa estudar? |
|---|---|---|
| `src/main/java` | Código Java da aplicação | Sim, é o foco principal |
| `com/lucdev/orcamento` | Pacote base; corresponde a `package com.lucdev.orcamento` | Sim, para entender onde o Spring encontra as classes |
| `controller` | Entrada HTTP da API | Sim |
| `service` | Regras e operações de negócio | Sim |
| `repository` | Acesso às entidades persistidas | Sim |
| `model` | Entidade e tipos do domínio financeiro | Sim |
| `dto` | Formato dos dados de entrada e saída | Sim |
| `exception` | Exceções e conversão delas em respostas HTTP | Sim |
| `src/main/resources` | Configuração que acompanha a aplicação | Sim, especialmente o banco |
| `src/test/java` | Testes que não fazem parte do código de produção | Sim, ajudam a entender os comportamentos esperados |
| `.mvn/wrapper` | Configuração do Maven Wrapper, usando Maven 3.9.9 | Entenda a função; não é regra de negócio |
| `pom.xml` | Dependências, versão Java e configuração de build | Sim |
| `target` | Classes compiladas, relatórios e outros resultados do Maven | Não é onde você escreve código; não contém o banco configurado |
| `.idea` | Configuração do IntelliJ | Não é arquitetura Spring |
| `.git` | Histórico e controle de versão | Não executa regras da aplicação |

## 3. Como a aplicação nasce

Arquivo: `src/main/java/com/lucdev/orcamento/OrcamentoApplication.java`.

```java
@SpringBootApplication
public class OrcamentoApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrcamentoApplication.class, args);
    }
}
```

`main` é o ponto de entrada do Java. `SpringApplication.run` inicia o contexto do Spring e o servidor web embutido.

`@SpringBootApplication` combina configuração, autoconfiguração e busca de componentes. Com essa classe no pacote `com.lucdev.orcamento`, o Spring encontra os componentes dos subpacotes deste projeto, além das entidades e repositories pelos mecanismos de autoconfiguração.

Um **bean** é um objeto administrado pelo Spring. Aqui, o controller e o service são beans; o Spring Data fornece o bean que implementa o repository.

Veja os construtores:

```java
public TransacaoController(TransacaoService service) {
    this.service = service;
}

public TransacaoService(TransacaoRepository repository) {
    this.repository = repository;
}
```

O Spring entrega as dependências ao criar os objetos. Isso é **injeção de dependência por construtor**. Como há um único construtor em cada classe, não é necessário escrever `@Autowired` nele. `private final` impede trocar a referência depois de inicializá-la; não torna o objeto inteiro imutável.

Na inicialização, as configurações e dependências permitem montar a conexão com H2, o Hibernate e os repositories. A porta HTTP definida no projeto é `8081`.

## 4. Pasta model: quais dados existem

### TipoTransacao.java

É um `enum`, um conjunto fechado de opções:

```java
public enum TipoTransacao {
    RECEITA,
    DESPESA
}
```

Você não pode inventar um terceiro tipo num request sem alterar o código.

### Categoria.java

Outro `enum`, com oito opções. Cada uma possui um rótulo para apresentação e um tipo financeiro:

| Constante usada no JSON | Rótulo | Tipo |
|---|---|---|
| `ALIMENTACAO` | Alimentação | DESPESA |
| `TRANSPORTE` | Transporte | DESPESA |
| `MORADIA` | Moradia | DESPESA |
| `LAZER` | Lazer | DESPESA |
| `SAUDE` | Saúde | DESPESA |
| `SALARIO` | Salário | RECEITA |
| `PRESENTE` | Presente | RECEITA |
| `EXTRA` | Renda extra | RECEITA |

Em `ALIMENTACAO("Alimentação", TipoTransacao.DESPESA)`, os argumentos inicializam os campos pelo construtor do enum. `name()` retorna `ALIMENTACAO`; `getRotulo()` retorna `Alimentação`; `getTipo()` retorna `DESPESA`.

As categorias são definidas no código, não cadastradas numa tabela própria. Adicionar uma categoria exige alterar esse enum.

### Transacao.java

É a entidade persistida: o objeto que o Hibernate mapeia para uma tabela.

| Campo Java | Significado | Comportamento no projeto |
|---|---|---|
| `Long id` | Identificador do lançamento | Gerado pelo banco |
| `String descricao` | Texto como Mercado | O service remove espaços nas pontas |
| `BigDecimal valor` | Quantia em dinheiro | O request exige valor positivo |
| `Categoria categoria` | Categoria do lançamento | Persistida pelo nome da constante |
| `LocalDateTime dataHora` | Data e hora de criação | Definida com `LocalDateTime.now()` no construtor |

As anotações explicam o mapeamento:

- `@Entity`: a classe é uma entidade JPA.
- `@Id`: `id` é a chave primária.
- `@GeneratedValue(strategy = GenerationType.IDENTITY)`: o banco gera o identificador ao inserir.
- `@Enumerated(EnumType.STRING)`: a categoria é armazenada como nome, por exemplo `ALIMENTACAO`, e não como posição numérica do enum.

Como `@Id` está no campo, este mapeamento usa acesso pelos campos. O método `getTipo()` é calculado e não cria uma coluna `tipo`:

```java
public TipoTransacao getTipo() {
    return categoria.getTipo();
}
```

Também não há coluna `categoriaRotulo`; o rótulo é obtido do enum para montar a resposta. Não há campo de usuário, relacionamento entre entidades ou tabela de resumo no código.

O construtor vazio `protected Transacao()` permite ao JPA reconstruir uma entidade ao ler o banco. O construtor público é usado pelo service para criar um lançamento e atribui a data/hora atual do servidor. `LocalDateTime` não armazena um fuso horário.

`BigDecimal` permite cálculo decimal adequado para dinheiro. No código, `add` e `subtract` devolvem novos valores; por isso é necessário atribuir o resultado. As opções de precisão e escala da coluna não estão explicitamente configuradas na entidade: não confunda a capacidade do tipo Java com a capacidade da coluna gerada.

Os setters existem para os campos editáveis. Não há setter para `id` nem `dataHora`. Portanto, atualizar descrição, valor e categoria conserva a data de criação.

`equals` considera iguais duas transações com o mesmo ID não nulo. Duas entidades novas com ID nulo não são iguais, a menos que sejam a mesma instância. `hashCode` usa a classe, mantendo o hash estável quando o ID é atribuído. Isso ajuda a lidar com entidades em coleções; não salva dados nem gera IDs.

## 5. Pasta dto: o contrato da API

DTO significa objeto de transferência de dados. A entidade representa a persistência; os DTOs representam o que a API recebe ou devolve. Eles não são tabelas.

Os DTOs usam `record`. O Java gera construtor, acessores, `equals`, `hashCode` e `toString`. Um acesso é `request.valor()`, não `request.getValor()`. Os componentes são finais, mas isso não torna uma lista contida num record automaticamente imutável.

### TransacaoRequest.java: entrada

Recebe somente `descricao`, `valor` e `categoria`.

```json
{
  "descricao": "  Mercado  ",
  "valor": 200.00,
  "categoria": "ALIMENTACAO"
}
```

| Validação | O que rejeita |
|---|---|
| `@NotBlank` em descrição | `null`, texto vazio ou apenas espaços |
| `@NotNull` em valor | Valor ausente ou nulo |
| `@Positive` em valor | Zero e valores negativos |
| `@NotNull` em categoria | Categoria ausente ou nula |

`@Positive` sozinho não rejeita `null`; por isso existe também `@NotNull`. `@Valid`, usado no controller, manda executar essas restrições antes de chamar o service. Chamar o service diretamente num teste não ativa automaticamente a validação HTTP.

### TransacaoResponse.java: saída

Devolve `id`, `descricao`, `valor`, `categoria`, `categoriaRotulo`, `tipo` e `dataHora`.

O método estático `de(Transacao t)` faz a conversão explícita da entidade para esse record. Ele obtém o nome e o rótulo do enum e calcula o tipo. Nada é gravado no banco por esse método.

### ResumoResponse.java: saída dos cálculos

Contém `totalReceitas`, `totalDespesas`, `saldo` e uma lista `gastosPorCategoria`. O record interno `GastoPorCategoria` possui `categoria`, `rotulo` e `total`.

Esses resultados são calculados a cada consulta ao resumo. Não são entidades nem registros de uma tabela de resumo.

## 6. Pasta repository: quem acessa o banco

Arquivo: `TransacaoRepository.java`.

```java
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findAllByOrderByDataHoraDesc();
}
```

`Transacao` é o tipo de entidade; `Long` é o tipo da chave primária. A interface herda operações como `save`, `findById`, `findAll` e `delete`. Não há uma classe de implementação escrita no projeto: o Spring Data fornece essa implementação em execução.

O nome `findAllByOrderByDataHoraDesc` descreve a consulta: buscar todas as transações e ordenar por `dataHora` de forma decrescente, da mais recente para a mais antiga. O Spring Data interpreta o nome, como documentado em [métodos de consulta](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html).

SQL equivalente para visualizar a ideia, não uma cópia do SQL real gerado:

```sql
SELECT * FROM transacao ORDER BY data_hora DESC;
```

`findById` retorna um `Optional`: pode haver uma entidade ou nenhuma. O service decide o que fazer quando não encontra.

## 7. Pasta service: como cada operação funciona

`@Service` registra `TransacaoService` como componente do Spring. Essa classe recebe o repository e concentra as operações.

| Método | Passo a passo |
|---|---|
| `listar()` | Busca as entidades ordenadas, converte cada uma com `TransacaoResponse.de` e retorna a lista |
| `buscar(id)` | Busca a entidade ou lança exceção; converte a encontrada para resposta |
| `criar(request)` | Aplica `trim()` à descrição, constrói a entidade, salva e converte o resultado |
| `atualizar(id, request)` | Busca a entidade existente, altera os três campos editáveis, salva e converte |
| `apagar(id)` | Busca a entidade existente e pede ao repository que a apague |
| `resumo()` | Busca todas as transações e calcula os totais em Java |
| `somar(transacoes, tipo)` | Método privado que soma somente os lançamentos do tipo informado |
| `buscarEntidade(id)` | Método privado que centraliza a busca e a exceção quando o ID não existe |

Em `stream().map(TransacaoResponse::de).toList()`, `stream` permite processar a sequência, `map` transforma cada entidade numa resposta e `toList` reúne as respostas. `TransacaoResponse::de` é uma referência ao método; equivale a `t -> TransacaoResponse.de(t)`.

`buscarEntidade` usa:

```java
repository.findById(id).orElseThrow(
    () -> new RecursoNaoEncontradoException(
        "Transação %d não encontrada.".formatted(id)));
```

Se o resultado estiver vazio, a função depois de `orElseThrow` cria a exceção. `%d` é substituído pelo ID. A mesma regra vale para buscar, atualizar e apagar.

### O cálculo do resumo

Imagine os lançamentos:

| Descrição | Categoria | Valor |
|---|---|---|
| Salário | SALARIO | 3000 |
| Mercado | ALIMENTACAO | 200 |
| Padaria | ALIMENTACAO | 50 |
| Uber | TRANSPORTE | 60 |

O método `somar` calcula receitas = `3000` e despesas = `310`. O saldo é `3000 - 310 = 2690`.

Depois, um `HashMap<Categoria, BigDecimal>` acumula apenas despesas. `getOrDefault(categoria, BigDecimal.ZERO)` começa em zero quando aquela categoria ainda não apareceu. Somar Mercado e Padaria produz `ALIMENTACAO = 250`; Transporte fica em `60`.

O service transforma cada entrada do mapa num `GastoPorCategoria` e ordena os resultados do maior para o menor com `b.total().compareTo(a.total())`. A ordem invertida de `b` e `a` é o que produz a ordenação decrescente.

O resumo considera todas as transações disponíveis. Não existe filtro por mês, período ou usuário. Com nenhuma transação, os três totais são zero e a lista de gastos fica vazia. Categorias sem despesas não entram na lista.

Esse cálculo usa `findAll` e percorre os registros em memória; não utiliza `SUM` ou `GROUP BY` no banco.

### O que @Transactional faz aqui

`criar`, `atualizar` e `apagar` têm `@Transactional`. Quando o controller chama o service administrado pelo Spring, o mecanismo de transação envolve o método: inicia ou participa de uma transação, executa a operação e confirma as alterações ao terminar com sucesso. Uma exceção de execução não tratada provoca rollback por padrão.

`save` não deve ser interpretado como "já confirmou tudo no disco". Hibernate pode enviar SQL antes do fim do método, mas a confirmação da transação é outra etapa. No cadastro com `IDENTITY`, o insert permite obter o ID gerado para montar a resposta.

Na atualização, a entidade buscada fica gerenciada durante a transação. Hibernate consegue detectar mudanças em seus campos; o código também chama `save` explicitamente.

## 8. Pasta controller: quais URLs existem

`@RestController` registra a entrada web e faz os retornos serem escritos no corpo da resposta. `@RequestMapping("/api")` define o prefixo comum.

| Método HTTP | Caminho | Método Java | Resposta de sucesso |
|---|---|---|---|
| GET | `/api/transacoes` | `listar` | 200, lista de transações |
| GET | `/api/transacoes/{id}` | `buscar` | 200, uma transação |
| POST | `/api/transacoes` | `criar` | 201, transação criada |
| PUT | `/api/transacoes/{id}` | `atualizar` | 200, transação atualizada |
| DELETE | `/api/transacoes/{id}` | `apagar` | 204, sem corpo |
| GET | `/api/resumo` | `resumo` | 200, totais e gastos por categoria |
| GET | `/api/categorias` | `categorias` | 200, categorias definidas no enum |

`@PathVariable Long id` transforma o trecho da URL em argumento Java. `@RequestBody` recebe o corpo JSON convertido para `TransacaoRequest`. Essa conversão é feita pelos conversores HTTP do Spring MVC, usando Jackson para JSON neste conjunto de dependências.

`ResponseEntity` controla status e corpo. `ok` significa 200; `status(CREATED)` significa 201; `noContent().build()` significa 204.

`categorias()` é a exceção ao fluxo controller -> service -> repository: ele percorre `Categoria.values()` diretamente. Não precisa consultar o banco porque os valores vêm do enum. Seu `CategoriaResponse` é um record interno ao controller com `nome`, `rotulo` e `tipo`.

## 9. Pasta exception: como erros viram respostas

`RecursoNaoEncontradoException` estende `RuntimeException` e carrega uma mensagem. O service a lança quando não existe o ID solicitado.

`ApiExceptionHandler`, anotado com `@RestControllerAdvice`, centraliza o tratamento das exceções na camada web. Seus métodos com `@ExceptionHandler` escolhem a resposta correspondente:

| Exceção | Quando aparece | Resultado |
|---|---|---|
| `RecursoNaoEncontradoException` | ID não encontrado | HTTP 404 |
| `MethodArgumentNotValidException` | Restrições do request falharam com `@Valid` | HTTP 400 com mensagens dos campos |
| `IllegalArgumentException` | Argumento ilegal que chegue a esse tratamento | HTTP 400 com a mensagem |

O corpo usa `ProblemDetail`, que descreve o problema com campos como `status` e `detail`. Na validação, o código reúne os erros no formato `campo: mensagem`, separados por `;`.

JSON malformado ou categoria escrita como `"COMIDA"` falham na conversão do corpo, antes de validar um DTO completo. Essas falhas não são explicitamente tratadas pelos três métodos acima; o Spring MVC fornece o tratamento padrão. Não conte com o mesmo texto de erro de `@NotNull` para uma constante de enum desconhecida.

## 10. Qual é o banco e onde ele fica

O banco é **H2**, versão **2.3.232** na árvore de dependências resolvida. É um banco relacional escrito em Java. Neste projeto ele opera embutido na aplicação e persiste em arquivo, conforme a URL configurada e os modos descritos na [documentação do H2](https://h2database.com/html/features.html).

Configuração atual em `src/main/resources/application.properties`:

```properties
spring.application.name=orcamento
server.port=8081
spring.datasource.url=jdbc:h2:file:~/.orcamento/dados
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
springdoc.swagger-ui.path=/swagger-ui.html
```

| Configuração | Significado nesta aplicação |
|---|---|
| `spring.application.name` | Nome lógico da aplicação |
| `server.port=8081` | Porta HTTP da API, não uma porta separada do banco |
| `jdbc:h2:` | Usa JDBC e o driver H2 |
| `file:` | Armazenamento persistente em arquivo |
| `~/.orcamento/dados` | Caminho baseado na pasta pessoal do usuário que executa o Java |
| `username=sa` | Usuário usado nas conexões |
| `password=` | Senha vazia |
| `ddl-auto=update` | Hibernate tenta adequar o esquema às entidades ao iniciar |
| `h2.console.enabled=true` | Habilita o console web do H2 |
| `swagger-ui.path` | Caminho configurado para a documentação interativa da API |

Foi encontrado o arquivo `C:\Users\thede\.orcamento\dados.mv.db`. O banco não fica dentro de `src`, de `target` ou da pasta OneDrive deste projeto. Copiar só o projeto para outro computador não copia esses dados.

Ao reiniciar sob o mesmo usuário e com essa configuração, a aplicação pode reabrir o mesmo arquivo. Isso é diferente de usar uma URL `jdbc:h2:mem:...`, que configuraria armazenamento em memória.

`ddl-auto=update` trata a estrutura das tabelas, não cria lançamentos de exemplo. Não foram encontrados `data.sql`, `schema.sql` nem migrações no projeto. Esse modo não substitui um histórico de migrações e não garante qualquer transformação de esquema; as opções estão descritas em [inicialização do banco no Spring Boot](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html).

Com o mapeamento atual e as convenções padrão, a entidade corresponde à tabela `transacao`, com campos `id`, `descricao`, `valor`, `categoria` e `data_hora`. Isso descreve o mapeamento esperado; o esquema do arquivo existente não foi inspecionado nesta análise.

## 11. Dependências: quem faz cada parte

No `pom.xml`, o parent é `spring-boot-starter-parent:3.5.16`. Ele fornece gerenciamento de versões e padrões de build. A configuração `java.version=17` define o nível Java do projeto; não significa que o Java instalado na máquina obrigatoriamente seja a versão 17.

Uma dependência **direta** aparece no seu `pom.xml`. Uma **transitiva** vem porque outra biblioteca precisa dela. Um **starter** reúne bibliotecas para uma funcionalidade.

| Dependência direta | Papel no projeto |
|---|---|
| `spring-boot-starter-web` | Spring MVC, servidor Tomcat embutido e conversão JSON; permite os endpoints HTTP |
| `spring-boot-starter-data-jpa` | Spring Data JPA, Hibernate e infraestrutura JDBC; permite repositories e persistência |
| `spring-boot-starter-validation` | Implementação de Bean Validation; executa as restrições do request |
| `springdoc-openapi-starter-webmvc-ui:2.8.17` | Documentação OpenAPI e Swagger UI; não é necessário para persistir dados |
| `h2` com escopo `runtime` | Motor do banco e driver JDBC disponíveis durante a execução |
| `spring-boot-starter-test` com escopo `test` | JUnit, Mockito e AssertJ para os testes; não compõe as dependências de produção do JAR |

O plugin `spring-boot-maven-plugin` pertence à seção de build, não à lista de dependências da aplicação. Ele permite executar pelo Maven e empacotar um JAR executável com as bibliotecas e o servidor embutido.

### O conjunto que faz o banco funcionar

```text
TransacaoRepository
    -> Spring Data JPA: implementação das operações do repository
    -> JPA: contrato de persistência e anotações da entidade
    -> Hibernate: implementação JPA que mapeia objetos e gera SQL
    -> Infraestrutura JDBC / conexões administradas pelo HikariCP
    -> Driver JDBC H2
    -> Motor H2
    -> dados.mv.db
```

Esse desenho explica responsabilidades, não uma lista literal de chamadas entre todas as classes internas.

**JPA é uma especificação; Hibernate é uma implementação; H2 é o banco.** JDBC é a API Java para conversar com bancos por drivers. ORM é o mapeamento entre objetos e tabelas, realizado aqui pelo Hibernate.

Versões confirmadas por `mvnw.cmd dependency:tree`:

| Biblioteca transitiva | Versão resolvida | Responsabilidade |
|---|---|---|
| `spring-data-jpa` | 3.5.13 | Operações de repository JPA |
| `hibernate-core` | 6.6.53.Final | Mapeamento, contexto de persistência e SQL |
| `spring-jdbc` | 6.2.19 | Infraestrutura Spring para JDBC |
| `HikariCP` | 6.3.3 | Pool que disponibiliza e reutiliza conexões |
| `hibernate-validator` | 8.0.3.Final | Validação dos DTOs, não persistência |

HikariCP chega pelo starter JDBC, transitivo do starter JPA. Com essa combinação, o Boot pode autoconfigurar o `DataSource`, objeto que fornece conexões. A seleção do pool e a configuração SQL são descritas na [documentação SQL do Spring Boot](https://docs.spring.io/spring-boot/3.5/reference/data/sql.html).

O driver é inferido da URL `jdbc:h2:...`; não há `driver-class-name` nem configuração manual de `DataSource` no projeto. Hibernate identifica o banco para gerar SQL compatível. Não há servidor MySQL ou PostgreSQL envolvido na configuração atual.

## 12. Siga um cadastro do começo ao fim

Envio: `POST http://localhost:8081/api/transacoes`, com `Content-Type: application/json` e o JSON da seção de DTOs.

1. Tomcat recebe a requisição HTTP; Spring MVC encontra `TransacaoController.criar`.
2. Jackson converte o JSON para `TransacaoRequest`. `"ALIMENTACAO"` vira a constante do enum.
3. `@Valid` verifica descrição, valor e categoria. Se falhar, a camada web retorna 400 e o service não é chamado.
4. O controller chama `service.criar(request)`. O mecanismo de `@Transactional` envolve a operação.
5. O service transforma `"  Mercado  "` em `"Mercado"` com `trim()`.
6. `new Transacao(...)` monta o objeto e atribui a data/hora atual. Ainda não é uma linha salva só por ter sido construído.
7. `repository.save` passa a persistência para a implementação JPA. Hibernate usa uma conexão e o driver H2 para inserir; o banco gera o ID.
8. `TransacaoResponse.de` monta o DTO, incluindo `tipo=DESPESA` a partir da categoria.
9. Ao terminar a operação transacional com sucesso, ocorre a confirmação. Uma falha nessa confirmação também pode impedir o retorno de sucesso.
10. O controller monta a resposta 201 e Jackson serializa o DTO em JSON.

Exemplo ilustrativo de resposta; ID e horário reais podem ser diferentes:

```json
{
  "id": 1,
  "descricao": "Mercado",
  "valor": 200.00,
  "categoria": "ALIMENTACAO",
  "categoriaRotulo": "Alimentação",
  "tipo": "DESPESA",
  "dataHora": "2026-10-07T09:00:00"
}
```

## 13. Pasta test: o que os testes provam

`TransacaoServiceTest` tem oito testes unitários, todos aprovados na execução feita durante esta análise.

`@ExtendWith(MockitoExtension.class)` habilita Mockito. `@Mock` cria um repository simulado. `@InjectMocks` monta o service com esse mock. Nenhum contexto Spring ou banco H2 é iniciado por essa classe.

`when(...).thenReturn(...)` define o retorno simulado. `thenAnswer(i -> i.getArgument(0))`, usado em `save`, devolve o objeto recebido sem gravar no disco. `verify(repository).delete(...)` verifica uma chamada, não a exclusão de uma linha real. AssertJ fornece as verificações `assertThat` e `assertThatThrownBy`.

| Teste | Comportamento verificado |
|---|---|
| `criarSalvaComOTipoVindoDaCategoria` | A resposta deriva DESPESA da categoria ALIMENTACAO |
| `criarRemoveEspacoEmVoltaDaDescricao` | A descrição perde espaços nas pontas |
| `buscarUmIdQueNaoExisteEstoura404` | O service lança a exceção de recurso não encontrado |
| `atualizarTrocaOsCamposDoLancamentoExistente` | A resposta reflete os campos alterados |
| `apagarRemoveOLancamentoEncontrado` | O service chama delete com a entidade encontrada |
| `oResumoSomaReceitasDespesasESaldo` | Os totais e o saldo são calculados corretamente |
| `oResumoAgrupaGastosDoMaiorParaOMenorEIgnoraReceitas` | Agrupamento, ordenação e exclusão de receitas da lista de gastos |
| `oResumoDeUmOrcamentoVazioEZeroENaoUmErro` | Orçamento vazio devolve zero e lista vazia |

Apesar de citar "404" no nome, o teste de busca verifica a exceção Java. O status HTTP 404 é responsabilidade do `ApiExceptionHandler`, que esse teste não executa. Os testes também não comprovam geração de IDs, SQL, persistência em disco, validação via HTTP ou comportamento transacional com Spring.

## 14. Roteiro prático de estudo sem frontend

Leia nesta ordem: `TipoTransacao` -> `Categoria` -> `Transacao` -> DTOs -> repository -> service -> controller -> exceptions -> configuração/POM -> testes. Assim você aprende primeiro quais dados existem e depois como eles circulam.

Para executar, num terminal PowerShell com JDK compatível instalado:

```powershell
Set-Location 'C:\Users\thede\OneDrive\Desktop\PROJETOS\Prova-PROJETO-DE-PROGRAMACAO'
.\mvnw.cmd spring-boot:run
```

O Maven Wrapper pode baixar o Maven e bibliotecas que ainda não estiverem no cache. Se já houver uma aplicação na porta 8081 ou outro processo usando o mesmo arquivo H2, resolva esse conflito antes de iniciar outra instância.

Com a aplicação iniciada, use a documentação em [Swagger UI](http://localhost:8081/swagger-ui.html). Não é necessário usar o frontend para chamar a API.

Para visualizar o banco, abra [H2 Console](http://localhost:8081/h2-console) e preencha:

```text
Driver Class: org.h2.Driver
JDBC URL: jdbc:h2:file:~/.orcamento/dados
User Name: sa
Password: deixar vazio
```

Use exatamente a URL configurada. Uma URL como `jdbc:h2:mem:testdb` apontaria para outro banco. O console é uma ferramenta para consultar o banco, não o responsável por armazenar os dados.

Consulta de estudo, somente leitura:

```sql
SELECT id, descricao, valor, categoria, data_hora
FROM transacao
ORDER BY data_hora DESC;
```

Exercícios para executar num ambiente de estudo, pois cadastro e atualização alteram o banco configurado:

1. Consulte `/api/categorias` e compare com `Categoria.java`. Identifique por que não há consulta ao repository.
2. Cadastre Mercado por 200 em ALIMENTACAO. Compare request, response e linha da tabela.
3. Cadastre Salário por 3000 em SALARIO. Consulte `/api/resumo`: sem outros lançamentos, o saldo esperado é 2800.
4. Atualize Mercado para 250. Observe que o ID e a data de criação permanecem; o saldo passa a 2750 se só existirem esses dois lançamentos.
5. Envie valor zero. Siga o caminho `@Valid` -> handler -> 400.
6. Busque um ID inexistente. Siga `findById` -> Optional vazio -> exceção -> handler -> 404.
7. Pare e reinicie a aplicação normalmente. Consulte os lançamentos para observar a persistência em arquivo.

Para executar os testes existentes:

```powershell
.\mvnw.cmd -B -ntp test
```

Para gerar o JAR e depois executá-lo:

```powershell
.\mvnw.cmd package
java -jar target/orcamento-1.0.0.jar
```

## 15. Perguntas para conferir se você entendeu

| Pergunta | Resposta |
|---|---|
| Qual classe recebe o POST? | `TransacaoController`, método `criar` |
| Quem remove os espaços da descrição? | `TransacaoService`, com `trim()` |
| Quem valida valor positivo no fluxo HTTP? | Bean Validation ativada por `@Valid`, com a restrição do request |
| Quem decide receita ou despesa? | A categoria, por `getTipo()` |
| Qual classe vira tabela? | `Transacao`, anotada com `@Entity` |
| DTO vira tabela? | Não; ele define o contrato de entrada ou saída |
| Quem implementa a interface do repository? | Spring Data JPA em execução |
| Quem traduz o mapeamento de objetos em SQL? | Hibernate |
| Quem executa e armazena os dados? | H2 |
| Qual é o arquivo do banco encontrado nesta máquina? | `C:\Users\thede\.orcamento\dados.mv.db` |
| O resumo fica salvo numa tabela? | Não, é calculado pelo service |
| Reiniciar obrigatoriamente apaga os lançamentos? | Não, a URL atual usa arquivo persistente |
| Os oito testes usam esse arquivo? | Não, usam um repository simulado |
| Existe separação dos lançamentos por usuário? | Não existe no modelo ou nas consultas atuais |

Você deve conseguir explicar um cadastro apontando, nesta ordem: endpoint -> request validado -> service -> entidade -> repository -> Hibernate/JDBC/H2 -> response. Depois repita o raciocínio para consulta, atualização, exclusão e resumo.
