# Simulador de Monitoreo y Mantenimiento de Planta

Proyecto del curso **Estructuras de Datos** — Universidad Nacional de Colombia.
Equipo: **Cristian Robayo** y **Kevin Toro**.

Este documento es la fuente de verdad del diseño. Cualquier cambio de decisión se registra aquí antes de implementarlo.

**Estado:** la Entrega 1 está cerrada (versión Swing, etiqueta de Git `entrega-1`). La Entrega 2 está en curso en la rama `entrega-2`: el proyecto migra a **Spring Boot** y este repositorio queda como **backend**. El frontend vive en un repositorio aparte. El diseño de la Entrega 2 está en la sección 16 y su plan de commits en la sección 17.

---

## 1. Contexto y requisitos de la Entrega 1

**Presentación:** jueves 22 de octubre de 2026, en clase.

Requisitos del enunciado:
- Usar **al menos tres** estructuras lineales: arreglos dinámicos, pilas, colas, colas circulares, colas con prioridad. **Este proyecto usa las cinco.**
- Lenguaje: Java. Se permiten las estructuras de la librería estándar y código generado con IA, **declarando el porcentaje generado por IA**.
- Interfaz de usuario muy intuitiva (gráfica, idealmente).
- Repositorio en GitHub con:
  - documentación en línea de código (Javadoc),
  - manual de usuario,
  - documento explicativo del **por qué y cómo** se usó cada estructura,
  - video de máximo 5 minutos.

Lo que evalúa el profesor: **el funcionamiento y la claridad de la explicación**. Cada decisión de este documento debe poder defenderse en voz alta.

El proyecto es incremental: las entregas 2 y 3 añadirán estructuras sobre esta base. Esta entrega se limita a estructuras **lineales**.

### Entrega 2

**Presentación:** fecha por definir.

- Temas vistos en el curso que se pueden usar: árboles (n-arios, de búsqueda, balanceados, B), montículos, conjuntos y árboles de segmentos. Se siguen permitiendo las estructuras de la librería de Java.
- Lo que evalúa el profesor: que **las estructuras de datos queden muy claras**, es decir por qué se eligió cada una, cómo se usa y cómo está implementada. El framework no suma ni resta. Que el proyecto se vea atractivo **sí suma**.
- Decisión del 5 de noviembre de 2026: **migrar a Spring Boot** y separar el frontend en otro repositorio. Así la interfaz puede ser web, más visual e interactiva, y el modelo y las estructuras no cambian. Ver la sección 16 y `docs/BITACORA_DESARROLLO.md`.

---

## 2. Qué hace el programa

Simula una planta con **6 máquinas** que reportan lecturas de sensores. El sistema:
1. Genera fallos aleatorios en las máquinas.
2. Detecta anomalías en las lecturas.
3. Las clasifica por prioridad (P1 a P4), con un esquema inspirado en ISA-18.2 (prioridad según severidad y tiempo de respuesta).
4. Genera órdenes de mantenimiento y las asigna a los técnicos disponibles.

**Cada estructura de datos tiene su propio panel en la interfaz, que muestra su comportamiento real** (FIFO, LIFO, prioridad, sobrescritura circular, acceso por índice).

---

## 3. Stack técnico

- **Java 21** (LTS), proyecto **Maven**, IDE **IntelliJ IDEA**.
- **Desde la Entrega 2: Spring Boot**, en la última versión estable de la línea 4.x, que se fija en el `pom.xml`.
  Las únicas dependencias permitidas son el starter **web MVC**, que trae Tomcat y Jackson para JSON, y el starter
  **de pruebas**, que trae JUnit Jupiter y MockMvc. No se agregan bases de datos, seguridad, Lombok ni otros starters.
- Interfaz: un frontend web en un **repositorio aparte**, que consume la API REST de este repositorio.
  La interfaz Swing de la Entrega 1 se retira del código y queda en la etiqueta `entrega-1`.
- Pruebas: JUnit para las estructuras y el modelo; MockMvc para los endpoints.

---

## 4. Arquitectura: MVC con la vista en otro repositorio

Paquete raíz: `edu.unal.planta`

```
edu.unal.planta
├── AplicacionPlanta         → punto de entrada de Spring Boot
├── estructuras/             → estructuras propias, genéricas y sin dependencias del dominio
│                              (ColaCircular; en la Entrega 2, ArbolNario y ArbolSegmentos)
├── modelo/                  → dominio y simulación. Java puro: NO importa Spring, javax.swing ni java.awt
│   └── comandos/            → acciones del operador que se pueden deshacer
├── servicio/                → ServicioSimulacion: dueño único del modelo, reloj y acceso serializado
└── api/                     → controladores REST (@RestController) y manejo de errores
    └── dto/                 → records inmutables que viajan como JSON
```

La **vista** es el frontend web, en otro repositorio. Los **controladores** son las clases de `api/`, que reciben
las acciones del usuario y llaman al servicio.

La Entrega 1 tenía `App`, `vista/` (Swing) y `controlador/` (`javax.swing.Timer`). Se conservan en la etiqueta `entrega-1`.

### Reglas de la arquitectura (no se negocian)
1. **El modelo no conoce la vista ni el framework.** `modelo/` y `estructuras/` no importan nada de Spring. Por eso la migración no cambia el modelo y sus pruebas siguen pasando tal cual. `ObservadorPlanta` se conserva en el modelo, aunque la API no lo necesita: consulta el estado cuando se lo piden.
2. **La API solo expone DTOs.** Ningún endpoint devuelve un objeto del modelo. Los DTOs son `record` inmutables que se construyen copiando el estado, igual que en la Entrega 1 la vista recibía copias o vistas no modificables.
3. **Todas las acciones del usuario pasan por un endpoint**, que llama al servicio, y el servicio llama al modelo. Los controladores REST no tienen lógica de simulación.
4. **Un único dueño del modelo y acceso serializado.** Tomcat atiende cada petición en un hilo distinto y el reloj corre en otro, así que **sí hay concurrencia**. `ServicioSimulacion` es el único que guarda el modelo, y todos sus métodos públicos son `synchronized`, incluido el tick: una petición nunca ve un tick a medias, y dos acciones nunca se mezclan. Es la misma garantía que daba el EDT de Swing en la Entrega 1. Los DTOs se construyen **dentro** del método sincronizado, para que la foto sea consistente.
5. **El reloj es un `@Scheduled`** con un intervalo fijo configurado en `application.properties`. En cada disparo llama al tick del servicio solo si la simulación está en ejecución. No se crean hilos a mano.
6. **Aleatoriedad reproducible:** una semilla configurable. Con varias compañías hay una **semilla maestra**, y de ella se derivan las semillas de cada compañía de forma determinista. La misma semilla produce la misma simulación.
7. **El frontend consulta por sondeo** (`GET` periódico del estado). No se usan WebSockets ni SSE en esta entrega.
8. El contrato de la API se documenta en `docs/API.md`, que es lo que lee el repositorio del frontend.

---

## 5. Estructuras de datos (decisión final)

| Estructura | Implementación | Uso en el proyecto | Justificación |
|---|---|---|---|
| Arreglo dinámico | `ArrayList<Maquina>` | Inventario de máquinas | La operación más frecuente es recorrer todas las máquinas en cada tick y acceder por índice: O(1). Agregar al final cuesta O(1) amortizado. Las máquinas no se eliminan del inventario; solo cambian de estado. |
| Cola circular | **`ColaCircular<T>` propia** | Últimas N lecturas de cada máquina | Memoria fija: la lectura nueva sobrescribe la más antigua. Así funcionan los registradores de datos reales. **Java no provee una cola circular que sobrescriba**; por eso es la única estructura propia. |
| Cola FIFO | `ArrayDeque` (declarada como `Queue`) | Eventos por clasificar y técnicos disponibles | Orden de llegada estricto; encolar y desencolar en O(1). Más eficiente que `LinkedList`, que crea un nodo por elemento. |
| Cola con prioridad | **Arreglo de 4 colas** (`ArrayDeque`), índice = prioridad | Órdenes de trabajo P1–P4 | Insertar O(1). Extraer recorre 4 niveles fijos: O(k) con k = 4, constante en la práctica. El orden de llegada dentro de cada prioridad se cumple solo, porque cada nivel es FIFO. |
| Pila | `ArrayDeque` (`push`/`pop`) | Deshacer acciones del operador | La última acción es la primera en revertirse (LIFO); O(1). |

### Alternativas descartadas (para responder preguntas)
- **`PriorityQueue`:** internamente es un heap (aún no visto en el curso) y **no garantiza orden de llegada** entre elementos de igual prioridad.
- **Arreglo ordenado como cola con prioridad:** insertar cuesta O(n) por el desplazamiento de elementos.
- **`Stack`:** clase heredada de `Vector`, sincroniza todos sus métodos (costo innecesario) y permite acceso por índice, lo que rompe el concepto de pila. La documentación de Java recomienda `Deque`.
- **`LinkedList` como cola:** funciona, pero crea un objeto nodo por elemento. `ArrayDeque` es más eficiente.
- **`ArrayBlockingQueue` como cola circular:** internamente es circular, pero está diseñada para concurrencia, no sobrescribe (rechaza cuando está llena) y no expone sus índices.
- **Crecimiento de `ArrayList`:** al llenarse crea un arreglo 1,5 veces más grande y copia los elementos. Por eso agregar al final es O(1) *amortizado* y no O(1) puro.

### Especificación de `ColaCircular<T>` (propia)
- Arreglo interno de **capacidad fija** definida al construirla.
- Mantiene `frente` (índice del elemento más antiguo), `tamaño` y un contador de sobrescrituras. El índice `final` se calcula como `(frente + tamaño) % capacidad`.
- **Encolar:** si hay espacio, escribe en `final`. Si está llena, sobrescribe la posición `frente`, avanza `frente` una posición (módulo capacidad) e incrementa el contador de sobrescrituras. Siempre O(1).
- **Desencolar** y **ver frente:** O(1). Lanzan excepción si está vacía.
- Consultas de solo lectura para la vista: capacidad, tamaño, índice frente, índice final, contenido de la celda i (puede ser vacía), elementos en orden lógico (del más antiguo al más reciente), número de sobrescrituras.
- Pruebas obligatorias: cola vacía, llenado exacto, sobrescritura, varias vueltas completas, desencolar después de dar la vuelta, capacidad 1.

### Especificación de la cola con prioridad (clase `ColaPrioridadOrdenes` en `modelo`)
- Arreglo de 4 `ArrayDeque<OrdenTrabajo>`: índice 0 = P1, … 3 = P4.
- **Insertar(orden):** encola en la cola de su nivel.
- **Extraer():** recorre los niveles desde P1 y saca el frente de la primera cola no vacía. Devuelve vacío si no hay órdenes.
- **Ver siguiente** (sin extraer), tamaño total, tamaño por nivel, contenido de cada nivel en solo lectura (para la vista).
- Pruebas obligatorias: orden por prioridad, FIFO dentro del mismo nivel, una P1 insertada de última sale primero, extraer con la estructura vacía.

---

## 6. Modelo de dominio

| Clase | Responsabilidad | Datos principales |
|---|---|---|
| `Maquina` | Estado y parámetros de una máquina | etiqueta (ej. `PRENSA-01`), tipo, estado, carga % (0–100), ticks desde el último mantenimiento, intervalo preventivo, tasa base de fallo, sensores, `ColaCircular<Lectura>`, anomalía activa (si existe), indicador de alarma activa |
| `Sensor` | Definición de una variable medida | nombre, unidad, valor nominal, ruido, umbral de advertencia, umbral alto, umbral crítico |
| `Lectura` | Medición de todos los sensores en un tick | tick, valor por sensor |
| `Anomalia` | Fallo físico en curso en una máquina | sensor afectado, magnitud (leve/moderada/severa), desplazamiento que aplica a la lectura |
| `EventoFallo` | Detección cruda, **sin prioridad** | máquina, sensor, valor leído, tick |
| `OrdenTrabajo` | Evento ya clasificado | id, máquina, prioridad, descripción, tick de creación, duración estimada |
| `Prioridad` (enum) | P1 CRÍTICA, P2 ALTA, P3 MEDIA, P4 BAJA | nivel (0–3), descripción, acción, color |
| `EstadoMaquina` (enum) | OPERANDO, EN_ALARMA, DETENIDA, EN_MANTENIMIENTO, PARADA_MANUAL | — |
| `Tecnico` | Persona de mantenimiento | nombre, orden actual, ticks restantes |
| `GeneradorFallos` | Decide si aparece una anomalía y su magnitud | usa el `Random` con semilla |
| `Clasificador` | Convierte `EventoFallo` → `OrdenTrabajo` | reglas de umbrales (sección 7) |
| `Planta` | Fachada del modelo: contiene todas las estructuras y ejecuta `tick()` | inventario, colas, pila, bitácora, tick actual, observadores |

**Por qué `EventoFallo` y `OrdenTrabajo` están separados:** así funciona en la industria. El PLC detecta, el SCADA clasifica y el sistema de mantenimiento genera la orden. Esta separación justifica que existan dos colas distintas.

### Máquinas iniciales (valores PROVISIONALES — los define Cristian)

Son **6 máquinas** y no 3. El cambio se decidió el 18 de octubre, tras medir la simulación: una máquina con anomalía activa no desarrolla otra, así que con 3 máquinas y 2 técnicos la cola con prioridad nunca pasaba de **una** orden y los paneles de colas se veían vacíos en toda la demostración. Con 6, la cola llegó a un máximo de 4 órdenes en la medición (no es un tope: una máquina puede tener a la vez una correctiva y un preventivo P4). Ver `docs/BITACORA_DESARROLLO.md`.
Todos los parámetros deben estar en **un solo lugar** (una clase de configuración o fábrica de máquinas) para poder ajustarlos sin tocar la lógica.

| Etiqueta | Tipo | Sensores | Criticidad |
|---|---|---|---|
| `PRENSA-01` | Prensa hidráulica | temperatura aceite (°C), presión (bar), vibración (mm/s) | Alta |
| `TORNO-01` | Torno CNC | temperatura husillo (°C), corriente motor (A), vibración (mm/s) | Media |
| `ROBOT-01` | Brazo robótico de soldadura | temperatura motor (°C), corriente (A), vibración (mm/s) | Media |
| `COMPRES-01` | Compresor de tornillo | temperatura descarga (°C), presión (bar), vibración (mm/s) | Alta |
| `HORNO-01` | Horno de tratamiento térmico | temperatura cámara (°C), temperatura carcasa (°C), consumo de gas (m³/h) | Media |
| `BOMBA-01` | Bomba centrífuga | temperatura rodamiento (°C), presión descarga (bar), vibración (mm/s) | Alta |

Condición para los valores: nominal + ruido debe quedar **claramente por debajo** del umbral de advertencia, para que no haya falsas alarmas sin anomalía.

Otros valores provisionales:
- Capacidad de la cola circular: **12 lecturas** por máquina.
- Técnicos: **2** (bastante menos técnicos que máquinas, para que se formen filas).
- Duración de reparación en ticks: P1 = 10, P2 = 8, P3 = 6, P4 = 4. Junto con las tasas de fallo, son los valores que deciden si la cola con prioridad forma filas visibles.

---

## 7. Simulación

### Orden exacto de un tick (`Planta.tick()`)
1. **Generar fallos.** Cada máquina OPERANDO **sin anomalía activa** desarrolla una con probabilidad
   `p = tasaBase × (carga / 100) × min(2, 1 + ticksDesdeMantenimiento / intervaloPreventivo)`.
   El factor de desgaste está **acotado a 2**: sin ese techo la fórmula se vuelve un lazo de realimentación, porque el preventivo es la prioridad más baja y puede quedar esperando, lo que hace fallar más la máquina y aleja todavía más el preventivo.
   Se elige un sensor al azar, y la magnitud con pesos **80 % leve / 15 % moderada / 5 % severa** (distribución recomendada por ISA-18.2 para tres prioridades).
2. **Leer sensores.** Cada máquina OPERANDO o EN_ALARMA genera una `Lectura` (nominal + ruido + desplazamiento de la anomalía) y la encola en su `ColaCircular`.
3. **Detectar.** Si la última lectura supera el umbral de advertencia en algún sensor **y la máquina no tiene alarma activa**, se crea un `EventoFallo`, se encola en la **cola de eventos** y la máquina pasa a EN_ALARMA. La regla de una sola alarma por máquina evita la inundación de alarmas.
4. **Clasificar.** Se desencola **un solo** evento por tick (así la cola puede acumularse y verse), se clasifica y se inserta en la **cola con prioridad**. Si es P1, la máquina pasa a DETENIDA de inmediato.
5. **Avanzar trabajos.** A cada técnico ocupado se le resta un tick. Al terminar: la máquina vuelve a OPERANDO, se borran la anomalía y la alarma, y el técnico se **encola al final** de la cola de técnicos.
   Los ticks desde el mantenimiento **solo se reinician si la orden atendida era P4**, es decir un preventivo. Una reparación correctiva arregla el fallo pero no es un servicio completo, así que el reloj del preventivo sigue corriendo. Decisión del 18 de octubre: sin esta distinción, cada reparación aplazaba el preventivo y el carril P4 no llegaba a usarse.
6. **Asignar.** Mientras haya técnicos libres y órdenes pendientes: se extrae la orden más prioritaria, se desencola el primer técnico y la máquina pasa a EN_MANTENIMIENTO.
7. **Preventivo.** Si una máquina supera su intervalo preventivo y no tiene orden P4 pendiente, se genera una orden P4 directamente (no es un fallo, no pasa por la cola de eventos).
8. Incrementar el tick y **notificar a los observadores**.

### Reglas de clasificación
El clasificador **no conoce** los pesos del generador; solo compara el valor leído contra los umbrales del sensor. Por eso funcionaría igual con datos reales.

| Valor leído | Prioridad | Acción |
|---|---|---|
| ≥ umbral crítico | P1 CRÍTICA | Paro inmediato de la máquina |
| ≥ umbral alto y < crítico | P2 ALTA | Atender en el turno |
| ≥ advertencia y < alto | P3 MEDIA | Ir a revisar |
| Intervalo preventivo cumplido | P4 BAJA | Mantenimiento programado |

La magnitud de la anomalía ubica la lectura en la zona correspondiente: leve → entre advertencia y alto, moderada → entre alto y crítico, severa → sobre crítico.

---

## 8. Acciones del operador y la pila de deshacer

Patrón **Comando**: cada acción deshacible guarda lo necesario para revertirse y se apila al ejecutarse.

**Se pueden deshacer:**
- Parar o arrancar manualmente una máquina (OPERANDO ↔ PARADA_MANUAL).
- Cambiar la carga de una máquina.

**No se pueden deshacer:** clasificar, asignar ni inyectar un fallo. Deshacerlas obligaría a reinsertar por el frente de una cola FIFO o a sacar un elemento que no es el siguiente de la cola con prioridad. Ambas cosas violan el tipo abstracto de datos. **Esta explicación debe aparecer en el documento explicativo.**

**Caso borde:** un comando solo se revierte si el estado actual sigue siendo el que el comando dejó. Si la simulación ya lo cambió (por ejemplo, un P1 detuvo la máquina), se descarta y se registra en la bitácora.

---

## 9. Interfaz

> **Desde la Entrega 2, la interfaz es el frontend web del otro repositorio.** El diagrama de abajo es la
> distribución de la ventana Swing de la Entrega 1 (etiqueta `entrega-1`). Lo que se conserva como requisito para
> el frontend es la lista "Qué muestra cada panel": cada estructura debe seguir viéndose con su comportamiento real.
> Las vistas nuevas de la Entrega 2 están en la sección 16.

```
┌───────────────────────────────────────────────────────────────────────┐
│ [▶ Iniciar/⏸ Pausar] [Paso] [Reiniciar]          Tick: 42  Semilla: 7 │
├───────────────┬──────────────────────────────────────────┬────────────┤
│ INVENTARIO    │ COLA CIRCULAR (máquina seleccionada)      │ PILA       │
│ (ArrayList)   │ celdas con índice, frente/final           │ (deshacer) │
│ celdas por    ├────────────────────┬─────────────────────┤ cima arriba│
│ índice        │ COLA DE EVENTOS    │ COLA DE TÉCNICOS    │            │
├───────────────┤ (FIFO)             │ (FIFO) + en trabajo │ [Deshacer] │
│ DETALLE       ├────────────────────┴─────────────────────┤            │
│ máquina selec.│ COLA CON PRIORIDAD: 4 carriles P1..P4     │            │
│ carga, parar, │                                           │            │
│ inyectar fallo│                                           │            │
├───────────────┴──────────────────────────────────────────┴────────────┤
│ BITÁCORA (texto: qué pasó en cada tick)                                │
└───────────────────────────────────────────────────────────────────────┘
```

Qué muestra cada panel:
- **Inventario (`ArrayList`):** una celda por índice con la etiqueta de la máquina y un color según su estado. Clic para seleccionar.
- **Cola circular:** arreglo fijo de 12 celdas con índices 0–11, punteros `frente` y `final`, el valor del sensor seleccionado coloreado por zona, y el contador de sobrescrituras. Debe verse el `final` dar la vuelta.
- **Cola de eventos:** cajas de izquierda a derecha; "sale" en el frente, "entra" al final.
- **Cola de técnicos:** técnicos libres en fila FIFO, y aparte los técnicos trabajando con su orden y los ticks restantes.
- **Cola con prioridad:** 4 carriles horizontales (P1 rojo, P2 naranja, P3 amarillo, P4 azul), cada uno una fila FIFO. Resaltar el carril del que sale la siguiente orden.
- **Pila:** vertical, con la cima arriba y la descripción de cada acción.
- **Bitácora:** una línea por evento relevante ("Tick 42: PRENSA-01 vibración 9.8 mm/s → P2").
- **Inyectar fallo:** el operador elige sensor y magnitud para la máquina seleccionada. Sirve para forzar casos en la demo.
  **Se rechaza si la máquina ya tiene una anomalía o una alarma activa** (el botón se deshabilita y el modelo registra el rechazo en la bitácora). Una máquina guarda un solo fallo: aceptar otro lo reemplazaría, y una falla severa podía desaparecer detrás de una leve. Decisión del 26 de octubre; soportar varios fallos por máquina queda para otra entrega. Ver `docs/BITACORA_DESARROLLO.md`.

**No dibujar estado interno que `java.util` no expone** (por ejemplo, la capacidad del `ArrayList`). Se muestra el comportamiento del tipo abstracto, no una memoria inventada.

---

## 10. Alcance

**Mínimo obligatorio (Entrega 1):**
- 6 máquinas fijas, 2 técnicos.
- Iniciar, pausar, paso a paso y reiniciar con la misma semilla.
- Los 6 paneles descritos.
- Inyectar fallo, parar o arrancar máquina, cambiar carga, deshacer.
- Bitácora.
- Pruebas JUnit de `ColaCircular` y `ColaPrioridadOrdenes`, y una prueba del `tick()` sin interfaz.

**Solo si sobra tiempo:**
- Agregar máquinas desde la interfaz (deshacible: quitar la última, O(1)).
- Gráfica de las lecturas de la cola circular.
- Control de velocidad de la simulación.

**Fuera de alcance (Entrega 1):** conexión a PLC reales, persistencia en archivos o base de datos, estructuras no lineales.

**Mínimo obligatorio (Entrega 2, backend):**
- Paridad con la Entrega 1 a través de la API REST: control de la simulación, estado de cada estructura y acciones del operador.
- Varias compañías en un árbol n-ario, con totales por rama.
- Ranking de máquinas por puntaje de fallos, por compañía y global, con su recomendación (overhaul o reemplazo).
- Solicitudes de overhaul atendidas por técnicos del fabricante, y reemplazo de máquinas.
- Historial de fallos consultable por ventana de ticks (árbol de segmentos).
- Modo manual que se puede activar y desactivar con la simulación en marcha.
- Pruebas JUnit de cada estructura nueva y del modelo; pruebas MockMvc de los endpoints.

**Fuera de alcance (Entrega 2):** autenticación de usuarios (el "superusuario" es una vista, no un login), persistencia, WebSockets, despliegue en la nube.

---

## 11. Plan de trabajo

> Plan de la **Entrega 1**, ya cumplido. El plan de la Entrega 2 son los commits de la sección 17.

| Día | Kevin | Cristian |
|---|---|---|
| Dom 18 | Crear el repo, el proyecto Maven y los paquetes; README y `docs/USO_IA.md` | Definir los valores de máquinas, sensores y umbrales (sección 6) |
| Lun 19 | `ColaCircular`, `ColaPrioridadOrdenes`, sus pruebas y las entidades del modelo | Revisar las reglas de fallo y los tiempos de reparación |
| Mar 20 | `Planta.tick()` completo y probado **sin interfaz** | Paneles de vista: primero cola circular y cola con prioridad |
| Mié 21 | Controlador, integración y documento explicativo | Manual de usuario, guion y grabación del video (≤ 5 min) |
| Jue 22 | Ensayo y presentación | Ensayo y presentación |

**Orden obligatorio:** estructuras → modelo probado → vista → controlador. No empezar la interfaz antes de que el tick funcione en pruebas.

---

## 12. Entregables (lista de verificación)

- [ ] Código en GitHub con Javadoc en todas las clases y métodos públicos
- [ ] `README.md`: descripción, requisitos, cómo compilar y ejecutar, capturas
- [ ] `docs/MANUAL_USUARIO.md`
- [ ] `docs/DOCUMENTO_EXPLICATIVO.md`: por qué y cómo se usó cada estructura (secciones 5, 7 y 8)
- [ ] `docs/USO_IA.md`: herramientas usadas, en qué partes y el porcentaje estimado
- [ ] Video de máximo 5 minutos (enlace en el README)

**Entrega 2 (este repositorio):**
- [ ] Javadoc en todas las clases y métodos públicos nuevos, con la complejidad de cada operación en las estructuras
- [ ] `docs/API.md`: el contrato de la API para el frontend
- [ ] `docs/DOCUMENTO_EXPLICATIVO.md`: una sección nueva con el por qué y el cómo de cada estructura de la Entrega 2. **Es lo que más evalúa el profesor**
- [ ] `README.md`: cómo ejecutar el backend y el enlace al repositorio del frontend
- [ ] `docs/USO_IA.md` actualizado
- [ ] El manual de usuario y el video dependen del frontend: se coordinan con ese repositorio

---

## 13. Convenciones de código

- Identificadores en español, sin tildes ni ñ (`Maquina`, `ColaCircular`, `tamano`). Comentarios y Javadoc en español, con tildes.
- Javadoc obligatorio en clases y métodos públicos. En las estructuras, indicar la **complejidad** de cada operación.
- Clases pequeñas con una sola responsabilidad.
- El modelo y las estructuras nunca importan `javax.swing`, `java.awt` ni `org.springframework`.
- Rutas de la API en español, sin tildes y en minúscula con guiones (`/api/companias/{id}/clasificar-siguiente`). Los campos JSON van en camelCase y en español.
- Constantes y parámetros de simulación centralizados, sin números sueltos en la lógica.

---

## 14. Registro del uso de IA

El enunciado exige declarar el porcentaje de código generado con IA, así que se registra desde el primer commit:
- Los commits hechos con Claude Code conservan la línea `Co-Authored-By` que agrega la herramienta. Los commits sin esa línea son escritos por el equipo.
- `docs/USO_IA.md` lleva una tabla por archivo o componente: quién lo escribió (IA / equipo / mixto) y qué se revisó o modificó a mano.

---

## 15. Instrucciones para el asistente (Claude Code)

- Trabajar **por etapas, una a la vez**, siguiendo el orden de la sección 11 (Entrega 1) o de la sección 17 (Entrega 2). Al terminar cada etapa: resumir qué se hizo y **esperar confirmación** antes de seguir. Antes de empezar una etapa, contar qué se va a hacer.
- **Commits pequeños y revisables (Entrega 2):** un commit de la sección 17 a la vez. Antes de cada commit: ejecutar `./mvnw test`, resumir el diff, pedir permiso y esperar la revisión manual del equipo. No juntar varios commits del plan en uno.
- **Mantenerlo simple.** Las pruebas cubren los casos que exige este documento, no todos los casos imaginables: unas pocas pruebas claras por estructura, sin casos defensivos extra salvo que el equipo los pida. Preferir la solución directa antes que la extensible. Antes de agregar clases, capas, interfaces o métodos que este documento no pida, **proponerlo** en vez de implementarlo. El criterio es que el equipo pueda explicar todo el código en clase.
- **Explicar cada clase que se cree:** su responsabilidad, la estructura que usa y la complejidad de sus operaciones. El equipo debe poder defender cada línea ante el profesor.
- Respetar las decisiones de las secciones 5 y 16. **No usar** `Stack`, `LinkedList` ni `Vector`. `PriorityQueue` se usa **solo** donde lo dice la sección 16 (solicitudes de overhaul, con secuencia como desempate); la cola P1–P4 sigue siendo la de la sección 5.
- No agregar dependencias distintas de las de la sección 3 (Spring Boot web MVC y el starter de pruebas).
- El modelo y las estructuras no importan Spring. Si una tarea parece exigirlo, la lógica va en `servicio/` o en `api/`.
- Si una decisión de este documento resulta inviable o contradictoria, **decirlo y proponer el cambio antes de implementarlo**, y actualizar este documento.
- Ejecutar las pruebas antes de declarar terminada una etapa.
- **No hacer `commit` ni `push` sin pedir permiso.**
- Al cerrar cada etapa, actualizar `docs/USO_IA.md` con los archivos generados.

---

## 16. Entrega 2: diseño

### Qué se agrega
Varias **compañías** con la misma lógica que la `Planta` de la Entrega 1, y un **superusuario** que las ve todas e interactúa con cada una. Lo que gana el superusuario:
- un dashboard propio;
- un ranking de máquinas por fallos;
- una recomendación de **overhaul** o **reemplazo** para cada máquina;
- un **modo manual** para operar la simulación a mano.

### Estructuras de la Entrega 2

| Estructura | Implementación | Uso en el proyecto | Justificación |
|---|---|---|---|
| Árbol n-ario | **`ArbolNario<T>` propio** (cada nodo guarda sus hijos en un `ArrayList`) | Jerarquía organización → compañías → máquinas | Es una jerarquía real. El tick global es un **preorden**, y los totales por rama del dashboard son un **postorden**: cada nodo suma lo de sus hijos. Agregar un hijo cuesta O(1) amortizado y los recorridos O(n). Java no trae un árbol n-ario genérico; por eso es propio, igual que `ColaCircular` en la Entrega 1. |
| Árbol de búsqueda balanceado | `TreeSet<Maquina>` (árbol rojo-negro) envuelto en `RankingFallos` | Ranking por puntaje de fallos: uno por compañía y uno global | El orden siempre está listo. Insertar y eliminar cuestan O(log n), y el recorrido en orden da el ranking en O(n). `tailSet`/`headSet` dan la consulta por rango "puntaje ≥ umbral", que son las candidatas a overhaul. La altura queda acotada a 2·log2(n+1) aunque las máquinas lleguen ordenadas. |
| Montículo | `PriorityQueue<SolicitudOverhaul>` (montículo binario) | Solicitudes de overhaul aprobadas, de todas las compañías | Los técnicos del fabricante son pocos y compartidos, así que cuando se acumulan solicitudes debe salir la máquina en peor estado. Insertar y extraer cuestan O(log n). El comparador desempata por **número de secuencia**, y eso garantiza el orden de llegada entre empates, que era la razón para descartarlo en la Entrega 1. |
| Cola FIFO | `ArrayDeque<TecnicoExterno>` | Técnicos del fabricante libres | Mismo patrón que los técnicos internos: al terminar, el técnico se encola al final. |
| Árbol de segmentos | **`ArbolSegmentos` propio** (arreglo de tamaño 2·potencia de 2; cada nodo guarda la suma y el máximo) | Historial de puntaje de fallos por tick: uno por compañía y uno global | Responde "total y **pico** de fallos entre el tick a y el b" en O(log n), con una ventana que elige el usuario. Actualizar una posición cuesta O(log n). El horizonte de ticks es fijo y se configura. |

### Alternativas descartadas (para responder preguntas)
- **Ordenar el inventario cada vez que se pide el ranking:** cuesta O(n log n) por consulta, y el frontend consulta en cada sondeo. El `TreeSet` lo cambia por O(log n) por fallo. El argumento es de **lecturas contra escrituras**: hay muchas más consultas que fallos.
- **AVL propio:** daría lo mismo que `TreeSet`, y como la librería está permitida, el rojo-negro es suficiente.
- **Montículo para el ranking:** da la peor máquina en O(1), pero no da la lista ordenada, y `PriorityQueue` no permite cambiar una clave en su lugar.
- **Sumas prefijas en vez del árbol de segmentos:** los fallos siempre se agregan en el tick actual, nunca en el pasado. Para **sumas** en un rango bastaría un arreglo de sumas prefijas: agregar O(1) y consultar O(1). **El árbol de segmentos se justifica por el máximo en un rango arbitrario**, que las sumas prefijas no pueden responder. Si alguna vez solo se pidieran totales, sobraría.
- **Árbol B:** está pensado para minimizar los accesos a disco con nodos grandes. Aquí todo está en memoria y hay pocas máquinas.
- **Técnicos del fabricante creados bajo demanda:** si siempre hay un técnico disponible, la cola nunca se forma y el montículo no tiene nada que decidir. Un número fijo de técnicos, compartidos entre compañías, es lo realista.

### Puntaje de fallos y recomendación
- `Prioridad` gana un **peso**: P1 = 3, P2 = 2, P3 = 1 y P4 = 0, porque un preventivo no es un fallo.
- `Maquina` gana dos campos: `puntajeFallos`, ponderado y acumulado desde el último overhaul, y `numeroOverhauls`.
- `Recomendacion` (enum `NINGUNA` / `OVERHAUL` / `REEMPLAZO`) es un **dato derivado**: se calcula, no se guarda.
  - `puntajeFallos ≥ umbralOverhaul` → OVERHAUL.
  - `numeroOverhauls ≥ maximoOverhauls` y además supera el umbral → REEMPLAZO.
- **El puntaje solo cambia en un lugar:** `RankingFallos.sumarFallo(maquina, peso)` hace **eliminar → sumar → insertar**, en el ranking de la compañía y en el global. Si la clave cambiara con la máquina dentro del `TreeSet`, el árbol quedaría desordenado y `remove` dejaría de encontrarla, sin lanzar ningún error. **Esta trampa debe aparecer en el documento explicativo.**
- **El ranking se actualiza cuando hay un evento, no cada cierto tiempo:** en el paso 4 del tick, cuando se clasifica un evento como P1–P3, ya sea en automático o en manual. También se reinserta al terminar un overhaul (el puntaje vuelve a 0), y con un reemplazo la máquina vieja sale y la nueva entra.
- Las etiquetas se repiten entre compañías. El comparador del ranking global ordena por puntaje descendente, luego por compañía y luego por etiqueta. El de cada compañía, por puntaje descendente y luego por etiqueta. Sin el desempate, el `TreeSet` trataría dos máquinas distintas como la misma.

### Overhaul y reemplazo
1. Al clasificar un evento se actualiza el ranking, y la recomendación aparece en la API. **La recomendación no asigna a nadie.**
2. El superusuario o la compañía **aprueba** el overhaul. La solicitud entra al montículo con el puntaje de ese momento **congelado** como clave. Mientras espera, la máquina sigue operando, y su clave no cambia, por la misma trampa del `TreeSet`. Una máquina con una solicitud pendiente no puede tener otra.
3. En cada tick, mientras haya técnicos del fabricante libres, se **mira la cima** del montículo. Si la máquina está libre, se extrae la solicitud, se le asigna un técnico y la máquina pasa al estado nuevo **`EN_OVERHAUL`**. "Libre" significa sin alarma activa, sin orden correctiva pendiente y fuera de mantenimiento.
   Si la máquina de la cima **no** está libre, la asignación espera a un tick siguiente. No se salta a la segunda solicitud, porque eso violaría el tipo abstracto de datos.
4. Una máquina `EN_OVERHAUL` no lee sensores, no falla y no genera preventivos. Al terminar:
   - el puntaje vuelve a 0 y `numeroOverhauls` sube en 1;
   - se reinician los ticks desde el mantenimiento, porque el overhaul **es** un servicio completo;
   - la máquina se reinserta en los rankings y vuelve a OPERANDO;
   - el técnico vuelve al final de su cola.
5. **Reemplazo:** se aprueba sobre una máquina con recomendación REEMPLAZO que no esté `EN_OVERHAUL`. La máquina nueva usa el mismo tipo y la misma etiqueta, con `numeroOverhauls` y puntaje en 0. Entra en el **mismo índice** del `ArrayList` del inventario (`set`, O(1)), así se respeta la regla de que las máquinas no se eliminan, y se actualizan la jerarquía y los dos rankings. Es inmediato: no usa técnicos.
- **Pendiente de definir en el commit 16:** qué pasa con una orden P4 pendiente de una máquina que entra en overhaul. Se propone antes de implementarlo.

### Orden de un tick de la organización (`Organizacion.tick()`)
1. Recorrer las compañías en **preorden** y ejecutar `Planta.tick()` en cada una (pasos 1–7 de la sección 7; el paso 4 actualiza los rankings).
2. **Avanzar overhauls:** a cada técnico del fabricante ocupado se le resta un tick, y se cierran los que terminan.
3. **Asignar overhauls** con la regla de la cima (solo en modo automático).
4. Registrar en los árboles de segmentos (el de la compañía y el global) el puntaje sumado en este tick.
5. Incrementar el tick de la organización.

### Modo manual
- Se puede **activar y desactivar con la simulación en marcha** (`AUTOMATICO` ↔ `MANUAL`). El tiempo sigue corriendo: las lecturas, la detección y el avance de los trabajos siguen siendo automáticos.
- En modo manual se apagan los pasos automáticos que el usuario quiere hacer a mano:
  - el **paso 1** (fallos aleatorios): los fallos se inyectan;
  - el **paso 4** (clasificar): un botón para *clasificar el siguiente evento*;
  - el **paso 6** (asignar): un botón para *asignar la siguiente orden*;
  - la **asignación de overhauls**: un botón para *asignar el siguiente overhaul*.
- **El usuario dispara operaciones; no elige elementos.** "Asignar a mano" no significa escoger cualquier orden o cualquier técnico. Eso sería sacar un elemento que no es el siguiente de la cola con prioridad, o saltarse la FIFO de técnicos, y viola el tipo abstracto de datos. Es el mismo argumento de la sección 8. El botón ejecuta la operación del tipo de dato (`extraer`, `desencolar`), y la estructura decide qué sale.

### Valores iniciales (PROVISIONALES — los define Cristian)
- **3 compañías**, cada una con las 6 máquinas de la sección 6 y 2 técnicos internos, con semillas derivadas de la maestra.
- **2 técnicos del fabricante**, compartidos. El overhaul dura **30 ticks**.
- `umbralOverhaul` = 12 puntos, `maximoOverhauls` = 2. Horizonte del historial: 2000 ticks.
- Todos estos valores van en `ConfiguracionPlanta` (o en una `ConfiguracionOrganizacion` si crece demasiado; se propone antes de crearla).

### Superficie de la API (borrador; el contrato final está en `docs/API.md`)
- **Simulación:** `GET /api/simulacion`; `POST /api/simulacion/iniciar | pausar | paso | reiniciar`; `PUT /api/simulacion/modo`.
- **Organización:** `GET /api/companias` (árbol con totales por rama); `GET /api/companias/{id}`, con el estado completo de una planta: inventario, cola circular, eventos, técnicos, cola con prioridad, pila y bitácora.
- **Operador (por compañía):** inyectar un fallo, parar o arrancar, cambiar la carga, deshacer, clasificar el siguiente evento y asignar la siguiente orden.
- **Superusuario:** `GET /api/ranking` (global o por compañía); `POST /api/overhauls` (aprobar), `GET /api/overhauls` (montículo y técnicos del fabricante), `POST /api/overhauls/asignar-siguiente`; `POST .../reemplazo`; `GET /api/historial?desde=&hasta=&compania=`.
- **Errores:** `400` si la entrada es inválida, `404` si la compañía o la máquina no existe, y `409` si la acción no se permite en el estado actual (por ejemplo, inyectar un fallo en una máquina con alarma). El cuerpo lleva el mensaje en español.

### Vistas que el frontend debe ofrecer (requisito para el otro repositorio)
- **Vista de compañía:** los paneles de la sección 9 para una compañía.
- **Vista de superusuario (dashboard):**
  - el árbol de compañías con los totales por rama;
  - el ranking global con sus recomendaciones;
  - el montículo de solicitudes y los técnicos del fabricante;
  - una gráfica del historial con una ventana [a, b] seleccionable, que muestre el total y el pico;
  - el interruptor de modo.
- Como en la Entrega 1, cada estructura se dibuja con su comportamiento real, sin inventar estado interno que la librería no expone.

---

## 17. Entrega 2: plan de commits

Todo se trabaja en la rama `entrega-2`. Cada commit es pequeño, deja las pruebas en verde y se revisa a mano antes del siguiente (sección 15). Cada uno actualiza `docs/USO_IA.md` con sus archivos.

**Por qué la migración va primero:** así el repositorio del frontend puede empezar contra una API real con paridad de la Entrega 1, mientras aquí se construyen las estructuras nuevas. El modelo de la Entrega 1 ya está probado, así que no se rompe el orden "modelo probado → vista". La única consecuencia es el commit 19, que mueve las rutas de `/api/planta` a `/api/companias/{id}`, y se anuncia en `docs/API.md`.

**Antes del commit 2:** crear la etiqueta `entrega-1` sobre `main`, con el permiso del equipo para crearla y publicarla.

### Etapa A — Decisión
1. `docs: registrar la migración a Spring Boot y el diseño de la Entrega 2`: este documento, la bitácora y `USO_IA.md`.

### Etapa B — Migración a Spring Boot (paridad con la Entrega 1, el modelo no cambia)
2. `build: convertir el proyecto a Spring Boot`: padre de Spring Boot, los dos starters, `spring-boot-maven-plugin`, `AplicacionPlanta` y una prueba de que el contexto carga.
3. `refactor: retirar la interfaz Swing`: se eliminan `App`, `vista/`, `controlador/`, `PanelesVistaTest` y `ControladorPlantaTest`. Las pruebas del modelo siguen pasando sin cambios.
4. `feat: servicio de simulación`: `ServicioSimulacion` con sus métodos `synchronized`, el `@Scheduled`, iniciar, pausar, paso y reiniciar, y el intervalo en `application.properties`. Pruebas sin HTTP.
5. `feat: DTOs del estado de la planta`: `record`s para la máquina, la lectura, la cola circular (con frente, final, celdas y sobrescrituras), los eventos, los técnicos, los cuatro carriles, la pila y la bitácora. Prueba de que son copias.
6. `feat: endpoints de control y consulta`: `GET /api/planta`, `GET /api/simulacion` y `POST` para iniciar, pausar, paso y reiniciar. Pruebas MockMvc.
7. `feat: endpoints de acciones del operador`: inyectar un fallo, parar o arrancar, cambiar la carga y deshacer, más el manejador de errores 400/404/409. Pruebas MockMvc.
8. `feat: CORS para el frontend local y contrato de la API`: configuración de CORS para `localhost`, `docs/API.md` y el README con cómo ejecutar el backend.

### Etapa C — Estructuras nuevas (`estructuras/`, genéricas y sin dominio)
9. `feat: árbol n-ario genérico`: agregar un hijo, eliminar un hijo, preorden y postorden. Pruebas: un árbol de un solo nodo, los dos recorridos y la eliminación.
10. `feat: árbol de segmentos de suma y máximo`: actualizar una posición y consultar un rango. Pruebas: el rango completo, un rango de un elemento, un máximo a mitad del rango y una actualización.

### Etapa D — Modelo de la Entrega 2 (sin HTTP, todo probado)
11. `feat: peso de fallo, puntaje y recomendación`: `Prioridad.peso`, los campos nuevos de `Maquina`, `Recomendacion` y los umbrales. Pruebas de la recomendación.
12. `feat: ranking de fallos por compañía`: `RankingFallos`, conectado al paso 4. Prueba: tras N ticks, el recorrido del ranking coincide con ordenar el inventario.
13. `feat: organización con varias compañías`: `Organizacion`, la jerarquía con `ArbolNario`, las semillas derivadas, el tick en preorden y el ranking global. Prueba: misma semilla, mismo resultado.
14. `feat: modo manual`: el modo en `Planta` y `Organizacion`, `clasificarSiguiente` y `asignarSiguiente`. Pruebas: en manual no se generan fallos aleatorios ni se asigna nada solo.
15. `feat: solicitudes de overhaul y técnicos del fabricante`: `SolicitudOverhaul`, `TecnicoExterno`, aprobar (con rechazo de duplicadas), el montículo con secuencia y la cola FIFO. Pruebas: el mayor puntaje sale primero y los empates salen por orden de llegada.
16. `feat: ejecutar overhauls en el tick`: `EN_OVERHAUL`, la regla de la cima y el cierre del overhaul. **Se propone primero la regla pendiente sobre la P4.** Pruebas: no consume técnicos internos y vuelve con puntaje 0 y reinsertada.
17. `feat: reemplazo de máquina`: `set` en el inventario, la jerarquía y los dos rankings. Prueba.
18. `feat: historial con árbol de segmentos`: uno por compañía y uno global, alimentados en el tick. Prueba de suma y pico en una ventana.

### Etapa E — API de la Entrega 2
19. `refactor: el servicio usa la organización y rutas por compañía`: `/api/companias` y `/api/companias/{id}/...`, con `docs/API.md` actualizado.
20. `feat: endpoints del modo manual`.
21. `feat: endpoints del ranking y las recomendaciones`.
22. `feat: endpoints de overhaul y reemplazo`.
23. `feat: endpoint del historial por ventana de ticks`.

### Etapa F — Documentación
24. `docs: documento explicativo de la Entrega 2`: el por qué y el cómo de cada estructura de la sección 16, con sus complejidades, la trampa de la clave que cambia y la justificación del árbol de segmentos frente a las sumas prefijas.
25. `docs: README, USO_IA y cierre de la bitácora de la Entrega 2`.
