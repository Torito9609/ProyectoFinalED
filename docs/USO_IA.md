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

### Etapa 2 — Entidades del modelo

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/EstadoMaquina.java`, `modelo/Magnitud.java` | IA | Estados y magnitudes tomados de las secciones 6 y 7 |
| `modelo/Sensor.java`, `modelo/Lectura.java`, `modelo/Anomalia.java`, `modelo/EventoFallo.java` | IA | Campos tomados de la sección 6 |
| `modelo/Maquina.java`, `modelo/Tecnico.java` | IA | Revisadas por el equipo |
| `estructuras/ColaCircularSoloLectura.java` | Mixto | **Interfaz pedida por Kevin** para que la vista no pueda modificar la cola; implementada por IA |
| `modelo/ConfiguracionPlanta.java` | Mixto | Estructura generada por IA; **los valores de máquinas, sensores y umbrales los define Cristian** |
| `test/.../ConfiguracionPlantaTest.java` | IA | Tres pruebas de las condiciones de la sección 6 |

### Etapa 3 — Simulación

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/ObservadorPlanta.java` | IA | Patrón Observador, exigido por la sección 4 |
| `modelo/ColaPrioridadSoloLectura.java` | Mixto | **Interfaz pedida por Kevin**, igual que la de la cola circular |
| `modelo/GeneradorFallos.java` | IA | Pesos y fórmula tomados de la sección 7 |
| `modelo/Clasificador.java` | IA | Reglas de umbrales tomadas de la sección 7 |
| `modelo/comandos/*.java` | IA | Patrón Comando, según la sección 8 |
| `modelo/Planta.java` | IA | Los ocho pasos del tick, en el orden de la sección 7 |
| `test/.../PlantaTest.java` | IA | Seis pruebas de la simulación completa |

### Ajuste del inventario a 6 máquinas

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/ConfiguracionPlanta.java` | Mixto | **Decisión de Kevin**; las tres máquinas nuevas y sus sensores los redactó la IA y los valores quedan sujetos a revisión de Cristian |
| `CLAUDE.md` (secciones 2, 6 y 10) | Mixto | Actualizado para reflejar la decisión del equipo |
| `test/.../ConfiguracionPlantaTest.java` | IA | Prueba de que cada magnitud cae en su zona de umbrales |

### Calibración de la simulación

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/Planta.java` (paso 5) | Mixto | **Decisión de Kevin** (opción 2); implementada por IA |
| `modelo/Maquina.java` (techo de desgaste) | IA | Hallazgo de la medición; revisado por el equipo |
| `modelo/ConfiguracionPlanta.java` (duraciones) | IA | Calibrado midiendo tres variantes; las tasas de fallo siguen siendo de Cristian |
| `CLAUDE.md` (sección 7, pasos 1 y 5) | Mixto | Actualizado para reflejar las decisiones |

### Etapa 4 — Paneles de la vista

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `vista/Colores.java`, `vista/SeleccionVista.java` | IA | Paleta y estado de selección |
| `vista/PanelEstructura.java` | IA | Clase base con método plantilla |
| `vista/Panel*.java` (7 paneles) | IA | Diseño acordado con Kevin antes de implementar; revisados sobre renders PNG |
| `vista/VentanaPrincipal.java` | IA | Distribución de la sección 9 del documento de diseño |
| `test/.../PanelesVistaTest.java` | IA | Una prueba: que ningún panel falle al dibujarse |
| `docs/img/ventana.png` | — | Captura generada del programa |

### Etapa 5 — Controlador e integración

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `controlador/ControladorPlanta.java` | IA | Reloj y acciones del operador, según la sección 8 |
| `vista/AccionesOperador.java` | IA | Frontera entre vista y controlador |
| `vista/PanelControlesMaquina.java` | IA | Controles de la sección 9 |
| `vista/VentanaPrincipal.java`, `vista/PanelInventario.java`, `vista/PanelPila.java` | IA | Interacción agregada sobre los paneles de la etapa 4 |
| `App.java` | IA | Armado final del patrón MVC |
| `test/.../ControladorPlantaTest.java` | IA | Cuatro pruebas de las acciones del operador |

### Manual de usuario

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `docs/MANUAL_USUARIO.md` | IA | Redactado sobre el índice acordado por el equipo; nombres de botones, colores y mensajes tomados del código de la vista y del modelo. Pendiente de revisión del equipo |
| `docs/img/manual/*.png` (15 capturas) | — | Generadas dibujando la ventana real del programa con la semilla 7; el programa que las genera no forma parte del repositorio |

### Documento explicativo

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `docs/DOCUMENTO_EXPLICATIVO.md` | IA | Redactado sobre el índice acordado y las decisiones de las secciones 5, 7 y 8 de `CLAUDE.md` y de `BITACORA_DESARROLLO.md`; fragmentos de código copiados del código real. Pendiente de revisión del equipo |

### Bloqueo de la inyección sobre un fallo activo (26 de octubre)

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/Planta.java` (`inyectarFallo`) | Mixto | **Decisión de Kevin**, tras reproducir con IA los casos en que un fallo reemplazaba a otro; implementada por IA |
| `vista/PanelControlesMaquina.java` (botón Inyectar) | Mixto | **Decisión de Kevin** de restringirlo también desde la interfaz; implementada por IA |
| `test/.../PlantaTest.java` | IA | Dos pruebas: una severa no se pierde detrás de una leve, y una máquina con alarma no acepta otro fallo |

### Entrega 2 — Commit 1: decisión y diseño (5 de noviembre)

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `CLAUDE.md` (secciones 1, 3, 4, 9–13, 15, 16 y 17) | Mixto | **Decisiones del equipo:** migrar a Spring Boot, frontend aparte, árboles, overhaul con técnicos fijos del fabricante, fallo ponderado y modo manual. La IA propuso las estructuras y las alternativas descartadas, que el equipo aprobó, y redactó el texto |
| `docs/BITACORA_DESARROLLO.md` | IA | Entrada de la etapa A, desviaciones y control de etapas |

### Entrega 2 — Commit 2: proyecto Spring Boot

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `pom.xml` | IA | Padre de Spring Boot 4.1.2 (última estable, verificada en Maven Central), los dos starters permitidos por la sección 3 y el plugin de empaquetado |
| `AplicacionPlanta.java` | IA | Punto de entrada de Spring |
| `test/.../AplicacionPlantaTest.java` | IA | Prueba de humo: el contexto carga |

### Entrega 2 — Commit 3: retirar la interfaz Swing

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `App.java`, `vista/`, `controlador/`, `PanelesVistaTest`, `ControladorPlantaTest` | — | Eliminados; se conservan en la etiqueta `entrega-1` |
| `package-info.java` (raíz) | IA | Describe los paquetes `estructuras`, `modelo`, `servicio` y `api` |
| Javadoc de `ObservadorPlanta`, `EstadoMaquina`, `Prioridad`, `Planta`, `modelo/package-info` y `ColaCircular` | IA | Solo texto: ya no hablan de Swing ni del hilo de eventos. La lógica no cambia |
| `pom.xml` | IA | Se quita la `mainClass` explícita: queda un solo `main` |
| `README.md` | IA | Árbol de paquetes y nota que remite a la etiqueta `entrega-1` |

### Entrega 2 — Commit 4: servicio de simulación

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `servicio/ServicioSimulacion.java` | IA | Dueño único de la `Planta`; métodos `synchronized`, reloj con `@Scheduled` |
| `servicio/package-info.java` | IA | Documentación del paquete |
| `src/main/resources/application.properties` | IA | Semilla e intervalo del reloj |
| `AplicacionPlanta.java` | IA | Se agrega `@EnableScheduling` |
| `ConfiguracionPlanta.java` | IA | Se quita `MILIS_POR_TICK`: el intervalo pasa a `application.properties` |
| `test/.../ServicioSimulacionTest.java` | IA | 5 pruebas sin HTTP; el reloj se dispara a mano |

### Entrega 2 — Commit 5: DTOs del estado de la planta

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `api/dto/` (10 `record`s y `package-info`) | IA | Copias inmutables del modelo: máquina, sensor, lectura, cola circular (orden físico, `null` en celdas vacías), eventos, órdenes, técnicos, carriles, cola con prioridad y estado completo |
| `servicio/ServicioSimulacion.java` | IA | Se agrega `estadoPlanta()`, `synchronized`, que arma la foto dentro del candado |
| `test/.../EstadoPlantaDtoTest.java` | IA | 4 pruebas: es una copia, listas no modificables, cola circular fiel y cuatro carriles |
| `test/.../ServicioSimulacionTest.java` | IA | Una prueba más: la foto refleja tick, semilla y ejecución |

### Entrega 2 — Commit 6: endpoints de control y consulta

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `api/ControladorSimulacion.java` | IA | `GET /api/planta`, `GET /api/simulacion` y `POST` iniciar, pausar, paso y reiniciar; sin lógica |
| `api/package-info.java` | IA | Documentación del paquete |
| `api/dto/EstadoSimulacionDto.java` | IA | Tick, semilla y ejecución |
| `servicio/ServicioSimulacion.java` | IA | Las acciones de control devuelven su `EstadoSimulacionDto`, armado dentro del mismo método `synchronized`; nuevo `estadoSimulacion()` |
| `test/.../ControladorSimulacionTest.java` | IA | 5 pruebas MockMvc con el servicio real |

### Entrega 2 — Commit 7: endpoints de acciones del operador

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `api/ControladorOperador.java` | IA | Inyectar fallo, parar o arrancar, cambiar carga y deshacer; sin lógica |
| `api/ManejadorErrores.java` | IA | `@RestControllerAdvice`: 400, 404 y 409 con `ErrorDto` |
| `api/dto/InyeccionFalloDto.java`, `CargaDto.java`, `ErrorDto.java` | IA | Cuerpos de petición y de error |
| `servicio/NoEncontradoException.java`, `AccionNoPermitidaException.java` | IA | Excepciones que el manejador traduce a 404 y 409 |
| `servicio/ServicioSimulacion.java` | IA | Acciones del operador con sus validaciones; el rechazo se sigue registrando en la bitácora |
| `test/.../ControladorOperadorTest.java` | IA | 7 pruebas MockMvc: camino feliz, 400, 404, 409 y deshacer |

### Entrega 2 — Commit 8: CORS y contrato de la API

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `api/ConfiguracionCors.java` | IA | `GET`, `POST` y `PUT` en `/api/**` desde orígenes locales |
| `src/main/resources/application.properties` | IA | Propiedad `planta.cors.origenes` |
| `test/.../ConfiguracionCorsTest.java` | IA | 2 pruebas MockMvc: origen local permitido y origen externo rechazado |
| `docs/API.md` | IA | Contrato para el frontend; los ejemplos de respuesta son salidas reales del servidor |
| `README.md` | IA | Cómo ejecutar el backend, enlace a la API, estado de la Entrega 2 |

### Entrega 2 — Commit 10: árbol de segmentos

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `estructuras/ArbolSegmentos.java` | IA | Generada contra la API que aprobó el equipo: arreglos paralelos de tamaño 2·m, actualización iterativa y consultas recursivas |
| `estructuras/package-info.java` | IA | Menciona la nueva estructura propia |
| `test/.../ArbolSegmentosTest.java` | IA | 4 pruebas del plan sobre un árbol de tamaño 6 (con hojas de relleno) |

### Etapas siguientes

*(se completa al cerrar cada etapa)*

## 4. Porcentaje estimado

| Componente | Generado con IA | Observación |
|---|---|---|
| Documento de diseño (`CLAUDE.md`) | 0 % en la Entrega 1; mixto desde la Entrega 2 | Las secciones de la Entrega 2 las redactó la IA sobre decisiones del equipo |
| Esqueleto del proyecto (etapa 0) | ~95 % | Configuración de Maven y documentación de paquetes |
| Estructuras de datos y sus pruebas (etapa 1) | ~95 % | Generadas contra una especificación escrita por el equipo, que define capacidad, punteros, complejidades y casos de prueba obligatorios |

**Estimación global del proyecto:** *(se calcula al cerrar la última etapa, sobre el total de líneas
de código fuente)*
