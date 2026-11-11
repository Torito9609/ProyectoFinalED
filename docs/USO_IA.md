# Declaración del uso de inteligencia artificial

El enunciado del proyecto permite usar código generado con IA siempre que se declare el porcentaje
correspondiente. Este documento lleva ese registro desde el primer commit y se actualiza al cerrar
cada etapa de trabajo.

**Equipo:** Cristian Robayo y Kevin Toro.

---

## 1. Herramientas utilizadas

| Herramienta | Modelo | Para qué se usó |
|---|---|---|
| Claude Code (CLI) | Claude Opus 5 (Entrega 1) y Claude Opus 5.5 (Entrega 2) | Generación de código Java, pruebas JUnit, Javadoc y documentación del repositorio, siempre a partir del diseño definido por el equipo en `CLAUDE.md` |

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

### Entrega 2 — Commit 11: peso de fallo, puntaje y recomendación

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/Prioridad.java` | IA | Campo `peso`: P1 = 3, P2 = 2, P3 = 1, P4 = 0 |
| `modelo/Maquina.java` | IA | `puntajeFallos`, `numeroOverhauls` y `recomendacion()`; `sumarPuntaje` y `completarOverhaul` con visibilidad de paquete |
| `modelo/Recomendacion.java` | IA | Enum `NINGUNA` / `OVERHAUL` / `REEMPLAZO` |
| `modelo/ConfiguracionPlanta.java` | IA | `UMBRAL_OVERHAUL = 12`; `MAXIMO_OVERHAULS = 3`, valor que decidió el equipo |
| `test/.../MaquinaTest.java` | IA | 4 pruebas de la recomendación |

### Entrega 2 — Commit 12: ranking de fallos por compañía

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/RankingFallos.java` | IA | `TreeSet` con orden por puntaje descendente y etiqueta; `sumarFallo` (eliminar → sumar → insertar), `enOrden`, `conPuntajeMinimo` y `reconstruir` |
| `modelo/Planta.java` | IA | El paso 4 suma el fallo al ranking; `reiniciar()` reconstruye el ranking; nuevo `rankingFallos()` |
| `test/.../RankingFallosTest.java` | IA | 3 pruebas: reordenar, empate y consulta por umbral |
| `test/.../PlantaTest.java` | IA | 1 prueba: tras 500 ticks el ranking coincide con ordenar el inventario, y se rehace al reiniciar |

### Entrega 2 — Commit 13: organización con varias compañías

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/Organizacion.java` | IA | `ArrayList<Planta>` (id = índice), semillas `maestra × 1000 + id`, tick en orden y ranking global |
| `modelo/Maquina.java` | IA | Clave compuesta `(idCompania, indice)`, decisión del equipo |
| `modelo/RankingFallos.java` | IA | Un solo comparador por clave compuesta; referencia al global; `agregar` y `quitar`; se retira `reconstruir` |
| `modelo/Planta.java` | IA | Id, nombre y ranking global; `reiniciar()` saca y vuelve a meter cada máquina en los rankings |
| `modelo/ConfiguracionPlanta.java` | IA | `crearMaquinas(idCompania)` y `NOMBRES_COMPANIAS` |
| `test/.../OrganizacionTest.java` | IA | 2 pruebas: misma semilla maestra, mismo resultado; ranking global igual a ordenar las 18 máquinas |
| `test/.../RankingFallosTest.java`, `PlantaTest.java` | IA | Ajustados al desempate por clave compuesta |

### Entrega 2 — Commit 13a: crear máquinas con etiqueta única

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/TipoMaquina.java` | IA | Catálogo de los 6 tipos, con descripción y prefijo de etiqueta |
| `modelo/ConfiguracionPlanta.java` | IA | `crearMaquina(tipo, id, indice, etiqueta)` con un `switch`; los valores de Cristian no cambian, solo se movieron |
| `modelo/Maquina.java` | IA | Guarda el `TipoMaquina` en vez de un texto |
| `modelo/Planta.java` | IA | `Set<String> etiquetas = new TreeSet<>()`, `existeEtiqueta` y `crearMaquina`; reiniciar conserva las máquinas creadas (opción A del equipo) |
| `api/dto/MaquinaDto.java` | IA | Envía `tipo().descripcion()`: el JSON no cambia |
| `test/.../PlantaTest.java`, `OrganizacionTest.java` | IA | 3 pruebas: crear, etiqueta repetida y misma etiqueta en otra compañía |

### Entrega 2 — Commit 13b: crear compañías con nombre único

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/Organizacion.java` | IA | `Set<String> nombres = new TreeSet<>()` con clave en minúsculas, `existeCompania` y `crearCompania`; la compañía nueva arranca con las 6 máquinas (opción A del equipo) |
| `modelo/Planta.java` | IA | Parámetro `tickInicial`: todas las plantas tienen el tick de la organización |
| `test/.../OrganizacionTest.java` | IA | 2 pruebas: crear a mitad de la simulación y reproducibilidad con creaciones |

### Entrega 2 — Commit 19: el servicio usa la organización y rutas por compañía

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `servicio/ServicioSimulacion.java` | IA | Guarda la `Organizacion`; las acciones reciben `idCompania`; `resumenCompanias` y `estadoCompania`, todos `synchronized` |
| `api/ControladorCompanias.java` | IA | `GET /api/companias` y `GET /api/companias/{id}` |
| `api/ControladorSimulacion.java`, `api/ControladorOperador.java` | IA | Sale `GET /api/planta`; las acciones pasan a `/api/companias/{id}/...` |
| `api/dto/ResumenCompaniaDto.java` | IA | Totales por compañía, con las máquinas por estado |
| `api/dto/EstadoPlantaDto.java`, `api/dto/MaquinaDto.java` | IA | `idCompania` y `nombre`; `tipo` como código del catálogo y `tipoDescripcion` |
| `test/.../ControladorCompaniasTest.java` y pruebas adaptadas | IA | 4 pruebas nuevas de compañías, 1 de independencia entre compañías y 2 del servicio |
| `docs/API.md` | IA | Contrato con las rutas por compañía |

### Entrega 2 — Commit 19a: endpoints para crear compañías y máquinas

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `servicio/ServicioSimulacion.java` | IA | `crearCompania`, `crearMaquina` y `tiposMaquina`; el repetido se revisa antes para responder 409 y no 400 |
| `api/ControladorCompanias.java` | IA | `POST /api/companias`, `POST /api/companias/{id}/maquinas` (201) y `GET /api/tipos-maquina` (opción a del equipo) |
| `api/dto/CrearCompaniaDto.java`, `CrearMaquinaDto.java`, `TipoMaquinaDto.java` | IA | Cuerpos de las peticiones y el catálogo |
| `test/.../ControladorCreacionTest.java` | IA | 6 pruebas, con `@DirtiesContext` porque reiniciar conserva lo creado |
| `docs/API.md` | IA | Sección "Crear compañías y máquinas" |

### Entrega 2 — Commit 21: endpoints del ranking y las recomendaciones

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/Planta.java`, `modelo/Organizacion.java` | IA | `rankingConPuntajeMinimo` y `rankingGlobalConPuntajeMinimo`: exponen la consulta por rango de `RankingFallos` |
| `servicio/ServicioSimulacion.java` | IA | `ranking(idCompania, puntajeMinimo)`, sin ordenar nada |
| `api/ControladorSuperusuario.java` | IA | `GET /api/ranking` con `compania` y `puntajeMinimo` opcionales |
| `api/dto/RankingDto.java`, `EntradaRankingDto.java` | IA | El ranking con los umbrales y una entrada por máquina |
| `api/dto/MaquinaDto.java` | IA | `puntajeFallos`, `numeroOverhauls` y `recomendacion` |
| `test/.../ControladorSuperusuarioTest.java`, `OrganizacionTest.java` | IA | 5 pruebas MockMvc tras 300 ticks y 1 del modelo para la consulta por rango |
| `docs/API.md` | IA | Sección "Ranking de fallos y recomendaciones" |

### Entrega 2 — Decisión: el modo manual es un juego de gestión

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `CLAUDE.md`, `docs/BITACORA_DESARROLLO.md` | Mixto | Las ideas (juego de gestión, presupuesto, especialidades, tipo de fallo, dificultad, conservar el historial de fallos) son del equipo; la IA propuso la regla central, las reglas de desgaste y la redacción |

### Entrega 2 — Commit 14: modo manual

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/ModoSimulacion.java` | IA | `AUTOMATICO` y `MANUAL` |
| `modelo/Planta.java` | IA | El tick se salta los pasos 1, 4 y 6 en manual; `clasificarSiguiente` y `asignarSiguiente` reutilizan el paso 4 y `asignarUna` (el cuerpo del paso 6); reiniciar conserva el modo |
| `modelo/Organizacion.java` | IA | `cambiarModo` aplica el modo a todas las compañías; una compañía creada después lo hereda |
| `test/.../PlantaTest.java`, `OrganizacionTest.java` | IA | 5 pruebas: nada pasa solo en manual, el ciclo clasificar → asignar, los botones rechazados en automático, reiniciar conserva el modo y el modo llega a todas |

### Entrega 2 — Commit 14a: tipo de fallo y técnicos con especialidad

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/TipoFallo.java` | IA | `MECANICO` y `ELECTRICO`; también es la especialidad del técnico |
| `modelo/Sensor.java`, `ConfiguracionPlanta.java` | IA | Cada sensor declara su tipo (5 de 18 eléctricos, aprobado por el equipo); `PENALIDAD_ESPECIALIDAD = 2`; Ana mecánica y Luis eléctrico |
| `modelo/EventoFallo.java`, `OrdenTrabajo.java`, `Clasificador.java` | IA | El tipo se copia del sensor; el preventivo es mecánico |
| `modelo/Tecnico.java` | IA | Especialidad y penalidad al asignar |
| `modelo/Planta.java` | IA | `EnumMap` de dos colas FIFO; la asignación automática espera a la especialidad; `asignarSiguiente(especialidad)` en manual |
| `api/dto/*` | IA | `tipoFallo`, `especialidad` y `tecnicosLibres` como dos colas |
| `test/.../PlantaTest.java`, `ColaPrioridadOrdenesTest.java` | IA | 3 pruebas nuevas: el tipo sale del sensor, el automático espera a su especialidad y la penalidad |
| `docs/API.md`, `CLAUDE.md` | IA | El contrato nuevo y la asignación de tipos de fallo |

### Entrega 2 — Commit 14b: economía y partida

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/Contabilidad.java` | IA | Caja, ingresos, costos, costo del tick y disponibilidad, en enteros; quiebra con caja negativa |
| `modelo/ConfiguracionPlanta.java` | IA | Presupuesto 300.000, ingreso 1.000, mano de obra 1.500 y partida de 1000 ticks, calculados con una medición de la simulación |
| `modelo/Planta.java` | IA | Paso 7b (contabilizar), contador de fallos, la quiebra congela la compañía y rechaza acciones |
| `modelo/Organizacion.java`, `servicio/ServicioSimulacion.java` | IA | La partida termina en el tick 1000 y el reloj se pausa |
| `api/ManejadorErrores.java` | IA | `IllegalStateException` del modelo → 409 |
| `test/.../ContabilidadTest.java`, `PlantaTest.java`, `OrganizacionTest.java`, `ServicioSimulacionTest.java` | IA | 6 pruebas |

### Entrega 2 — Commit 15: solicitudes de overhaul y técnicos del fabricante

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/SolicitudOverhaul.java` | IA | Puntaje congelado y comparador con secuencia para los empates |
| `modelo/TecnicoExterno.java` | IA | Técnico del fabricante; asignar y terminar llegan en el commit 16 |
| `modelo/Organizacion.java` | IA | Montículo (`PriorityQueue`), cola FIFO de técnicos del fabricante, `aprobarOverhaul` (solo con recomendación, opción a del equipo), la cima y el orden de salida |
| `modelo/Maquina.java` | IA | Marca `overhaulPendiente`, con visibilidad de paquete para cambiarla |
| `modelo/Planta.java`, `ConfiguracionPlanta.java` | IA | `anotar` para la bitácora; 2 técnicos del fabricante y `DURACION_OVERHAUL = 30` |
| `test/.../SolicitudOverhaulTest.java`, `OrganizacionTest.java` | IA | 3 pruebas: el orden del comparador, la salida por puntaje y los rechazos con el reinicio |

### Entrega 2 — Commit 16: ejecutar overhauls en el tick

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/EstadoMaquina.java` | IA | `EN_OVERHAUL` |
| `modelo/Maquina.java` | IA | Las dos reglas de desgaste en `probabilidadFallo` |
| `modelo/TecnicoExterno.java` | IA | Asignar, avanzar, terminar y liberar |
| `modelo/Planta.java` | IA | `iniciarOverhaul` (cobra), `terminarOverhaul` (reinserta en los rankings), descarte perezoso de la P4, sin preventivos ni fallos inyectados en overhaul |
| `modelo/Organizacion.java` | IA | Avanzar overhauls, la regla de la cima, el descarte de solicitudes de compañías en quiebra y el botón manual |
| `modelo/ConfiguracionPlanta.java` | IA | Umbral 30, máximo 1, costo 120.000 y los factores de desgaste (aprobados por el equipo) |
| `test/...` | IA | 4 pruebas nuevas (desgaste, ciclo, cima y descarte perezoso) y 3 ajustadas al umbral nuevo |

### Entrega 2 — Commit 17: reemplazo de máquina

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/Planta.java` | IA | `reemplazarMaquina` (`set` en el mismo índice, los dos rankings, cobra); deshacer descarta el comando de una máquina reemplazada |
| `modelo/Organizacion.java` | IA | Descarte perezoso de la solicitud de una máquina reemplazada; usa `Maquina.estaLibre` |
| `modelo/Maquina.java` | IA | `estaLibre`, la regla que comparten el overhaul y el reemplazo |
| `modelo/comandos/*.java` | IA | `maquina()` en `ComandoOperador` y sus dos comandos (aprobado por el equipo) |
| `modelo/ConfiguracionPlanta.java` | IA | `COSTO_REEMPLAZO = 300.000` |
| `test/.../OrganizacionTest.java`, `PlantaTest.java` | IA | 3 pruebas nuevas (reemplazo, rechazos, descartes de la pila y del montículo) y 1 ajustada al mensaje nuevo |

### Entrega 2 — Commit 18: historial con árbol de segmentos

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/ResumenHistorial.java` | IA | Record nuevo: total y pico de las dos series (aprobado por el equipo) |
| `modelo/Planta.java` | IA | Dos `ArbolSegmentos` por compañía, `puntajeDelTick`, `registrarHistorial` e `historial(desde, hasta)` |
| `modelo/Organizacion.java` | IA | Dos árboles globales, paso 4 del tick e `historial(desde, hasta)` |
| `modelo/Contabilidad.java` | IA | El costo del tick se pone en 0 al registrar el historial (aprobado por el equipo) |
| `test/.../OrganizacionTest.java`, `PlantaTest.java` | IA | 2 pruebas nuevas (ventana por compañía y global contra un cálculo directo; reemplazo entre ticks y reinicio) y 1 ajustada |

### Entrega 2 — Commit 20: endpoints del modo manual y la partida

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `api/dto/ModoDto.java`, `AsignacionDto.java`, `ResumenPartidaDto.java` | IA | Cuerpos del modo y de la asignación; resumen de la partida |
| `api/dto/EstadoSimulacionDto.java`, `EstadoPlantaDto.java`, `ResumenCompaniaDto.java` | IA | Modo, partida, caja, ingresos, costos, fallos, disponibilidad y quiebra |
| `servicio/ServicioSimulacion.java` | IA | `cambiarModo`, `clasificarSiguiente`, `asignarSiguiente` (409 si el botón no tiene efecto), `resumenPartida` y un conversor de enumerados |
| `api/ControladorSimulacion.java`, `ControladorOperador.java` | IA | `PUT /simulacion/modo`, `GET /partida/resumen`, `POST clasificar-siguiente` y `asignar-siguiente` |
| `test/.../ControladorJuegoTest.java` | IA | 4 pruebas MockMvc: el modo, los botones en automático, el ciclo manual y la economía con el resumen |
| `docs/API.md` | IA | Contrato de los endpoints nuevos |

### Entrega 2 — Commit 22: endpoints de overhaul y reemplazo

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `api/dto/AprobarOverhaulDto.java`, `SolicitudOverhaulDto.java`, `TecnicoExternoDto.java`, `EstadoOverhaulsDto.java` | IA | Cuerpo de aprobar; la foto del montículo (cima y orden de salida, sin dibujar el árbol) y de los técnicos del fabricante |
| `api/dto/MaquinaDto.java` | IA | Campo `overhaulPendiente` (aprobado por el equipo) |
| `servicio/ServicioSimulacion.java` | IA | `aprobarOverhaul`, `asignarSiguienteOverhaul` (409 con la razón si el camión no sale), `estadoOverhauls` y `reemplazarMaquina` |
| `api/ControladorSuperusuario.java`, `ControladorOperador.java` | IA | `GET` y `POST /overhauls`, `POST /overhauls/asignar-siguiente` y `POST .../maquinas/{indice}/reemplazo` |
| `test/.../ControladorOverhaulTest.java` | IA | 4 pruebas MockMvc: el montículo vacío, aprobar (400, 404, 409 y éxito), despachar el camión y reemplazar |
| `docs/API.md` | IA | Sección 4c: contrato del overhaul y el reemplazo |

### Entrega 2 — Commit 23: endpoint del historial por ventana de ticks

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/Planta.java`, `Organizacion.java` | IA | `serieFallos` y `serieCostos`: el valor de cada tick de una ventana, leído hoja por hoja con `ArbolSegmentos.valor` (aprobado por el equipo) |
| `api/dto/HistorialDto.java` | IA | Total, pico y serie por tick de las dos series |
| `servicio/ServicioSimulacion.java` | IA | `historial`: ventana por defecto y validación (400) |
| `api/ControladorSuperusuario.java` | IA | `GET /api/historial` |
| `test/.../ControladorHistorialTest.java` | IA | 3 pruebas: el total y el pico contra la serie, el global contra la suma de las compañías, la ventana de una compañía y los errores |
| `docs/API.md` | IA | Sección 4d: contrato del historial |

### Entrega 2 — Commit 14c: media capacidad en alarma (la dificultad se pospuso)

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `modelo/ConfiguracionPlanta.java`, `Planta.java` | IA | `PRODUCCION_EN_ALARMA` = 50 y su uso en el paso 7b (aprobado por el equipo tras las mediciones) |
| `test/.../PlantaTest.java` | IA | 1 prueba: una máquina en alarma produce la mitad |
| `CLAUDE.md`, `docs/BITACORA_DESARROLLO.md` | IA | Mediciones de calibración y la decisión de posponer la dificultad |

### Entrega 2 — Commit 24: documento explicativo de la Entrega 2

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `docs/DOCUMENTO_EXPLICATIVO.md` | IA | Segunda parte (secciones 10 a 19): por qué y cómo de cada estructura nueva, la trampa de la clave que cambia, el descarte perezoso, el árbol de segmentos frente a las sumas prefijas y la tabla de complejidades. **Pendiente: revisión del equipo para defenderlo en clase** |

### Entrega 2 — Commit 25: cierre de la documentación

| Archivo | Autoría | Revisión del equipo |
|---|---|---|
| `README.md` | IA | Enlace al frontend, estructuras y estado de la Entrega 2 |
| `docs/USO_IA.md`, `docs/BITACORA_DESARROLLO.md`, `CLAUDE.md` | IA | Porcentajes, cierre de la bitácora y lista de entregables |

## 4. Porcentaje estimado

| Componente | Generado con IA | Observación |
|---|---|---|
| Documento de diseño (`CLAUDE.md`) | 0 % en la Entrega 1; mixto desde la Entrega 2 | Las secciones de la Entrega 2 las redactó la IA sobre decisiones del equipo |
| Esqueleto del proyecto (etapa 0) | ~95 % | Configuración de Maven y documentación de paquetes |
| Estructuras de datos y sus pruebas (etapa 1) | ~95 % | Generadas contra una especificación escrita por el equipo, que define capacidad, punteros, complejidades y casos de prueba obligatorios |

| Modelo, servicio y API de la Entrega 2 | ~100 % | Generados contra el diseño de las secciones 16 y 17 de `CLAUDE.md`; cada commit lo revisó el equipo antes de aprobarlo |
| Pruebas de la Entrega 2 | ~100 % | Los casos obligatorios los fija `CLAUDE.md` |
| Documentación (`docs/`, `README.md`) | ~100 % | Redactada sobre las decisiones del equipo; el documento explicativo está pendiente de revisión del equipo |

**Estimación global del proyecto, medida al cerrar la Entrega 2 (10 de noviembre de 2026).** Se
contó con `git blame` a qué commit pertenece cada línea de Java que existe hoy en el repositorio:

| Código | Líneas | De commits con `Co-Authored-By` |
|---|---|---|
| `src/main/java` | 8.244 | 8.244 (100 %) |
| `src/test/java` | 3.185 | 3.185 (100 %) |

Es decir, **el código fuente es 100 % generado con IA** según la trazabilidad de Git: 61 de los 71
commits llevan la línea `Co-Authored-By` al momento de medir. Los otros 10 son 7 merges, 2 ediciones
del README hechas desde GitHub y el documento de diseño inicial (`CLAUDE.md`), escrito por el equipo. Las líneas que el equipo ajustó a mano durante la revisión quedaron dentro de esos
mismos commits, así que no se pueden separar.

**Lo que aportó el equipo** no son líneas de código sino las decisiones que el código implementa:
el problema y su dominio, la elección y justificación de cada estructura, las reglas de la
simulación, los valores de las máquinas y los sensores (Cristian), el diseño del juego de gestión
(Kevin), la aprobación o el rechazo de cada propuesta antes de implementarla y la revisión de cada
commit. Todo eso está registrado, con fecha, en `CLAUDE.md` y en `docs/BITACORA_DESARROLLO.md`.
