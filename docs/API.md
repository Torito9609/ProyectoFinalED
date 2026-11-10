# Contrato de la API REST

Este documento es lo que necesita el repositorio del **frontend** para hablar con este backend. Si un
endpoint cambia, se cambia aquí primero.

- **URL base (local):** `http://localhost:8080`
- **Formato:** JSON en UTF-8. Los campos van en camelCase y en español.
- **CORS:** se permiten `GET`, `POST` y `PUT` desde cualquier puerto de `http://localhost` y
  `http://127.0.0.1` (propiedad `planta.cors.origenes`).
- **Enumerados:** viajan como texto, con su nombre: `"OPERANDO"`, `"P1"`, `"SEVERA"`.
- **Índices:** las compañías, las máquinas y los sensores se identifican por su **posición** (desde
  0), que es la misma en todas las respuestas. El id de una compañía es su índice en la organización.
  Nada se elimina, así que los ids no cambian.

> **Cambio del commit 19 (Entrega 2):** las rutas `/api/planta/...` pasaron a
> `/api/companias/{id}/...`, y `GET /api/planta` pasó a `GET /api/companias/{id}`. `/api/simulacion`
> se conserva: el reloj es uno solo para toda la organización. En las máquinas, `tipo` pasó a ser el
> **código** del catálogo (`"PRENSA"`), y la descripción viaja en el campo nuevo `tipoDescripcion`.

---

## 1. Cómo consultar el estado: sondeo

El backend no empuja cambios (no hay WebSockets). El frontend pide el estado periódicamente:

- `GET /api/companias/{id}` (la compañía que se está viendo) cada **500 a 1000 ms**. El reloj avanza
  un tick cada 800 ms por defecto (`planta.intervalo-ms`).
- Todas las acciones del operador **devuelven el estado completo**, así que después de una acción no
  hace falta pedirlo de nuevo.
- Cada respuesta es una **foto consistente**: se arma en el servidor sin que un tick quede a medias.

---

## 2. Simulación

| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| `GET` | `/api/simulacion` | — | `EstadoSimulacion` |
| `POST` | `/api/simulacion/iniciar` | — | `EstadoSimulacion` |
| `POST` | `/api/simulacion/pausar` | — | `EstadoSimulacion` |
| `POST` | `/api/simulacion/paso` | — | `EstadoSimulacion`. Primero pausa y luego avanza **un** tick |
| `POST` | `/api/simulacion/reiniciar` | — | `EstadoSimulacion`. Pausa y vuelve al tick 0 con la misma semilla. Las compañías y máquinas creadas se conservan |

Estas acciones actúan sobre **todas** las compañías a la vez: todas tienen siempre el mismo tick.

**`EstadoSimulacion`**

```json
{ "tick": 14, "semilla": 7, "enEjecucion": false }
```

`semilla` es la **semilla maestra**. La de la compañía `i` se deriva de ella: `maestra × 1000 + i`.

---

## 3. Compañías

| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/api/companias` | Lista de `ResumenCompania`, en el orden de sus ids |
| `GET` | `/api/companias/{id}` | `EstadoPlanta` de esa compañía |

Al arrancar hay 3 compañías: `0` "Planta Norte", `1` "Planta Centro" y `2` "Planta Sur", cada una
con las 6 máquinas y 2 técnicos.

**`ResumenCompania`** (los totales para el dashboard):

```json
{
  "id": 0,
  "nombre": "Planta Norte",
  "numeroMaquinas": 6,
  "maquinasPorEstado": { "OPERANDO": 4, "EN_ALARMA": 1, "DETENIDA": 0,
                         "EN_MANTENIMIENTO": 1, "PARADA_MANUAL": 0 },
  "eventosPendientes": 1,
  "ordenesPendientes": 2,
  "tecnicosLibres": 0
}
```

`maquinasPorEstado` trae siempre todos los estados, incluso los que tienen 0. `eventosPendientes`,
`ordenesPendientes` y `tecnicosLibres` son los tamaños de la cola de eventos, de la cola con
prioridad (los 4 carriles) y de las dos colas de técnicos libres, sumadas.

**`EstadoPlanta`** (ejemplo del tick 14, recortado con `"..."`):

```json
{
  "idCompania": 0,
  "nombre": "Planta Norte",
  "tick": 14,
  "semilla": 7000,
  "enEjecucion": false,
  "maquinas": [
    {
      "indice": 0,
      "etiqueta": "PRENSA-01",
      "tipo": "PRENSA",
      "tipoDescripcion": "Prensa hidráulica",
      "estado": "OPERANDO",
      "carga": 80,
      "ticksDesdeMantenimiento": 14,
      "intervaloPreventivo": 60,
      "tieneAnomalia": false,
      "alarmaActiva": false,
      "puntajeFallos": 0,
      "numeroOverhauls": 0,
      "recomendacion": "NINGUNA",
      "sensores": [
        { "nombre": "Temperatura aceite", "unidad": "°C", "nominal": 55.0,
          "umbralAdvertencia": 70.0, "umbralAlto": 80.0, "umbralCritico": 90.0,
          "tipoFallo": "MECANICO" },
        "..."
      ],
      "lecturas": {
        "capacidad": 12,
        "tamano": 12,
        "indiceFrente": 2,
        "indiceFinal": 2,
        "sobrescrituras": 2,
        "celdas": [
          { "tick": 12, "valores": [55.56, 179.21, 2.36] },
          { "tick": 13, "valores": [56.58, 176.44, 2.27] },
          { "tick": 2,  "valores": [55.90, 178.31, 2.36] },
          "..."
        ]
      }
    },
    "..."
  ],
  "eventos": [],
  "tecnicos": [
    { "nombre": "Ana", "especialidad": "MECANICO", "libre": true, "ordenActual": null,
      "ticksRestantes": 0 },
    { "nombre": "Luis", "especialidad": "ELECTRICO", "libre": false, "ticksRestantes": 3,
      "ordenActual": { "id": 2, "indiceMaquina": 5, "etiquetaMaquina": "BOMBA-01",
        "prioridad": "P3", "descripcion": "Vibración en 6,1 mm/s (Ir a revisar)",
        "tickCreacion": 10, "duracionEstimadaTicks": 6, "tipoFallo": "MECANICO" } }
  ],
  "tecnicosLibres": { "MECANICO": ["Ana"], "ELECTRICO": [] },
  "colaPrioridad": {
    "carriles": [
      { "prioridad": "P1", "descripcion": "CRÍTICA", "ordenes": [] },
      { "prioridad": "P2", "descripcion": "ALTA", "ordenes": [] },
      { "prioridad": "P3", "descripcion": "MEDIA", "ordenes": [] },
      { "prioridad": "P4", "descripcion": "BAJA", "ordenes": [] }
    ],
    "siguiente": null,
    "tamano": 0
  },
  "pilaDeshacer": [],
  "bitacora": [
    "Tick 10: Clasificado: OT-2 BOMBA-01 → P3 MEDIA",
    "Tick 10: Luis atiende OT-2 BOMBA-01 (P3, 6 ticks)"
  ]
}
```

### Qué significa cada parte (y qué estructura dibuja)

| Campo | Estructura | Cómo leerlo |
|---|---|---|
| `semilla` | — | La semilla de **esta** compañía, derivada de la maestra |
| `maquinas` | Arreglo dinámico (`ArrayList`) | En orden de índice. `estado`: `OPERANDO`, `EN_ALARMA`, `DETENIDA`, `EN_MANTENIMIENTO` o `PARADA_MANUAL`. `tipo` es el código del catálogo (`PRENSA`, `TORNO`, `ROBOT`, `COMPRESOR`, `HORNO` o `BOMBA`), y `tipoDescripcion` el texto para mostrar |
| `maquinas[i].puntajeFallos` | Clave del ranking (árbol rojo-negro) | Suma de los pesos de sus fallos desde el último overhaul: P1 = 3, P2 = 2, P3 = 1 (un preventivo P4 no suma). `recomendacion` es `NINGUNA`, `OVERHAUL` o `REEMPLAZO` (sección 4b) |
| `maquinas[i].lecturas` | Cola circular propia | `celdas` va en **orden físico** (índice 0 a 11), no en orden de llegada. Una celda vacía es `null`. `indiceFrente` es la lectura más antigua; `indiceFinal`, donde se escribirá la próxima. Cuando la cola está llena los dos coinciden, y la próxima lectura sobrescribe el frente. `valores[k]` corresponde a `sensores[k]` |
| `maquinas[i].sensores` | — | Umbrales para colorear cada valor: < advertencia normal; ≥ advertencia P3; ≥ alto P2; ≥ crítico P1. `tipoFallo` (`MECANICO` o `ELECTRICO`) es el tipo de los fallos de ese sensor |
| `eventos` | Cola FIFO (`ArrayDeque`) | El primero de la lista es el siguiente en clasificarse. Cada evento trae su `tipoFallo` |
| `tecnicosLibres` | Dos colas FIFO (`ArrayDeque`), una por especialidad | Objeto con las claves `MECANICO` y `ELECTRICO`. En cada cola, el primero de la lista recibe la siguiente orden de esa especialidad |
| `tecnicos` | — | Todos los técnicos, con su `especialidad`; los ocupados traen `ordenActual` y `ticksRestantes` |
| `ordenActual`, `colaPrioridad...ordenes[j]` | — | Cada orden trae su `tipoFallo`: la especialidad que pide. Si la atiende un técnico de la otra especialidad, tarda el doble, y `ticksRestantes` ya lo incluye |
| `colaPrioridad` | Cola con prioridad (arreglo de 4 colas) | Siempre 4 carriles, de P1 a P4. Cada carril es FIFO: el primero de `ordenes` sale primero. `siguiente` es el carril del que sale la próxima orden (`null` si no hay órdenes): es el que se resalta |
| `pilaDeshacer` | Pila (`ArrayDeque`) | El **primero** de la lista es la **cima**: lo que se deshace primero |
| `bitacora` | — | De la línea más antigua a la más reciente (máximo 200) |

---

## 4. Acciones del operador

Todas actúan sobre una compañía y devuelven su `EstadoPlanta` con código `200`. Cada compañía tiene
su propia pila de deshacer.

| Método | Ruta | Cuerpo | Se puede deshacer |
|---|---|---|---|
| `POST` | `/api/companias/{id}/maquinas/{indice}/inyectar-fallo` | `{ "indiceSensor": 0, "magnitud": "SEVERA" }` | No |
| `POST` | `/api/companias/{id}/maquinas/{indice}/parar-o-arrancar` | — | Sí |
| `PUT` | `/api/companias/{id}/maquinas/{indice}/carga` | `{ "carga": 60 }` | Sí |
| `POST` | `/api/companias/{id}/deshacer` | — | — |

- **Inyectar fallo:** `magnitud` es `LEVE`, `MODERADA` o `SEVERA`. Se rechaza si la máquina ya tiene
  un fallo activo (`tieneAnomalia` o `alarmaActiva`): conviene deshabilitar el botón en ese caso.
- **Parar o arrancar:** solo aplica si la máquina está `OPERANDO` (la para) o `PARADA_MANUAL` (la
  arranca). Conviene deshabilitar el botón en los demás estados.
- **Carga:** de 0 a 100. Si es igual a la actual, no se apila nada.
- **Deshacer:** revierte la cima de la pila. Si la simulación ya cambió ese estado (por ejemplo, una
  alarma crítica detuvo la máquina), la acción se **descarta** sin revertirse, y la bitácora lo
  explica. Esa respuesta también es `200`.

---

## 4a. Crear compañías y máquinas

| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| `POST` | `/api/companias` | `{ "nombre": "Planta Oriente" }` | **`201`** con el `ResumenCompania` de la nueva (trae su `id`) |
| `POST` | `/api/companias/{id}/maquinas` | `{ "tipo": "TORNO", "etiqueta": "torno-02" }` | **`201`** con el `EstadoPlanta` de la compañía |
| `GET` | `/api/tipos-maquina` | — | El catálogo de tipos, para el formulario |

- **Compañía:** el nombre debe ser único en la organización. Se compara sin espacios a los lados y sin
  distinguir mayúsculas, pero se muestra como se escribió (sin los espacios). Entra al final de la
  lista con el id siguiente, con las 6 máquinas por defecto y 2 técnicos, y en el **tick actual**:
  todas las compañías tienen siempre el mismo tick. Su semilla es `maestra × 1000 + id`.
- **Máquina:** `tipo` es un código del catálogo (no distingue mayúsculas). La etiqueta debe ser única
  **dentro de su compañía** (en compañías distintas se puede repetir) y se guarda sin espacios a los
  lados y en mayúsculas. La máquina entra al final del inventario: su `indice` es el siguiente.
- **No se deshace ni se elimina.** Reiniciar la simulación conserva lo creado y lo devuelve a su
  estado inicial.

**Catálogo** (`GET /api/tipos-maquina`), en este orden:

```json
[
  { "tipo": "PRENSA",    "descripcion": "Prensa hidráulica",            "prefijo": "PRENSA" },
  { "tipo": "TORNO",     "descripcion": "Torno CNC",                    "prefijo": "TORNO" },
  { "tipo": "ROBOT",     "descripcion": "Brazo robótico de soldadura",  "prefijo": "ROBOT" },
  { "tipo": "COMPRESOR", "descripcion": "Compresor de tornillo",        "prefijo": "COMPRES" },
  { "tipo": "HORNO",     "descripcion": "Horno de tratamiento térmico", "prefijo": "HORNO" },
  { "tipo": "BOMBA",     "descripcion": "Bomba centrífuga",             "prefijo": "BOMBA" }
]
```

El `prefijo` sirve para sugerir la etiqueta siguiente (`TORNO-02`).

| Caso | Código | Ejemplo de mensaje |
|---|---|---|
| Nombre o etiqueta vacíos o que faltan | `400` | `El nombre de la compañía no puede estar vacío` |
| Tipo que no existe | `400` | `El tipo 'GRUA' no existe: use PRENSA, TORNO, ROBOT, COMPRESOR, HORNO, BOMBA` |
| Nombre de compañía repetido | `409` | `Ya existe una compañía llamada Planta Norte` |
| Etiqueta repetida en la compañía | `409` | `Ya existe una máquina con etiqueta TORNO-01 en Planta Norte` |
| La compañía no existe | `404` | `No existe una compañía con id 99` |

---

## 4b. Ranking de fallos y recomendaciones

| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/api/ranking` | `Ranking` global: las máquinas de todas las compañías |
| `GET` | `/api/ranking?compania={id}` | `Ranking` de esa compañía (`404` si no existe) |
| `GET` | `/api/ranking?puntajeMinimo={k}` | Solo las máquinas con puntaje ≥ k. Se combina con `compania` |

Cada ranking es un **árbol rojo-negro** (`TreeSet`) que el backend mantiene ordenado en cada fallo:
las respuestas no se ordenan al pedirlas. Con `puntajeMinimo` se hace una **consulta por rango** en
el árbol, O(log n + k): con `puntajeMinimo` igual a `umbralOverhaul` salen las candidatas a overhaul.

**`Ranking`** (ejemplo recortado):

```json
{
  "alcance": "Global",
  "umbralOverhaul": 12,
  "maximoOverhauls": 3,
  "maquinas": [
    { "posicion": 1, "idCompania": 2, "nombreCompania": "Planta Sur", "indice": 3,
      "etiqueta": "COMPRES-01", "tipo": "COMPRESOR", "estado": "OPERANDO",
      "puntajeFallos": 14, "numeroOverhauls": 0, "recomendacion": "OVERHAUL" },
    "..."
  ]
}
```

- `alcance` es `"Global"` o el nombre de la compañía. `posicion` empieza en 1.
- Orden: puntaje de mayor a menor; en un empate, por `idCompania` y luego por `indice`.
- Una máquina se identifica por `(idCompania, indice)`, igual que en las rutas de las acciones.
- **Recomendación:** `OVERHAUL` si `puntajeFallos ≥ umbralOverhaul`; `REEMPLAZO` si además ya tiene
  `numeroOverhauls ≥ maximoOverhauls`; si no, `NINGUNA`. Es una sugerencia: no asigna a nadie.

---

## 5. Errores

Todos los errores tienen el mismo cuerpo, con el mensaje en español listo para mostrar:

```json
{ "estado": 409, "mensaje": "No hay acciones para deshacer" }
```

| Código | Cuándo | Ejemplo de mensaje |
|---|---|---|
| `400` | Entrada inválida: carga fuera de rango, magnitud inexistente, JSON mal formado, índice que no es un número | `La carga debe estar entre 0 y 100: 150` |
| `404` | La compañía, la máquina o el sensor no existen | `No existe una compañía con id 99` |
| `409` | La acción no se permite en el estado actual (también si la compañía está en quiebra), o el nombre o la etiqueta ya existen | `No se puede inyectar un fallo en PRENSA-01: ya tiene un fallo activo` |

En los rechazos `409` de inyectar y deshacer, la bitácora también registra el rechazo.

---

## 6. Ejecutar el backend

```bash
./mvnw spring-boot:run
# o bien
./mvnw package && java -jar target/simulador-planta.jar --planta.semilla=42 --server.port=8080
```

Probar desde la terminal:

```bash
curl -X POST localhost:8080/api/simulacion/paso
curl localhost:8080/api/companias
curl localhost:8080/api/companias/0
curl -X PUT -H 'Content-Type: application/json' -d '{"carga": 60}' localhost:8080/api/companias/0/maquinas/0/carga
```
