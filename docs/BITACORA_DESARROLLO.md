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

## Etapa 5 — 2026-10-18 — Controlador e integración

**Responsable:** Kevin, con asistencia de Claude Code.

Con esta etapa queda cubierto el mínimo obligatorio de la sección 10: la aplicación funciona
completa.

### Hecho

- **`controlador/ControladorPlanta`**, dueño del `javax.swing.Timer` y único camino por el que
  pasan las acciones que modifican el modelo.
- **`vista/AccionesOperador`**, la interfaz que separa la vista del controlador.
- **`vista/PanelControlesMaquina`**: selector de sensor, carga, parar/arrancar e inyectar fallo.
- Barra con **Iniciar/Pausar, Paso y Reiniciar**; clic en el inventario para seleccionar máquina;
  botón **Deshacer** en el panel de la pila.
- **`App`** deja de ser provisional y arma el MVC completo.

### Decisiones

| Decisión | Alternativa descartada | Razón |
|---|---|---|
| `AccionesOperador` se declara en el paquete **vista** y la implementa el controlador | Declararla en `controlador`, o pasarle el controlador a los paneles | Así la vista no importa nada de `controlador` y el controlador no importa nada de Swing: los dos dependen del contrato, no uno del otro. Es la misma idea de `ObservadorPlanta`, pero en el sentido contrario |
| Los paneles avisan con un `IntConsumer` y la ventana decide a quién trasladarlo | Que cada panel recibiera el controlador | Un panel se puede dibujar y probar sin que exista ningún controlador, y de hecho así lo hace la prueba de la vista |
| Seleccionar máquina o sensor **no** pasa por el controlador | Hacer pasar toda interacción por él | Seleccionar no modifica la planta, solo cambia lo que el operador está mirando: es estado de presentación |
| Los comandos se construyen justo antes de ejecutarse y nunca se guardan | Crearlos una vez y reutilizarlos | Un comando toma nota del estado **al construirse**. Uno guardado recordaría un estado viejo y al deshacerlo restauraría algo equivocado |
| El botón de parar se deshabilita cuando no aplica, y además el controlador ignora la acción | Solo una de las dos | La vista evita que el operador pulse algo que no va a ocurrir; la comprobación del controlador es la red de seguridad |
| Controles de la máquina en un panel aparte del de detalle | Mezclar botones dentro del panel dibujado | El de detalle se dibuja a mano y el de controles necesita componentes de Swing de verdad; mezclarlos obligaba a reservar espacio a mano y a pelear con el `BorderLayout` |

### Incidencia

La prueba de reproducibilidad falló al principio, pero el fallo era de la prueba: las 29 líneas de
bitácora eran idénticas salvo la primera, que dice "Planta iniciada" al arrancar y "Planta
reiniciada" al reiniciar. Se ajustó la aserción para comparar lo que de verdad debe reproducirse.

### Verificación

| Comando | Resultado |
|---|---|
| `./mvnw test` | BUILD SUCCESS — 60 pruebas, 0 fallos |
| `./mvnw javadoc:javadoc` | BUILD SUCCESS, sin advertencias |
| `java -jar target/simulador-planta.jar` | La aplicación abre y funciona |

Cuatro pruebas nuevas, y ninguna toca el reloj: `javax.swing.Timer` es de la librería estándar y
probarlo sería probar a Swing. Se prueba que un paso avance exactamente un tick, que reiniciar
repita la misma simulación con la misma semilla, que parar una máquina se pueda deshacer, y que las
acciones que no aplican se ignoren en vez de fallar.

### Pendiente

- Etapa 6: manual de usuario, documento explicativo, cierre de `USO_IA.md` y guion del video.

---

## Etapa 4 — 2026-10-18 — Los paneles de la interfaz

**Responsable:** Kevin, con asistencia de Claude Code.

Ocho paneles más la ventana: `PanelInventario`, `PanelDetalleMaquina`, `PanelColaCircular`,
`PanelColaEventos`, `PanelColaTecnicos`, `PanelColaPrioridad`, `PanelPila`, `PanelBitacora` y
`VentanaPrincipal`, con `Colores` y `SeleccionVista` como apoyo. Solo visualización: los controles
llegan con el controlador, porque toda acción del usuario debe pasar por él.

### La decisión que ordenó el diseño

La sección 9 pide **no dibujar estado interno que `java.util` no expone**, y eso parte los paneles
en dos familias:

- `ColaCircular` es propia y expone celdas, punteros y sobrescrituras, así que su panel dibuja **el
  arreglo real**, con los huecos y el final dando la vuelta.
- Las otras cuatro son de `java.util`, así que sus paneles dibujan **solo la secuencia de
  elementos**. Nada de celdas ni de capacidad inventada.

Esa diferencia queda visible en pantalla y es exactamente lo que el profesor puede preguntar.

### Decisiones

| Decisión | Alternativa descartada | Razón |
|---|---|---|
| Clase base `PanelEstructura` con método plantilla: ella pinta la cabecera y cada subclase solo define `dibujarContenido` | Que cada panel se dibujara entero por su cuenta | Los ocho paneles se ven como una familia sin repetir código de presentación, y la ventana los refresca con **una sola llamada polimórfica** sobre una lista, sin saber cuál es cuál |
| Dibujo con `paintComponent` en vez de componentes Swing | `JLabel` dentro de `GridLayout` | Hacen falta celdas vacías y punteros ubicados con precisión; con layout managers habría tocado pelear por cada píxel |
| `PanelBitacora` sí usa `JTextArea` en un `JScrollPane` | Dibujar el texto a mano | Un registro necesita desplazamiento y selección con el ratón, y Swing ya lo resuelve. Para eso se agregó el gancho `actualizar()` a la clase base |
| El espacio sobrante de la ventana se lo lleva la bitácora | Repartirlo entre los paneles de estructuras | Los paneles tienen un alto natural y estirarlos solo agrega vacío; una bitácora más alta sí muestra más historia |
| `SeleccionVista` vive en la vista, no en `Planta` | Guardar la selección en el modelo | La planta funciona igual sin importar qué celda tenga seleccionada el usuario: es estado de presentación |

### Verificación

Los paneles se revisaron **mirándolos**: se renderizan a PNG sin abrir ventana, lo que permitió
corregir dos defectos antes de entregarlos, uno de ellos un recorte de la segunda fila del panel de
técnicos. La captura del resultado quedó en `docs/img/ventana.png`.

| Comando | Resultado |
|---|---|
| `./mvnw test` | BUILD SUCCESS — 56 pruebas, 0 fallos |
| `./mvnw javadoc:javadoc` | BUILD SUCCESS, sin advertencias |

Una sola prueba nueva, y cubre lo que de verdad puede romperse: que los ocho paneles se dibujen sin
excepción para **cada máquina** del inventario, con la planta recién creada (sin ninguna lectura) y
con la simulación en marcha. Ese es el caso que dejaría la ventana en blanco al arrancar.

### Pendiente

- Etapa 5: controlador, `javax.swing.Timer`, botones, selección con el ratón, inyectar fallo y
  deshacer.

---

## Ajuste — 2026-10-18 — Calibración de la simulación

**Responsable:** decisión de Kevin (opción 2), calibración medida con Claude Code.

Tres cambios, todos nacidos de medir y no de suponer:

1. **Reparaciones más largas** (P1=10, P2=8, P3=6, P4=4). Las tasas de fallo de Cristian quedaron
   sin tocar.
2. **El contador de mantenimiento se reinicia solo con una orden P4.** Una reparación correctiva no
   es un servicio preventivo.
3. **Techo de 2 al factor de desgaste**, que apareció como consecuencia del punto anterior.

### El lazo de realimentación

Al aplicar el punto 2 la planta se saturó: promedio de 8,2 órdenes en cola y el carril P4 lleno el
91 % de los ticks pero sin atenderse nunca. La causa está en la fórmula del paso 1:

`p = tasaBase × (carga/100) × (1 + ticksDesdeMantenimiento / intervaloPreventivo)`

Como el P4 es la prioridad más baja, bajo carga se queda al fondo de la cola. Y como ya no se
reinicia con las reparaciones correctivas, el contador crece sin límite, la máquina falla cada vez
más, llega más trabajo correctivo y el preventivo se aleja todavía más. Es inanición por prioridad
realimentando la tasa de fallo. El techo de 2 lo corta: una máquina muy atrasada falla al doble de
su tasa base, no más.

### Calibración medida (1000 ticks, semilla 7)

| Variante | Ticks con cola | Promedio | Máx. | Carriles con contenido |
|---|---|---|---|---|
| 3 máquinas, valores iniciales | 1 % | — | 1 | casi ninguno |
| 6 máquinas, valores iniciales | 6 % | 0,07 | 2 | P3 5 %, P4 1 % |
| **B: 6 máquinas, reparaciones largas, techo** | **38 %** | **0,54** | **4** | **P1 1 %, P2 3 %, P3 16 %, P4 27 %** |
| C: lo mismo con tasas 1,5× | 74 % | 2,10 | 9 | P3 46 %, P4 58 % |

Se eligió **B**: los cuatro carriles muestran contenido, la planta se ve ocupada pero no saturada, y
es la opción que menos se aparta de los valores de Cristian. Si en el ensayo del video se quiere una
planta más movida, pasar a C es cambiar seis números en un archivo. Para el pico de la demostración
está además el botón de inyectar fallo, que es justo para eso.

---

## Ajuste — 2026-10-18 — El inventario sube de 3 a 6 máquinas

**Responsable:** decisión de Kevin, implementada con Claude Code.

Se agregaron `COMPRES-01` (compresor de tornillo), `HORNO-01` (horno de tratamiento térmico) y
`BOMBA-01` (bomba centrífuga), cada una con tres sensores. Es un cambio **solo de configuración**:
no se tocó ni una línea de lógica. `CLAUDE.md` quedó actualizado en sus secciones 2, 6 y 10.

Se agregó también una prueba que recorre las 6 máquinas, sus 18 sensores y las 3 magnitudes, y
verifica que **cada magnitud deje la lectura en su zona de umbrales incluso con el ruido en
contra**: leve entre advertencia y alto, moderada entre alto y crítico, severa sobre el crítico. Es
la prueba que protege los ajustes de valores de Cristian, porque si una zona queda más estrecha que
el ruido del sensor, una anomalía severa podría dejar de clasificarse como P1 y la demostración
fallaría sin dar ninguna señal.

### Medición del efecto

| | Antes (3 máquinas) | Después (6 máquinas) | Con las tasas propuestas |
|---|---|---|---|
| Ticks con órdenes en cola | 1 % | 6 % | 60 % |
| Máximo de órdenes en cola | 1 | 2 | 4 |
| Ticks con eventos sin clasificar | — | 1 % | 4 % |

Subir las máquinas levanta el **techo** de la cola, pero no la llena: para eso hace falta que las
órdenes lleguen más rápido de lo que los técnicos las atienden. La tercera columna corresponde a la
propuesta de tasas y duraciones que figura en la tabla de desviaciones, medida pero **no
implementada**, porque esos valores son de Cristian.

Esa propuesta tiene un efecto secundario que hay que resolver antes de adoptarla: con tantos fallos,
cada reparación reinicia el contador de mantenimiento y **el carril P4 nunca se llena** (0 % de los
ticks). Las dos salidas son bajar los intervalos preventivos, o reiniciar el contador solo cuando la
orden atendida es P4, que es más realista pero se aparta del paso 5 de la sección 7.

---

## Etapa 3 — 2026-10-18 — La simulación funcionando, sin interfaz

**Responsable:** Kevin, con asistencia de Claude Code.

### Hecho

- **`ObservadorPlanta`**, interfaz propia del patrón Observador. Es la pieza que permite que el
  modelo no conozca la vista: al terminar cada tick, `Planta` avisa sin saber quién escucha.
- **`ColaPrioridadSoloLectura`**, la interfaz de solo consulta de la cola con prioridad, igual que
  la de la cola circular en la etapa 2.
- **`GeneradorFallos`**, la única clase con aleatoriedad, con un solo `Random` con semilla.
- **`Clasificador`**, que convierte evento en orden comparando únicamente contra los umbrales.
- **`comandos/ComandoOperador`** más sus dos implementaciones: `PararOArrancarMaquina` y
  `CambiarCargaMaquina`.
- **`Planta`**, la fachada con las cinco estructuras y `tick()` con sus ocho pasos.
- **`Maquina.preventivaPendiente`**, el indicador aprobado por Kevin.
- **Seis pruebas** de la simulación completa, sin interfaz gráfica.

### Decisiones

| Decisión | Alternativa descartada | Razón |
|---|---|---|
| El desplazamiento de una anomalía apunta al **centro** de la zona de umbrales que corresponde a su magnitud | Sortear un valor dentro de la zona | Apuntando al centro, el ruido normal del sensor no alcanza a sacar la lectura de su zona, así que una anomalía severa siempre termina en P1. El comportamiento queda predecible sin que el clasificador sepa nada de la magnitud |
| El ruido de las lecturas también sale de `GeneradorFallos` | Un `Random` aparte en `Planta` | Todo el azar debe salir del mismo generador con semilla, o la simulación deja de ser reproducible |
| La bitácora es un `ArrayDeque` con tope de 200 líneas | Un `ArrayList` que crece sin límite | Al llenarse hay que descartar las líneas más viejas por el frente, que en un deque es O(1). Queda documentado que **no es una de las cinco estructuras del enunciado**, sino un registro auxiliar |
| `Planta.accionesDeshacibles()` entrega textos, no comandos | Entregar la lista de comandos | Si la vista recibiera los comandos podría ejecutarlos o revertirlos por su cuenta. Con texto, deshacer solo ocurre por `Planta.deshacer()` |
| El desgaste (`envejecer`) solo avanza en las máquinas que están funcionando | Avanzarlo en todas | Una máquina detenida o en mantenimiento no se desgasta |

### Verificación

| Comando | Resultado |
|---|---|
| `./mvnw test` | BUILD SUCCESS — 54 pruebas, 0 fallos |
| `./mvnw javadoc:javadoc` | BUILD SUCCESS, sin advertencias |

Las seis pruebas nuevas cubren cadenas completas del tick, no métodos sueltos: reproducibilidad con
la misma semilla, una anomalía severa que termina en orden P1 asignada, un solo evento clasificado
por tick, la reparación que devuelve la máquina a operar, el preventivo que no se repite gracias al
indicador, y el descarte de un comando cuando la simulación ya cambió el estado.

### Hallazgo: las colas casi nunca se llenan

Un ensayo de 1000 ticks con la semilla 7 mostró que la cola con prioridad **está vacía el 99 % de
los ticks y nunca pasa de una orden**. La causa no es un error, es aritmética del escenario:

- Una máquina con anomalía activa no desarrolla otra, y una máquina en alarma, detenida o en
  mantenimiento tampoco genera lecturas nuevas.
- Por lo tanto, con **tres** máquinas hay como máximo tres fallos simultáneos en toda la planta.
- Con **dos** técnicos que reciben trabajo en el mismo tick en que la orden se clasifica, la cola no
  puede acumular más de una orden.

El problema es de presentación, no de lógica: los dos paneles de colas son el centro de la entrega y
se verían vacíos durante toda la demostración. Está pendiente de decisión del equipo (ver la tabla
de desviaciones).

### Pendiente

- Decidir cómo hacer que las colas se llenen de forma visible.
- **Cristian:** revisar los valores de `ConfiguracionPlanta`.
- Etapa 4: los seis paneles de la interfaz Swing.

---

## Etapa 2 — 2026-10-18 — Entidades del modelo y configuración

**Responsable:** Kevin, con asistencia de Claude Code.

### Hecho

- Enums **`EstadoMaquina`** (los cinco estados, con color) y **`Magnitud`** (leve, moderada,
  severa).
- Records de datos: **`Sensor`**, **`Lectura`**, **`Anomalia`** y **`EventoFallo`**.
- **`Maquina`**, la única entidad con estado mutable, dueña de su propia `ColaCircular<Lectura>`.
- **`Tecnico`**, con el ciclo asignar → avanzar → liberar.
- **`estructuras/ColaCircularSoloLectura<T>`**, interfaz con las once operaciones de consulta de la
  cola circular. `ColaCircular` la implementa y `Maquina.lecturas()` la devuelve, de modo que la
  vista no tenga forma de modificar la estructura.
- **`ConfiguracionPlanta`**, con todos los parámetros de la simulación en un solo archivo: semilla,
  milisegundos por tick, capacidad de la cola circular, duraciones de reparación, pesos de
  magnitud, nombres de los técnicos y la definición de las tres máquinas con sus nueve sensores.
- **Tres pruebas**, deliberadamente pocas: los records son datos y no necesitan pruebas propias.

### Decisiones

| Decisión | Alternativa descartada | Razón |
|---|---|---|
| `Sensor` valida en su constructor que `nominal + ruido < umbralAdvertencia` | Confiar en que los valores estén bien escritos | Si no hay margen aparecen alarmas falsas por puro ruido, sin ninguna anomalía. En la presentación eso se vería como que el simulador no funciona. Ahora una configuración mal escrita falla al arrancar, no en la demostración |
| `Lectura` guarda los valores en una `List<Double>` paralela a los sensores de la máquina | Un mapa de nombre de sensor a valor | La posición ya identifica la variable, y el mismo índice sirve para la lista de sensores y para los valores |
| La fórmula de probabilidad de fallo vive en `Maquina.probabilidadFallo()` | Ponerla en `GeneradorFallos` | El cálculo no tiene aleatoriedad, solo depende del estado de la máquina. Así el generador se queda únicamente con el sorteo |
| `Maquina.alarmaActiva` es un dato aparte del estado | Deducirlo de `estado == EN_ALARMA` | Una máquina puede estar DETENIDA o EN_MANTENIMIENTO y seguir teniendo su alarma pendiente |
| Se creó la interfaz propia `ColaCircularSoloLectura<T>`, que `ColaCircular` implementa, y `Maquina.lecturas()` devuelve ese tipo | Devolver la cola circular tal cual, documentando que la vista solo lee | Una copia no sirve, porque el panel necesita el estado físico de la cola (capacidad, punteros, celda por celda). Con la interfaz, quien recibe la cola no tiene métodos para encolar ni desencolar: **la regla la hace cumplir el compilador, no la buena voluntad del programador**. Además es un uso directo de encapsulamiento y polimorfismo, que son materia del curso. Decisión tomada por Kevin |
| Umbrales de vibración tomados de ISO 10816 (4,5 / 7,1 / 11,0 mm/s) | Inventar números | Es una norma real para máquinas industriales, así que ese dato se puede defender ante el profesor |
| Solo tres pruebas en esta etapa | Probar cada entidad | Los records son datos sin lógica. Lo que sí puede estar mal son los valores de la configuración, y eso es justo lo que se prueba |

### Verificación

| Comando | Resultado |
|---|---|
| `./mvnw test` | BUILD SUCCESS — 48 pruebas, 0 fallos |
| `./mvnw javadoc:javadoc` | BUILD SUCCESS, sin advertencias |

### Pendiente

- **Cristian:** revisar los valores provisionales de `ConfiguracionPlanta` (umbrales, tasas de
  fallo, intervalos preventivos y cargas iniciales). Es un solo archivo y no hay que tocar lógica.
- Etapa 3: `GeneradorFallos`, `Clasificador`, `ObservadorPlanta`, los comandos de deshacer y
  `Planta.tick()` con sus ocho pasos, probado sin interfaz.

---

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
| 2026-10-18 | 7, paso 7 (preventivo) | Marcar en `Maquina` un `boolean` de preventiva pendiente, en vez de recorrer el carril P4 de la cola con prioridad para saber si ya hay una | Recorrer el carril es O(n) y obliga a inspeccionar la cola desde afuera. El indicador es O(1) y respeta mejor el tipo abstracto de datos | **Implementada**, aprobada por Kevin |
| 2026-10-18 | 2, 6 y 10 (tres máquinas) | Subir el inventario inicial de **3 a 6 máquinas**, dejando los 2 técnicos | Con tres máquinas hay a lo sumo tres fallos simultáneos, y con dos técnicos que toman la orden en el mismo tick la cola con prioridad nunca pasa de una orden. Los paneles de colas se verían vacíos en toda la demostración | **Implementada**, aprobada por Kevin. `CLAUDE.md` ya actualizado |
| 2026-10-18 | 6 (duraciones de reparación) | Alargar las reparaciones: P1=10, P2=8, P3=6, P4=4 | Es lo que hace que las órdenes lleguen más rápido de lo que los dos técnicos las atienden, y por tanto lo que llena la cola. Las **tasas de fallo de Cristian quedaron intactas** | **Implementada** |
| 2026-10-18 | 7, paso 5 | Los ticks desde el mantenimiento se reinician **solo si la orden atendida era P4** | Una reparación correctiva arregla el fallo pero no es un servicio preventivo completo. Sin esta distinción, cada reparación aplazaba el preventivo y el carril P4 nunca se usaba | **Implementada**, aprobada por Kevin (opción 2) |
| 2026-10-18 | 7, paso 1 | Acotar el factor de desgaste a 2 en la fórmula de probabilidad de fallo | Descubierto al medir: con el paso 5 anterior, la fórmula se vuelve un lazo de realimentación. El preventivo es la prioridad más baja, así que puede quedar esperando; entonces el contador crece, la máquina falla más, la cola se llena de trabajo correctivo y el preventivo se aleja más. Sin techo, la planta se saturaba (promedio de 8,2 órdenes en cola) | **Implementada** |
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
| 2 | Resto de las entidades del modelo y clase de configuración. `Prioridad` y `OrdenTrabajo` ya quedaron hechas en la etapa 1, porque la cola con prioridad las necesitaba | **Terminada** (2026-10-18) |
| 3 | `GeneradorFallos`, `Clasificador`, comandos y `Planta.tick()` probado sin interfaz | **Terminada** (2026-10-18) |
| 4 | Interfaz Swing: los paneles de las estructuras | **Terminada** (2026-10-18) |
| 5 | Controlador, reloj e integración | **Terminada** (2026-10-18) |
| 6 | Manual de usuario, documento explicativo, cierre de `USO_IA.md` y guion del video | Pendiente |
