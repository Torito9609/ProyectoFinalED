# Simulador de Monitoreo y Mantenimiento de Planta

Proyecto del curso **Estructuras de Datos** — Universidad Nacional de Colombia.
Equipo: **Cristian Robayo** y **Kevin Toro**.

Este documento es la fuente de verdad del diseño. Cualquier cambio de decisión se registra aquí antes de implementarlo.

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
- Interfaz: **Swing**.
- Pruebas: **JUnit 5**. Es la única dependencia externa permitida.
- Sin frameworks adicionales.

---

## 4. Arquitectura: MVC

Paquete raíz: `edu.unal.planta`

```
edu.unal.planta
├── App                      → punto de entrada: crea modelo, vista y controlador
├── estructuras/             → ColaCircular (propia). Genérica, sin dependencias del dominio
├── modelo/                  → dominio y simulación. NO importa nada de javax.swing ni java.awt
│   └── comandos/            → acciones del operador que se pueden deshacer
├── vista/                   → ventana principal y un panel por estructura
└── controlador/             → temporizador, botones y acciones del operador
```

### Reglas de la arquitectura (no se negocian)
1. **El modelo no conoce la vista.** Cuando cambia, notifica a observadores mediante una interfaz propia del modelo, por ejemplo `ObservadorPlanta`. Así se podría reemplazar Swing por consola sin tocar el modelo.
2. **La vista solo lee el modelo y dibuja.** No modifica el estado.
3. **Todas las acciones del usuario pasan por el controlador**, que llama métodos del modelo.
4. **El reloj de la simulación es `javax.swing.Timer`.** Se ejecuta en el hilo de eventos de Swing (EDT), así que no hay concurrencia ni condiciones de carrera. No usar `Thread` ni `java.util.Timer`.
5. **Aleatoriedad reproducible:** un único `Random` con semilla configurable. La misma semilla debe producir la misma simulación, lo que permite repetir la demo y el video.
6. La vista accede a las estructuras en **solo lectura**: recibe copias o vistas no modificables, nunca la estructura original.

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

Son **6 máquinas** y no 3. El cambio se decidió el 18 de octubre, tras medir la simulación: una máquina con anomalía activa no desarrolla otra, así que con 3 máquinas y 2 técnicos la cola con prioridad nunca pasaba de **una** orden y los paneles de colas se veían vacíos en toda la demostración. Con 6 el techo sube a 4. Ver `docs/BITACORA_DESARROLLO.md`.
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

## 9. Interfaz (Swing)

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

**Fuera de alcance:** conexión a PLC reales, persistencia en archivos o base de datos, estructuras no lineales.

---

## 11. Plan de trabajo

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

---

## 13. Convenciones de código

- Identificadores en español, sin tildes ni ñ (`Maquina`, `ColaCircular`, `tamano`). Comentarios y Javadoc en español, con tildes.
- Javadoc obligatorio en clases y métodos públicos. En las estructuras, indicar la **complejidad** de cada operación.
- Clases pequeñas con una sola responsabilidad.
- El modelo nunca importa `javax.swing` ni `java.awt`.
- Constantes y parámetros de simulación centralizados, sin números sueltos en la lógica.

---

## 14. Registro del uso de IA

El enunciado exige declarar el porcentaje de código generado con IA, así que se registra desde el primer commit:
- Los commits hechos con Claude Code conservan la línea `Co-Authored-By` que agrega la herramienta. Los commits sin esa línea son escritos por el equipo.
- `docs/USO_IA.md` lleva una tabla por archivo o componente: quién lo escribió (IA / equipo / mixto) y qué se revisó o modificó a mano.

---

## 15. Instrucciones para el asistente (Claude Code)

- Trabajar **por etapas, una a la vez**, siguiendo el orden de la sección 11. Al terminar cada etapa: resumir qué se hizo y **esperar confirmación** antes de seguir. Antes de empezar una etapa, contar qué se va a hacer.
- **Mantenerlo simple.** Las pruebas cubren los casos que exige este documento, no todos los casos imaginables: unas pocas pruebas claras por estructura, sin casos defensivos extra salvo que el equipo los pida. Preferir la solución directa antes que la extensible. Antes de agregar clases, capas, interfaces o métodos que este documento no pida, **proponerlo** en vez de implementarlo. El criterio es que el equipo pueda explicar todo el código en clase.
- **Explicar cada clase que se cree:** su responsabilidad, la estructura que usa y la complejidad de sus operaciones. El equipo debe poder defender cada línea ante el profesor.
- Respetar las decisiones de la sección 5. **No usar** `PriorityQueue`, `Stack`, `LinkedList` ni `Vector`. No proponer heaps ni otras estructuras no lineales en esta entrega.
- No agregar dependencias distintas de JUnit 5.
- Si una decisión de este documento resulta inviable o contradictoria, **decirlo y proponer el cambio antes de implementarlo**, y actualizar este documento.
- Ejecutar las pruebas antes de declarar terminada una etapa.
- **No hacer `commit` ni `push` sin pedir permiso.**
- Al cerrar cada etapa, actualizar `docs/USO_IA.md` con los archivos generados.
