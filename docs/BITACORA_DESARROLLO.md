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

## Entrega 2, commit 16 — 2026-11-10 — Overhauls en el tick, y un hallazgo de calibración

### Decisiones
- **Umbral de overhaul de 12 a 30 y máximo de overhauls de 3 a 1.** Medido con 1000 ticks y 3
  semillas: con 12, la mitad de las máquinas lo cruzaba antes del tick 250 y todas al final; con 3
  overhauls, la recomendación de reemplazo no podía aparecer en una partida.
- **Costo del overhaul: 120.000**, cobrado al despachar el camión.
- **Descarte perezoso** de la P4 de una máquina que recibió overhaul, y de la solicitud de una
  compañía en quiebra que llega a la cima: se extraen por la operación normal y se descartan, sin
  sacar nada del medio de la estructura.
- **Regla de la cima:** si la máquina de la cima no está libre (tiene alarma o no está operando),
  el camión espera; no se salta a la segunda solicitud.

### Hallazgo: el desgaste casi no cambia la economía
Partida automática, 3 semillas × 3 compañías:

| | Sin desgaste (14b) | Con desgaste (16) |
|---|---|---|
| Fallos por compañía | 177–196 | 182–214 |
| Caja final | 1,76 a 2,00 millones | 1,59 a 1,91 millones |

Dos causas: una máquina **en alarma sigue produciendo al 100 %**, así que un fallo casi solo
cuesta mano de obra; y los técnicos están **saturados**: hacia el tick 700 casi todas las máquinas
esperan en alarma, y una máquina con alarma no desarrolla otro fallo, así que "fallar el doble"
casi no suma. Por lo mismo, en automático la máquina de la cima casi nunca está libre. El umbral
de 30 se cruza hacia el tick 600, no hacia el 400 como se había estimado.

La mecánica funciona y está probada; lo que falta es calibración. Se deja para el **14c**, con una
propuesta de diseño: una máquina en alarma produce a media capacidad.

## Entrega 2, decisión — 2026-11-10 — El modo manual es un juego de gestión

Al proponer el commit 14 apareció que el equipo tenía dos ideas distintas del modo manual. La de
`CLAUDE.md` era apagar tres pasos del tick y poner un botón para cada uno: el usuario hacía a mano lo
mismo que la simulación, sin decisiones reales. La de Kevin es un juego de gestión: asignar técnicos,
llamar a la fábrica, más adelante comprar recursos, y analizar costos. Se adoptó la segunda, que le
da sentido a la vista de juego.

### Decisiones
- **Regla central: el jugador decide qué, cuándo y con qué recurso, y paga; la estructura decide
  cuál elemento sale.** Escoger libremente el técnico o la orden convertiría las colas en adorno, y
  es lo que el profesor evalúa. Elegir la **cola** (la especialidad) sí es válido: se desencola su
  frente.
- **No hace falta un motor de juego.** Un juego de gestión es simulación + palancas con efectos que
  se contraponen + una medida del resultado. Al revisar las palancas apareció que el **overhaul y el
  reemplazo no rendían**: el puntaje no afectaba la probabilidad de fallo, así que solo costaban.
  Se agregan dos reglas de desgaste acotadas (historial de fallos, hasta × 2, y +25 % de tasa por
  cada overhaul).
- **Tipo de fallo en el sensor** (`MECANICO` / `ELECTRICO`), no al azar: es realista, el generador
  no cambia y el clasificador tampoco (el tipo decide quién repara, no la prioridad).
- **Dos colas FIFO de técnicos**, una por especialidad. La alternativa, una sola cola buscando al de
  la especialidad, cuesta O(n) y saca un elemento que no es el frente. En automático la orden espera
  a su especialidad (regla de la cima); en manual el jugador puede usar la otra con penalidad × 2.
- **Economía por compañía con quiebra**, con ingresos por producción (sin ingresos el presupuesto
  solo bajaría y siempre habría quiebra). **Partidas de 1000 ticks** con un resumen final.
- **El automático es la referencia**, con una política ingenua a propósito. La comparación entre
  partidas se hace a mano por ahora; guardarlas (por ejemplo con SQLite) queda como opción D, que hoy
  contradice las secciones 3 y 10.
- **El historial guarda dos series**: puntaje de fallos y costo. Son dos instancias del mismo
  `ArbolSegmentos`; el pico de gasto refuerza la justificación frente a las sumas prefijas.
- **Dificultad** elegida al reiniciar, como multiplicadores. Va al final (14c) y se pospone si al
  calibrar no se nota.
- Recursos como aceite y repuestos quedan como opción C para la entrega final.

### Hecho
- `CLAUDE.md`: secciones 1, 7, 10, 16 y 17. Commits nuevos 14a, 14b y 14c; cambian 14, 16, 17, 18,
  20 y 23.

### Pendiente
- Cristian: revisar el tipo de fallo de cada sensor y calibrar la economía y la dificultad.

## Entrega 2, decisión — 2026-11-10 — Se pospone el árbol n-ario y se pueden crear compañías y máquinas
**Responsable:** Kevin (decisión del equipo), redacción con Claude Code

### Decisiones
- **El árbol n-ario se pospone (opción A).** La jerarquía organización → compañías → máquinas tiene profundidad
  fija de 2. Un `ArrayList<Planta>`, donde cada planta tiene su inventario, ya es ese árbol de forma implícita, y
  los totales por compañía son un ciclo. Con 3 a 5 compañías, ante la pregunta "¿por qué no una lista?", la
  respuesta honesta era "daría lo mismo". Una estructura difícil de defender resta más de lo que suma.
- **Sin el árbol n-ario, la Entrega 2 conserva tres estructuras no lineales bien justificadas:** el `TreeSet` del
  ranking, la `PriorityQueue` de los overhauls y el `ArbolSegmentos` del historial.
- **Se pueden crear compañías y máquinas, pero no eliminarlas.** Crear agrega hermanos, no niveles, así que no
  justifica el árbol n-ario. Lo que sí aparece es la necesidad de nombres únicos, y eso se resuelve con un
  **conjunto** (`TreeSet<String>`): uno para los nombres de las compañías y uno por compañía para las etiquetas.
  Cuesta O(log n), contra O(n) de recorrer la lista. Eliminar se descarta porque `ArrayList.remove` corre los
  índices, que son los ids de la API, y porque sacar una solicitud del montículo que no está en la cima viola el
  tipo abstracto de datos.
- **Se usa `TreeSet` y no `TreeMap`:** la API identifica las máquinas por índice (O(1) en el `ArrayList`), así que
  no hace falta buscar por etiqueta; solo hay que verificar que no se repita.
- **Opción B para la entrega final:** el árbol n-ario vuelve si la jerarquía gana niveles variables (sedes y líneas
  de producción). Ahí sí cada operación tiene un uso real: postorden para los totales por rama y preorden para el
  tick y el dibujo. Al equipo le interesa porque es una estructura que no se suele ver en el curso.

### Hecho
- Se actualizaron las secciones 1, 4, 10, 16 y 17 de `CLAUDE.md`. El plan de commits no se renumeró: el 9 queda
  pospuesto, se agregan 13a, 13b y 19a, y la opción B es la etapa G.

### Pendiente
- **Siguiente paso: commit 10**, el árbol de segmentos. Primero se propone su API.
- Commit 13b: decidir si una compañía nueva arranca con las 6 máquinas por defecto o vacía.

---

## Entrega 2, commit 2 — 2026-11-05 — El proyecto ya es Spring Boot
**Responsable:** Kevin, con Claude Code

### Hecho
- `pom.xml` hereda de `spring-boot-starter-parent` **4.1.2**, la última estable en Maven Central el 5 de noviembre.
  Solo usa `spring-boot-starter-webmvc` y `spring-boot-starter-webmvc-test`. JUnit ya no se declara: llega por el
  starter de pruebas.
- Se agregó `AplicacionPlanta` (punto de entrada de Spring) con una prueba de humo de que el contexto carga.
- La interfaz Swing **sigue en el código**: se retira en el commit 3.

### Decisiones
- En Spring Boot 4 los starters cambiaron de nombre: `spring-boot-starter-webmvc` reemplaza a
  `spring-boot-starter-web`, y MockMvc viene en `spring-boot-starter-webmvc-test`.
- Mientras convivan dos `main` (el de `App` y el de `AplicacionPlanta`), la clase principal se declara en
  `spring-boot-maven-plugin`. El commit 3 quita esa línea.

### Verificación
- `./mvnw clean test`: **63 pruebas**, que son las 62 de antes más la nueva.
  Surefire muestra "0 pruebas" para `ColaCircularTest` y `ColaPrioridadOrdenesTest` porque solo contienen clases
  `@Nested`, que reporta aparte. **No es una pérdida:** se comprobó sumando el atributo `tests` de los
  `TEST-*.xml` antes y después del cambio.
- `./mvnw clean javadoc:javadoc`: sin advertencias.
- `java -jar target/simulador-planta.jar` arranca Tomcat en ~1,4 s. Responde 404 porque todavía no hay endpoints.

### Incidencias
- Un `git push` falló con un error de permisos y funcionó al reintentarlo. Fue un fallo pasajero de conexión,
  no de credenciales.

### Pendiente
- **Siguiente paso: commit 3**, retirar la interfaz Swing. El plan detallado se presentó al equipo, pero **aún no está
  aprobado**. Está en `docs/sesiones/2026-11-05.md`, sección "Dónde quedamos".

---

## Entrega 2, etapa A — 2026-11-05 — Migración a Spring Boot y diseño con árboles
**Responsable:** Kevin (decisiones del equipo), redacción con Claude Code

### Hecho
- Se registró en `CLAUDE.md` el diseño completo de la Entrega 2 (sección 16) y su plan de 25 commits pequeños (sección 17).
- Se actualizaron las secciones 1, 3, 4, 9, 10, 11, 12, 13 y 15 para reflejar la migración.
- Se creó la rama `entrega-2`.

### Decisiones
- **Migrar a Spring Boot y separar el frontend en otro repositorio.** En la Entrega 1 quedó claro que el profesor
  evalúa las estructuras de datos: su justificación, su uso y su implementación. El framework no suma ni resta, pero
  una interfaz atractiva sí suma. Una interfaz web permite íconos, animaciones y dibujar árboles con mucho menos
  esfuerzo que Swing. El modelo no cambia porque nunca dependió de la vista (regla 1 de la sección 4).
  Se consideró `HttpServer` del JDK sin dependencias, y se descartó: habría que escribir a mano el JSON y el enrutamiento.
- **La concurrencia ahora existe y se controla con un único dueño sincronizado.** Tomcat usa varios hilos, así que
  `ServicioSimulacion` serializa todo acceso al modelo. Reemplaza la garantía del EDT de Swing.
- **Estructuras de la Entrega 2:**
  - árbol n-ario propio para la jerarquía de compañías;
  - `TreeSet` para el ranking por puntaje de fallos;
  - `PriorityQueue` con secuencia para las solicitudes de overhaul;
  - `ArrayDeque` para los técnicos del fabricante;
  - árbol de segmentos propio para el historial.
  Las justificaciones y las alternativas descartadas están en la sección 16.
- **El ranking se actualiza cuando hay un evento, no cada cierto tiempo:** eliminar → sumar → insertar, solo al
  clasificar un fallo, al terminar un overhaul o con un reemplazo. Reconstruir el orden en cada tick costaría
  O(n log n) aunque no haya fallos nuevos.
- **Fallo ponderado por severidad:** P1 = 3, P2 = 2, P3 = 1, P4 = 0.
- **Técnicos del fabricante fijos y compartidos**, no creados bajo demanda: sin escasez no se forma cola y el
  montículo no decide nada.
- **El árbol de segmentos se justifica por el máximo en un rango, no por la suma:** como los fallos solo se agregan en
  el tick actual, las sumas prefijas resolverían los totales en O(1).
- **Modo manual que se alterna en vivo.** El usuario dispara operaciones del tipo de dato, nunca elige elementos.

### Pendiente
- Crear la etiqueta `entrega-1` sobre `main` antes del commit 2 (requiere permiso del equipo).
- Cristian: valores de las compañías, del overhaul y de los umbrales (sección 16, "Valores iniciales").
- Definir en el commit 16 qué pasa con una orden P4 pendiente de una máquina que entra en overhaul.
- Confirmar la fecha de presentación de la Entrega 2.

---

## Verificación — 2026-10-18 — Prueba de clonación limpia

Antes de pasarle el repositorio a Cristian se clonó desde GitHub en una carpeta vacía y se
construyó desde cero, tal como lo haría él. Resultado: `./mvnw test` pasa las 60 pruebas, `./mvnw
package` genera el jar y la aplicación abre. El `mvnw` conserva su permiso de ejecución y no hace
falta instalar Maven.

### Incidencia: las verificaciones de Javadoc anteriores no eran válidas

El clon limpio destapó **31 advertencias de Javadoc** que las verificaciones anteriores no habían
mostrado. La causa: el plugin omite regenerar la documentación si la salida en `target/` está más
reciente que las fuentes, así que las ejecuciones repetidas de `javadoc:javadoc` devolvían
`BUILD SUCCESS` sin volver a analizar nada.

Las 31 advertencias eran campos privados sin comentario, todos en el paquete `vista`; en `modelo`
sí estaban documentados desde el principio. Ya quedaron corregidos y la verificación se repitió con
`./mvnw clean javadoc:javadoc`, que fuerza la regeneración.

**Para futuras verificaciones: usar siempre `clean` antes de `javadoc:javadoc`.** Sin eso el
resultado no significa nada.

---

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

Subir las máquinas aumenta cuántas órdenes pueden acumularse, pero no llena la cola: para eso hace falta que las
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

- Una máquina con anomalía activa no desarrolla otra, y una máquina en alarma no genera eventos
  nuevos; detenida o en mantenimiento, tampoco genera lecturas.
- Por lo tanto, con **tres** máquinas hay como máximo tres fallos simultáneos en toda la planta.
- Con **dos** técnicos que reciben trabajo en el mismo tick en que la orden se clasifica, la cola no
  llegó a acumular más de una orden.

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
| 2026-10-26 | 9, «Inyectar fallo» | Rechazar la inyección en una máquina con anomalía o alarma activa: el modelo lo registra en la bitácora y el botón se deshabilita | Al probarlo se vio que `Maquina` guarda una sola anomalía y la inyección la **reemplazaba** sin comparar severidades: una severa seguida de una leve salía como P3; una severa inyectada sobre una máquina en alarma cambiaba la lectura pero nunca se clasificaba (la orden seguía en P3); y una inyectada durante el mantenimiento se borraba al terminar la reparación. El generador aleatorio ya respetaba esta regla; solo la inyección del operador la rompía. Soportar varios fallos por máquina queda para otra entrega | **Implementada**, aprobada por Kevin |
| 2026-11-05 | 3 y 4 (stack y arquitectura) | Migrar de Swing a Spring Boot; el frontend pasa a otro repositorio; las dependencias permitidas pasan a ser los starters web MVC y de pruebas | Una interfaz web hace más atractivo el proyecto, y eso suma en la evaluación; el framework es neutral. El modelo no cambia | **Aprobada** por el equipo; `CLAUDE.md` ya actualizado |
| 2026-11-05 | 4, regla 4 (reloj en el EDT) | El reloj pasa a ser `@Scheduled`, y todo acceso al modelo se serializa en `ServicioSimulacion` con métodos `synchronized` | Con Tomcat hay varios hilos; el EDT ya no existe | **Aprobada**; `CLAUDE.md` ya actualizado |
| 2026-11-05 | 5 y 15 (`PriorityQueue` prohibida) | Permitir `PriorityQueue` solo para las solicitudes de overhaul, con número de secuencia como desempate | Los montículos ya se vieron en el curso, y la secuencia resuelve el orden de llegada entre empates | **Aprobada**; `CLAUDE.md` ya actualizado |
| 2026-11-10 | 10, 16 y 17 (árbol n-ario) | Posponer el árbol n-ario a una opción B opcional; las compañías van en un `ArrayList`; se pueden crear compañías y máquinas con nombres únicos en un `TreeSet`; no se eliminan | Con profundidad fija de 2, una lista hace lo mismo que el árbol y no se puede defender. El árbol se justifica con niveles variables | **Aprobada** por Kevin; `CLAUDE.md` ya actualizado |
| 2026-11-10 | 16, «Valores iniciales» | `maximoOverhauls` sube de 2 a 3 | Decisión del equipo al aprobar el commit 11: una máquina recibe hasta tres overhauls antes de que se recomiende reemplazarla | **Aprobada** por Kevin; `CLAUDE.md` ya actualizado |
| 2026-11-10 | 17 (orden de los commits) | Pausa de QA con el frontend después del 13b, y adelantar 19 → 19a → 21 antes del 14 | La vista pesa mucho en la nota. El frontend (un juego con cámara fija, en React Three Fiber) debe poder correr todo cuanto antes, y el QA manual detecta problemas que las pruebas no ven | **Aprobada** por Kevin; `CLAUDE.md` ya actualizado |
| 2026-11-10 | 16, «Puntaje de fallos» (desempate del ranking) | Las máquinas se identifican con la clave compuesta `(idCompania, indice)`; los dos rankings usan un solo comparador: puntaje, compañía, índice. Antes: compañía y etiqueta | La clave queda hecha de números que nunca cambian y coinciden con la API; ya no depende de que las etiquetas sean únicas, lo que deja libre la opción B (sedes y líneas) | **Aprobada** por Kevin; `CLAUDE.md` ya actualizado |
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

**Entrega 2:** el control se lleva por commits, según la sección 17 de `CLAUDE.md`.

| Etapa | Commits | Contenido | Estado |
|---|---|---|---|
| A | 1 | Decisión y diseño | **Terminada** (2026-11-05) |
| B | 2–8 | Migración a Spring Boot con paridad de la Entrega 1 | **Terminada** (2026-11-07) |
| C | 9–10 | `ArbolSegmentos` (el 9, `ArbolNario`, se pospuso a la etapa G) | **Terminada** (2026-11-10) |
| D | 11–18, 13a, 13b, 14a–14c | Modelo de la Entrega 2: creación de compañías y máquinas, y el juego de gestión | 11, 12, 13, 13a, 13b, 14, 14a, 14b, 15 y 16 **terminados** (2026-11-10). Sigue el **17**; faltan 14c, 17 y 18 |
| — | — | **Pausa de QA con el frontend** (`ProyectoFinalED-frontend`, contra la API de la Entrega 1) | **Terminada** (2026-11-10): hito 1 del frontend (HUD, sondeo y escena isométrica) |
| E | 19–23, 19a | API de la Entrega 2. Se adelantan **19 → 19a → 21**, antes del 14 | 19, 19a y 21 **terminados** (2026-11-10). Faltan 20, 22 y 23 |
| F | 24–25 | Documentación | Pendiente |
| G | G1–G3 | Opción B: jerarquía con árbol n-ario (opcional, entrega final) | Opcional |
