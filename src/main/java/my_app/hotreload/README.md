# HotReload System Documentation / Documentação do Sistema HotReload

This package contains a custom Hot Reload implementation for JavaFX applications running on the Java Module System (JPMS).

Este pacote contém uma implementação customizada de Hot Reload para aplicações JavaFX rodando sobre o Java Module System (JPMS).

---

## English Documentation

### Architecture

The system observes file changes, recompiles modified strings, and reloads the UI without restarting the JVM.

#### Core Components

1.  **HotReload.java**: The main controller. It runs a `WatchService` loop to monitor `.java` and resource files.
2.  **HotReloadClassLoader.java**: A custom `ClassLoader` that isolates reloadable classes. It prioritizes loading classes from the file system (target/classes) over the parent classloader for non-excluded classes.
3.  **UIReloaderImpl.java**: The bridge between the reload logic and the JavaFX Application thread. It implements `Reloader` and is instantiated via reflection in the new ClassLoader context.

### Key Features & Solved Challenges

#### 1. Dynamic Module Path Detection

Running a modular application requires `javac` to know the module path. Since we are invoking the system Java compiler (`ToolProvider.getSystemJavaCompiler()`) at runtime, we must pass the same `--module-path` arguments that were used to launch the application.

**Solution:**
We use `ManagementFactory.getRuntimeMXBean().getInputArguments()` to inspect the JVM launch arguments. The code parses these arguments to find:
- `--module-path` / `-p` flags.
- `--module-path=...` style arguments.

These paths are combined and passed to the internal `javac` call, ensuring that the compiler can find external modules (like JavaFX, Jackson, Ikonli) during hot reload.

#### 2. Event Batching (Double Reload Fix)

File systems often emit multiple events for a single "Save" operation (e.g., MODIFY + MODIFY). This previously caused the application to reload twice rapidly.

**Solution:**
The `watchLoop` implements a **debounce mechanism**:
1.  Waits for the first event.
2.  Sleeps for a short duration (`WATCHER_TIMEOUT_MS`).
3.  Drains all subsequent events that occurred during the sleep.
4.  Processes the unique set of changed files in a single batch.
5.  Triggers compilation and reload only once per batch.

#### 3. Module Visibility & Reflection

Libraries like Jackson (for JSON) and the HotReload mechanism itself require reflective access to the application's private members.

**Requirement:**
The `module-info.java` must explicitly `open` packages to allow this access.
```java
module my.app {
    // ...
    requires java.management; // Required for RuntimeMXBean access
    
    // Allows HotReload and Jackson to access internal data
    opens my_app.data;
    opens my_app.hotreload;
    // ...
}
```

#### 4. Class Loading Strategy

To reload code, we cannot "unload" a class from the running ClassLoader. Instead, we:
1.  Create a **new** `HotReloadClassLoader` for every reload cycle.
2.  Load the entry point class (`UIReloaderImpl`) using this new loader.
3.  Pass the `Stage` context to the new instance.
4.  The new instance loads the updated versions of `AppScenes` and other UI components.

### Usage

The `HotReload` instance is initialized in `App.start()`. It requires:
- Source path (`src/main/java`)
- Output path (`target/classes`)
- Resources path (`src/main/resources`)
- A set of "Excluded Classes" (like `App` itself) that should **not** be reloaded (or cause a full restart if changed).

---

## Documentação em Português (Brasil)

### Arquitetura

O sistema observa mudanças em arquivos, recompila strings modificadas e recarrega a UI sem reiniciar a JVM.

#### Componentes Principais

1.  **HotReload.java**: O controlador principal. Executa um loop de `WatchService` para monitorar arquivos `.java` e recursos.
2.  **HotReloadClassLoader.java**: Um `ClassLoader` customizado que isola classes recarregáveis. Ele prioriza o carregamento de arquivos do sistema (target/classes) em vez do classloader pai para classes não excluídas.
3.  **UIReloaderImpl.java**: A "ponte" entre a lógica de recarga e a Thread de Aplicação JavaFX. Implementa `Reloader` e é instanciada via reflection no contexto do novo ClassLoader.

### Funcionalidades Chave & Desafios Resolvidos

#### 1. Detecção Dinâmica do Module Path

Rodar uma aplicação modular exige que o `javac` conheça o module path. Como invocamos o compilador Java do sistema (`ToolProvider.getSystemJavaCompiler()`) em tempo de execução, precisamos passar os mesmos argumentos `--module-path` usados para iniciar a aplicação.

**Solução:**
Usamos `ManagementFactory.getRuntimeMXBean().getInputArguments()` para inspecionar os argumentos de lançamento da JVM. O código analisa esses argumentos para encontrar:
- Flags `--module-path` / `-p`.
- Argumentos no estilo `--module-path=...`.

Esses caminhos são combinados e passados para a chamada interna do `javac`, garantindo que o compilador encontre módulos externos (como JavaFX, Jackson, Ikonli) durante o hot reload.

#### 2. Agrupamento de Eventos (Correção de Duplo Reload)

Sistemas de arquivos frequentemente emitem múltiplos eventos para uma única operação de "Salvar" (ex: MODIFY + MODIFY). Isso anteriormente causava recargas duplas rápidas na aplicação.

**Solução:**
O `watchLoop` implementa um **mecanismo de debounce**:
1.  Aguarda o primeiro evento.
2.  Dorme por uma curta duração (`WATCHER_TIMEOUT_MS`).
3.  Drena todos os eventos subsequentes que ocorreram durante o sono.
4.  Processa o conjunto único de arquivos alterados em um único lote.
5.  Dispara a compilação e recarga apenas uma vez por lote.

#### 3. Visibilidade de Módulos & Reflection

Bibliotecas como Jackson (para JSON) e o próprio mecanismo de HotReload requerem acesso via reflection aos membros privados da aplicação.

**Requisito:**
O `module-info.java` deve explicitamente "abrir" (`opens`) pacotes para permitir este acesso.
```java
module my.app {
    // ...
    requires java.management; // Necessário para acesso ao RuntimeMXBean
    
    // Permite que HotReload e Jackson acessem dados internos
    opens my_app.data;
    opens my_app.hotreload;
    // ...
}
```

#### 4. Estratégia de Class Loading

Para recarregar código, não podemos "descarregar" uma classe do ClassLoader em execução. Em vez disso, nós:
1.  Criamos um **novo** `HotReloadClassLoader` a cada ciclo de recarga.
2.  Carregamos a classe de ponto de entrada (`UIReloaderImpl`) usando este novo loader.
3.  Passamos o contexto do `Stage` para a nova instância.
4.  A nova instância carrega as versões atualizadas de `AppScenes` e outros componentes de UI.

### Uso / Usage

A instância de `HotReload` é inicializada em `App.start()`. Ela usa method chaining:

```java
boolean devMode = true;
if (devMode) {
    new HotReload()
        .sourcePath("src/main/java")
        .classesPath("build/classes/java/main")
        .resourcesPath("src/main/resources")
        .implementationClassName("my_app.hotreload.UIReloaderImpl")
        .screenClassName("my_app.screens.HomeScreen")
        .reloadContext(context)
        .classesToExclude(Set.of(
            "my_app.Main",
            "my_app.hotreload.Reloader",
            "my_app.hotreload.UIReloaderImpl",
            "my_app.hotreload.HotReload",
            "my_app.hotreload.HotReloadClassLoader"
        ))
        .start();
}
```

#### Parâmetros / Parameters

| Método | Descrição |
|--------|-----------|
| `sourcePath` | Caminho para os arquivos .java fonte |
| `classesPath` | Caminho para os arquivos .class compilados |
| `resourcesPath` | Caminho para os recursos |
| `implementationClassName` | Classe que implementa `Reloader` |
| `screenClassName` | Classe da screen principal a ser recarregada |
| `reloadContext` | Contexto (objeto context do MegalodonteApp) |
| `classesToExclude` | Classes que não devem ser recarregadas |
| `addExclude()` | Adicionar uma classe à lista de exclusão |

### Credits

Desenvolvido por [Eliezer Software Engineer](https://github.com/eliezer-software-enginner)
