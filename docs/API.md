# Contrato de la API REST

Este documento es lo que necesita el repositorio del **frontend** para hablar con este backend. Si un
endpoint cambia, se cambia aquí primero.

- **URL base (local):** `http://localhost:8080`
- **Formato:** JSON en UTF-8. Los campos van en camelCase y en español.
- **CORS:** se permiten `GET`, `POST` y `PUT` desde cualquier puerto de `http://localhost` y
  `http://127.0.0.1` (propiedad `planta.cors.origenes`).
- **Enumerados:** viajan como texto, con su nombre: `"OPERANDO"`, `"P1"`, `"SEVERA"`.
- **Índices:** las máquinas y los sensores se identifican por su **posición** (desde 0), que es la
  misma en todas las respuestas.

> **Aviso de cambio (commit 19 de la Entrega 2):** cuando se agreguen las compañías, las rutas
> `/api/planta/...` pasarán a `/api/companias/{id}/...`. `/api/simulacion` se conserva.

---

## 1. Cómo consultar el estado: sondeo

El backend no empuja cambios (no hay WebSockets). El frontend pide el estado periódicamente:

- `GET /api/planta` cada **500 a 1000 ms** mientras la simulación está en ejecución. El reloj avanza
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
| `POST` | `/api/simulacion/reiniciar` | — | `EstadoSimulacion`. Pausa y vuelve al tick 0 con la misma semilla |

**`EstadoSimulacion`**

```json
{ "tick": 14, "semilla": 7, "enEjecucion": false }
```

---

## 3. Estado de la planta

| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/api/planta` | `EstadoPlanta` |

**`EstadoPlanta`** (ejemplo real del tick 14 con semilla 7, recortado con `"..."`):

```json
{
  "tick": 14,
  "semilla": 7,
  "enEjecucion": false,
  "maquinas": [
    {
      "indice": 0,
      "etiqueta": "PRENSA-01",
      "tipo": "Prensa hidráulica",
      "estado": "OPERANDO",
      "carga": 80,
      "ticksDesdeMantenimiento": 14,
      "intervaloPreventivo": 60,
      "tieneAnomalia": false,
      "alarmaActiva": false,
      "sensores": [
        { "nombre": "Temperatura aceite", "unidad": "°C", "nominal": 55.0,
          "umbralAdvertencia": 70.0, "umbralAlto": 80.0, "umbralCritico": 90.0 },
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
    { "nombre": "Ana", "libre": true, "ordenActual": null, "ticksRestantes": 0 },
    { "nombre": "Luis", "libre": false, "ticksRestantes": 3,
      "ordenActual": { "id": 2, "indiceMaquina": 5, "etiquetaMaquina": "BOMBA-01",
        "prioridad": "P3", "descripcion": "Vibración en 6,1 mm/s (Ir a revisar)",
        "tickCreacion": 10, "duracionEstimadaTicks": 6 } }
  ],
  "tecnicosLibres": ["Ana"],
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
| `maquinas` | Arreglo dinámico (`ArrayList`) | En orden de índice. `estado`: `OPERANDO`, `EN_ALARMA`, `DETENIDA`, `EN_MANTENIMIENTO` o `PARADA_MANUAL` |
| `maquinas[i].lecturas` | Cola circular propia | `celdas` va en **orden físico** (índice 0 a 11), no en orden de llegada. Una celda vacía es `null`. `indiceFrente` es la lectura más antigua; `indiceFinal`, donde se escribirá la próxima. Cuando la cola está llena los dos coinciden, y la próxima lectura sobrescribe el frente. `valores[k]` corresponde a `sensores[k]` |
| `maquinas[i].sensores` | — | Umbrales para colorear cada valor: < advertencia normal; ≥ advertencia P3; ≥ alto P2; ≥ crítico P1 |
| `eventos` | Cola FIFO (`ArrayDeque`) | El primero de la lista es el siguiente en clasificarse |
| `tecnicosLibres` | Cola FIFO (`ArrayDeque`) | El primero de la lista recibe la siguiente orden |
| `tecnicos` | — | Todos los técnicos; los ocupados traen `ordenActual` y `ticksRestantes` |
| `colaPrioridad` | Cola con prioridad (arreglo de 4 colas) | Siempre 4 carriles, de P1 a P4. Cada carril es FIFO: el primero de `ordenes` sale primero. `siguiente` es el carril del que sale la próxima orden (`null` si no hay órdenes): es el que se resalta |
| `pilaDeshacer` | Pila (`ArrayDeque`) | El **primero** de la lista es la **cima**: lo que se deshace primero |
| `bitacora` | — | De la línea más antigua a la más reciente (máximo 200) |

---

## 4. Acciones del operador

Todas devuelven `EstadoPlanta` con código `200`.

| Método | Ruta | Cuerpo | Se puede deshacer |
|---|---|---|---|
| `POST` | `/api/planta/maquinas/{indice}/inyectar-fallo` | `{ "indiceSensor": 0, "magnitud": "SEVERA" }` | No |
| `POST` | `/api/planta/maquinas/{indice}/parar-o-arrancar` | — | Sí |
| `PUT` | `/api/planta/maquinas/{indice}/carga` | `{ "carga": 60 }` | Sí |
| `POST` | `/api/planta/deshacer` | — | — |

- **Inyectar fallo:** `magnitud` es `LEVE`, `MODERADA` o `SEVERA`. Se rechaza si la máquina ya tiene
  un fallo activo (`tieneAnomalia` o `alarmaActiva`): conviene deshabilitar el botón en ese caso.
- **Parar o arrancar:** solo aplica si la máquina está `OPERANDO` (la para) o `PARADA_MANUAL` (la
  arranca). Conviene deshabilitar el botón en los demás estados.
- **Carga:** de 0 a 100. Si es igual a la actual, no se apila nada.
- **Deshacer:** revierte la cima de la pila. Si la simulación ya cambió ese estado (por ejemplo, una
  alarma crítica detuvo la máquina), la acción se **descarta** sin revertirse, y la bitácora lo
  explica. Esa respuesta también es `200`.

---

## 5. Errores

Todos los errores tienen el mismo cuerpo, con el mensaje en español listo para mostrar:

```json
{ "estado": 409, "mensaje": "No hay acciones para deshacer" }
```

| Código | Cuándo | Ejemplo de mensaje |
|---|---|---|
| `400` | Entrada inválida: carga fuera de rango, magnitud inexistente, JSON mal formado, índice que no es un número | `La carga debe estar entre 0 y 100: 150` |
| `404` | La máquina o el sensor no existen | `No existe una máquina con índice 99` |
| `409` | La acción no se permite en el estado actual | `No se puede inyectar un fallo en PRENSA-01: ya tiene un fallo activo` |

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
curl localhost:8080/api/planta
curl -X PUT -H 'Content-Type: application/json' -d '{"carga": 60}' localhost:8080/api/planta/maquinas/0/carga
```
