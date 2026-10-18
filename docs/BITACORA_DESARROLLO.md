# Bitácora de desarrollo

Registro cronológico del avance del proyecto: qué se hizo en cada etapa, qué se decidió y por qué,
qué problemas aparecieron y cómo se resolvieron.

> **No confundir con la bitácora del simulador**, que es el panel de texto de la interfaz donde se
> registra lo que ocurre en cada tick de la simulación. Este archivo es la bitácora del *equipo*.

## Para qué sirve

1. **Defender el proyecto.** Cada decisión técnica queda con su razón y con la alternativa que se
   descartó. Es el material de respaldo para las preguntas del profesor.
2. **Coordinar al equipo.** Cristian y Kevin trabajan en paralelo; leyendo la última entrada se sabe
   en qué estado quedó el proyecto sin tener que revisar el código.
3. **Armar el video y el documento explicativo.** Al final de la semana, el guion sale de aquí.

## Cómo agregar una entrada

Las entradas van en **orden inverso**: la más reciente arriba. Al cerrar una etapa se agrega una
entrada nueva con este formato:

```markdown
## Etapa N — AAAA-MM-DD — Título corto
**Responsable:** quién lo hizo
### Hecho
### Decisiones
### Incidencias
### Verificación
### Pendiente
```

Secciones que no apliquen se omiten. Las decisiones que **cambien el diseño** acordado en
`CLAUDE.md` van además en la tabla de la sección [Desviaciones respecto al diseño](#desviaciones-respecto-al-diseño).

---

# Entradas

## Etapa 1 — 2026-10-18 — Las dos estructuras con sus pruebas

**Responsable:** Kevin, con asistencia de Claude Code.

### Hecho

- **`estructuras/ColaCircular<T>`**, la única estructura implementada por el equipo. Arreglo de
  capacidad fija, con puntero al frente y el final calculado como `(frente + tamaño) % capacidad`.
  Encolar, desencolar y consultar son O(1) incluso al sobrescribir. Incluye las consultas de solo
  lectura que necesita la vista: capacidad, tamaño, índices de frente y final, contenido de una
  celda física (que puede estar vacía), elementos en orden lógico y contador de sobrescrituras.
- **`modelo/Prioridad`**, enum de los cuatro niveles con su descripción, su acción y su color.
- **`modelo/OrdenTrabajo`**, `record` inmutable con la orden ya clasificada.
- **`modelo/ColaPrioridadOrdenes`**, arreglo de cuatro `ArrayDeque` indexado por nivel. Insertar es
  O(1) y extraer O(k) con k = 4.
- **45 pruebas JUnit** en dos archivos, agrupadas con `@Nested` y descritas con `@DisplayName` en
  español, de modo que el informe de pruebas se lea como una lista de propiedades de cada
  estructura.

### Decisiones

| Decisión | Alternativa descartada | Razón |
|---|---|---|
| `Prioridad` guarda el color como entero RGB (`0xRRGGBB`) | Guardar un `java.awt.Color` | El modelo no puede importar `java.awt`. El modelo define **qué** color corresponde a cada prioridad y la vista lo convierte, así que la decisión de diseño se conserva sin romper la arquitectura |
| `OrdenTrabajo` guarda el índice de la máquina en el inventario, más su etiqueta | Guardar una referencia a `Maquina` | El índice recupera la máquina del `ArrayList` en O(1), que es precisamente la razón por la que el inventario es un arreglo dinámico. La etiqueta deja la orden legible por sí sola en la vista y en la bitácora. Además evita una referencia circular entre máquina y orden, y permite que la orden sea inmutable |
| `OrdenTrabajo` es un `record` | Una clase mutable | Una orden de trabajo es un documento histórico: una vez emitida no cambia. Lo que cambia es su ubicación, y de eso se encargan las estructuras. Al ser inmutable se puede entregar a la vista sin riesgo |
| Un bloque estático en `Prioridad` verifica que `nivel == ordinal()` | Confiar en que nadie reordene las constantes | `ColaPrioridadOrdenes` usa el nivel como índice de su arreglo. Si alguien reordena el enum y olvida corregir los niveles, el programa falla al cargar la clase en vez de asignar las órdenes al carril equivocado |
| `ColaCircular` rechaza elementos nulos | Aceptarlos | Una celda nula es la forma de representar "celda vacía" para el panel. Aceptar nulos volvería ambigua esa lectura |
| `tamano()` de la cola con prioridad suma los cuatro niveles | Mantener un contador aparte | Sumar cuatro números es O(k) con k = 4, y evita sostener un dato que podría quedar desincronizado con las colas |
| Se agregó `verMasReciente()` a `ColaCircular` | Dejar solo `verFrente()` | El paso 3 del tick compara contra los umbrales **la última** lectura, no la más antigua. Es O(1) por aritmética de índices |

### Verificación

| Comando | Resultado |
|---|---|
| `./mvnw test` | BUILD SUCCESS — **45 pruebas, 0 fallos** |
| `./mvnw javadoc:javadoc` | BUILD SUCCESS, sin advertencias |

Casos obligatorios cubiertos, según la sección 5 del documento de diseño:

- **`ColaCircular`:** cola vacía, llenado exacto, sobrescritura, varias vueltas completas,
  desencolar después de dar la vuelta y capacidad 1. Se agregaron además el rechazo de capacidad
  inválida y de nulos, el acceso a celdas fuera de rango, y la comprobación de que las listas que
  recibe la vista son copias no modificables.
- **`ColaPrioridadOrdenes`:** orden por prioridad, FIFO dentro del mismo nivel, una P1 insertada de
  última que sale primero, y extraer con la estructura vacía. Se agregó el contrato del enum
  `Prioridad`, del que depende el índice del arreglo.

### Pendiente

- Etapa 2: resto de las entidades del modelo (`Sensor`, `Lectura`, `Maquina`, `Anomalia`,
  `EventoFallo`, `EstadoMaquina`, `Tecnico`) y la clase de configuración centralizada.
- Cristian debe definir los valores reales de máquinas, sensores y umbrales.
- Queda por aprobar la decisión de que `OrdenTrabajo` guarde el índice de la máquina en vez de la
  referencia; está registrada en la tabla de desviaciones.

---

## Etapa 0 — 2026-10-18 — Cimientos del proyecto

**Responsable:** Kevin, con asistencia de Claude Code.

**Punto de partida:** el proyecto era el esqueleto de plantilla de IntelliJ IDEA (un `Main.java` con
"Hello and welcome!", `pom.xml` sin dependencias) y el repositorio no tenía ningún commit.

### Hecho

- **`pom.xml` reescrito.** Coordenadas `edu.unal.planta:simulador-planta:1.0-SNAPSHOT`,
  `maven.compiler.release=21`, JUnit 5.14.4 en `scope test` como única dependencia, y versiones de
  todos los plugins fijadas (compiler 3.14.1, surefire 3.6.0, jar 3.4.2, javadoc 3.12.0).
- **Jar ejecutable.** `maven-jar-plugin` declara `edu.unal.planta.App` en el manifiesto. Como no hay
  dependencias de ejecución, basta un jar simple: `java -jar target/simulador-planta.jar`.
- **Javadoc configurado** en locale `es`, para cubrir el entregable de documentación en línea de
  código.
- **Eliminado** `src/main/java/org/example/Main.java`, la plantilla de IntelliJ.
- **Creados los cinco paquetes** de la sección 4 de `CLAUDE.md`, cada uno con su `package-info.java`
  documentando la regla de arquitectura que lo gobierna (por qué `estructuras` no conoce el dominio,
  por qué `modelo` no importa Swing, por qué el reloj es `javax.swing.Timer` y no `Thread`, por qué
  hay acciones que no se pueden deshacer).
- **`App.java`** provisional y documentado; se reemplaza por el armado real del MVC en la etapa 5.
- **Documentación del repositorio:** `README.md` completo, `docs/USO_IA.md` con el registro por
  archivo, y los índices de `docs/MANUAL_USUARIO.md` y `docs/DOCUMENTO_EXPLICATIVO.md` para que el
  contenido se redacte sobre una estructura ya acordada.
- **Maven Wrapper** 3.3.4 apuntando a Maven 3.9.16.
- **Rama** `master` renombrada a `main`.

### Decisiones

| Decisión | Alternativa descartada | Razón |
|---|---|---|
| Compilar con `maven.compiler.release=21` | `source` + `target`, o dejar Java 25 | `CLAUDE.md` fija Java 21 LTS. A diferencia de `source`/`target`, `release` **prohíbe en tiempo de compilación** cualquier API posterior a 21, así que no se puede colar algo de Java 25 que luego falle en otra máquina |
| JUnit **5**.14.4 | JUnit 6.0.3 | Era la única versión en el repositorio local de Maven, pero el enunciado y el diseño piden JUnit 5. Se descargó la última 5.x |
| Agregar el Maven Wrapper (`mvnw`) | Exigir Maven instalado | Permite construir el proyecto con `./mvnw test` sin instalar nada. Se generó del tipo *script*, que no deja ningún `.jar` binario en el repositorio |
| Plugins de `jar` y `javadoc` en el `pom.xml` | Generar el jar y el Javadoc a mano desde el IDE | Son herramientas de construcción de Maven, no librerías que el programa use al ejecutarse. El proyecto sigue teniendo **una sola dependencia**: JUnit 5 |
| Ignorar `.idea/` completa en `.gitignore` | Versionar algunos archivos del IDE, como venía por defecto | La configuración del IDE es local de cada quien; versionarla genera conflictos entre las dos máquinas del equipo |
| Renombrar la rama a `main` | Dejar `master` | El remoto en GitHub usa `main` por defecto. Hacer push como `master` habría dejado dos ramas y la portada del repositorio mostrando la vacía |
| Dos ramas: `main` y `desarrollo`, integrando con `--no-ff` | Trabajar todo sobre `main` | `main` debe compilar siempre, porque es la rama que se entrega y la que el profesor va a clonar. El trabajo en curso vive en `desarrollo` y solo se integra cuando `./mvnw test` pasa completo. El merge sin *fast-forward* deja en la historia un commit que marca cada etapa integrada |
| `package-info.java` en cada paquete | Carpetas vacías con `.gitkeep` | Git no versiona carpetas vacías, y de paso cada archivo es una página real de Javadoc donde queda escrita la regla de arquitectura del paquete |

### Incidencias

| Problema | Solución |
|---|---|
| `mvn` no está instalado en el sistema, solo el que trae IntelliJ | Se agregó el Maven Wrapper al proyecto |
| El JDK 21 instalado (`java-21-openjdk`) es solo JRE, no trae `javac` | Se compila con el JDK 25 del sistema apuntando a `release=21`. El bytecode resultante es de Java 21, verificado con `javap` |
| La plantilla de IntelliJ usaba características de Java 25 (`IO.println`, `main()` sin argumentos) | El archivo se eliminó por completo |

### Verificación

| Comando | Resultado |
|---|---|
| `./mvnw test` | BUILD SUCCESS, 2 pruebas, 0 fallos *(prueba temporal de cableado, eliminada después)* |
| `javap -verbose` sobre `App.class` | `major version: 65`, es decir Java 21 |
| `java -jar target/simulador-planta.jar` | Arranca e imprime correctamente |
| `./mvnw javadoc:javadoc` | BUILD SUCCESS, sin advertencias |

El cableado de JUnit se comprobó con una prueba desechable que se eliminó enseguida: una
aserción `2 + 2 == 4` no aporta nada a un repositorio calificado. Las pruebas reales entran en la
etapa 1, junto con las estructuras.

### Pendiente

- Primer commit del repositorio (17 archivos sin versionar).
- Etapa 1: `ColaCircular<T>` y `ColaPrioridadOrdenes` con sus pruebas.
- Cristian debe definir los valores reales de máquinas, sensores y umbrales (sección 6 de
  `CLAUDE.md`). Irán todos en una única clase de configuración.

---

# Desviaciones respecto al diseño

`CLAUDE.md` es la fuente de verdad del diseño y exige que todo cambio de decisión se registre allí
**antes** de implementarlo. Esta tabla lleva la cuenta de esos cambios y de las propuestas que
todavía no se han aprobado.

| Fecha | Sección de `CLAUDE.md` | Cambio propuesto | Razón | Estado |
|---|---|---|---|---|
| 2026-10-18 | 7, paso 7 (preventivo) | Marcar en `Maquina` un `boolean` de preventiva pendiente, en vez de recorrer el carril P4 de la cola con prioridad para saber si ya hay una | Recorrer el carril es O(n) y obliga a inspeccionar la cola desde afuera. El indicador es O(1) y respeta mejor el tipo abstracto de datos | **Propuesta**, sin implementar |
| 2026-10-18 | 6, campo «máquina» de `OrdenTrabajo` | La orden guarda `indiceMaquina` y `etiquetaMaquina` en vez de una referencia a `Maquina` | Recupera la máquina del inventario en O(1), evita la referencia circular máquina↔orden y permite que la orden sea inmutable | **Implementada**, pendiente de revisión del equipo |
| 2026-10-18 | 6, campo «color» de `Prioridad` | El color se guarda como entero `0xRRGGBB`, no como `java.awt.Color` | La sección 4 prohíbe que el modelo importe `java.awt`. El modelo define el color y la vista lo convierte | **Implementada**; resuelve una contradicción entre las secciones 4 y 6 |

---

# Control de etapas

Orden obligatorio de trabajo según la sección 11 de `CLAUDE.md`:
**estructuras → modelo probado → vista → controlador.**

| Etapa | Contenido | Estado |
|---|---|---|
| 0 | Cimientos: Maven, paquetes, JUnit, documentación base | **Terminada** (2026-10-18) |
| 1 | `ColaCircular<T>` y `ColaPrioridadOrdenes`, con pruebas | **Terminada** (2026-10-18) |
| 2 | Resto de las entidades del modelo y clase de configuración. `Prioridad` y `OrdenTrabajo` ya quedaron hechas en la etapa 1, porque la cola con prioridad las necesitaba | Pendiente |
| 3 | `GeneradorFallos`, `Clasificador`, comandos y `Planta.tick()` probado sin interfaz | Pendiente |
| 4 | Interfaz Swing: los seis paneles | Pendiente |
| 5 | Controlador, reloj e integración | Pendiente |
| 6 | Manual de usuario, documento explicativo, cierre de `USO_IA.md` y guion del video | Pendiente |
