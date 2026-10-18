# Declaración del uso de inteligencia artificial

El enunciado del proyecto permite usar código generado con IA siempre que se declare el porcentaje
correspondiente. Este documento lleva ese registro desde el primer commit y se actualiza al cerrar
cada etapa de trabajo.

**Equipo:** Cristian Robayo y Kevin Toro.

---

## 1. Herramientas utilizadas

| Herramienta | Modelo | Para qué se usó |
|---|---|---|
| Claude Code (CLI) | Claude Opus 5 | Generación de código Java, pruebas JUnit, Javadoc y documentación del repositorio, siempre a partir del diseño definido por el equipo en `CLAUDE.md` |

No se usaron generadores de código adicionales ni plantillas de terceros, más allá del esqueleto
inicial de proyecto Maven que crea IntelliJ IDEA (que fue reemplazado por completo).

## 2. Cómo se trabajó

El diseño del sistema —arquitectura, elección de estructuras de datos, reglas de la simulación y
alcance de la entrega— lo definió el equipo y está escrito en `CLAUDE.md`, que funciona como fuente
de verdad. La IA se usó para **implementar ese diseño**, no para decidirlo: cada clase se genera
contra una especificación previamente acordada y el equipo la revisa antes de darla por terminada.

Trazabilidad en el historial de Git: los commits producidos con asistencia de IA conservan la línea
`Co-Authored-By` que agrega la herramienta. Los commits sin esa línea fueron escritos directamente
por el equipo.

## 3. Registro por archivo

Convención de la columna **Autoría**:

- **Equipo** — escrito a mano por Cristian o Kevin.
- **IA** — generado por la herramienta y revisado por el equipo.
- **Mixto** — generado por la herramienta y modificado de forma sustancial a mano.

### Etapa 0 — Esqueleto del proyecto

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `CLAUDE.md` | Equipo | Documento de diseño, escrito íntegramente por el equipo |
| `pom.xml` | IA | Versiones de Java, JUnit y plugins verificadas contra el documento de diseño |
| `.gitignore` | Mixto | Base generada por IntelliJ, ajustada a mano |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/` | Herramienta | Generados por `maven-wrapper-plugin`, no escritos a mano |
| `src/main/java/edu/unal/planta/App.java` | IA | Punto de entrada provisional |
| `package-info.java` (5 paquetes) | IA | Contenido derivado de las secciones 4 y 5 de `CLAUDE.md` |
| `README.md` | IA | Redactado a partir del documento de diseño |
| `docs/BITACORA_DESARROLLO.md` | IA | Entrada de la etapa 0, redactada sobre el trabajo realmente ejecutado |
| `docs/MANUAL_USUARIO.md` | IA | Solo el índice; el contenido lo escribe el equipo |
| `docs/DOCUMENTO_EXPLICATIVO.md` | IA | Solo el índice; el contenido lo escribe el equipo |
| `docs/USO_IA.md` | IA | Este documento |

### Etapa 1 — Estructuras de datos

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `estructuras/ColaCircular.java` | IA | Implementada contra la especificación de la sección 5 de `CLAUDE.md`; el equipo verificó la aritmética de índices y la complejidad declarada en cada método |
| `modelo/Prioridad.java` | IA | Niveles, acciones y colores tomados de la sección 7 |
| `modelo/OrdenTrabajo.java` | IA | Campos tomados de la sección 6 |
| `modelo/ColaPrioridadOrdenes.java` | IA | Implementada contra la especificación de la sección 5 |
| `test/.../ColaCircularTest.java` | IA | Cubre los seis casos borde exigidos por el documento de diseño |
| `test/.../ColaPrioridadOrdenesTest.java` | IA | Cubre los cuatro casos exigidos por el documento de diseño |
| `docs/BITACORA_DESARROLLO.md` | IA | Entrada de la etapa 1 |

### Etapas siguientes

*(se completa al cerrar cada etapa)*

## 4. Porcentaje estimado

| Componente | Generado con IA | Observación |
|---|---|---|
| Documento de diseño (`CLAUDE.md`) | 0 % | Escrito por el equipo |
| Esqueleto del proyecto (etapa 0) | ~95 % | Configuración de Maven y documentación de paquetes |
| Estructuras de datos y sus pruebas (etapa 1) | ~95 % | Generadas contra una especificación escrita por el equipo, que define capacidad, punteros, complejidades y casos de prueba obligatorios |

**Estimación global del proyecto:** *(se calcula al cerrar la última etapa, sobre el total de líneas
de código fuente)*
