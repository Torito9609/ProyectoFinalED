# Por qué y cómo se usó cada estructura de datos

**Simulador de Monitoreo y Mantenimiento de Planta** — Entregas 1 y 2.
Curso Estructuras de Datos — Universidad Nacional de Colombia.
Equipo: Cristian Robayo y Kevin Toro.

Este documento justifica cada estructura de datos del proyecto: **qué problema resuelve, por qué es
la adecuada, qué alternativas se descartaron y cuánto cuesta cada operación**. Todo lo que se afirma
aquí corresponde al código real. Cada sección indica la clase y el campo donde está la estructura, y
la sección 8 lista las pruebas que lo respaldan.

---

## Contenido

1. [Introducción](#1-introducción)
2. [Arreglo dinámico — inventario de máquinas](#2-arreglo-dinámico--inventario-de-máquinas)
3. [Cola circular — últimas lecturas de cada máquina](#3-cola-circular--últimas-lecturas-de-cada-máquina)
4. [Cola FIFO — eventos por clasificar y técnicos disponibles](#4-cola-fifo--eventos-por-clasificar-y-técnicos-disponibles)
5. [Cola con prioridad — órdenes de trabajo P1 a P4](#5-cola-con-prioridad--órdenes-de-trabajo-p1-a-p4)
6. [Pila — deshacer acciones del operador](#6-pila--deshacer-acciones-del-operador)
7. [Cómo interactúan las estructuras en un tick](#7-cómo-interactúan-las-estructuras-en-un-tick)
8. [Pruebas](#8-pruebas)
9. [Tabla resumen de complejidades](#9-tabla-resumen-de-complejidades)

**Segunda parte — Entrega 2**

10. [Introducción a la Entrega 2](#10-introducción-a-la-entrega-2)
11. [Arreglo dinámico — las compañías](#11-arreglo-dinámico--las-compañías)
12. [Conjunto — nombres que no se repiten](#12-conjunto--nombres-que-no-se-repiten)
13. [Árbol de búsqueda balanceado — el ranking de fallos](#13-árbol-de-búsqueda-balanceado--el-ranking-de-fallos)
14. [Montículo — las solicitudes de overhaul](#14-montículo--las-solicitudes-de-overhaul)
15. [Colas FIFO — técnicos por especialidad y técnicos del fabricante](#15-colas-fifo--técnicos-por-especialidad-y-técnicos-del-fabricante)
16. [Árbol de segmentos — el historial por ventana de ticks](#16-árbol-de-segmentos--el-historial-por-ventana-de-ticks)
17. [Cómo interactúan las estructuras en un tick de la organización](#17-cómo-interactúan-las-estructuras-en-un-tick-de-la-organización)
18. [Pruebas de la Entrega 2](#18-pruebas-de-la-entrega-2)
19. [Tabla resumen de complejidades de la Entrega 2](#19-tabla-resumen-de-complejidades-de-la-entrega-2)

---

## 1. Introducción

### El problema

En una planta industrial, cada máquina tiene sensores que miden temperatura, presión, vibración o
corriente. Cuando una lectura se sale de lo normal se dispara una alarma. Esa alarma se clasifica
según su gravedad y termina en una orden de trabajo para un técnico de mantenimiento.

El simulador reproduce ese ciclo con **6 máquinas** y **2 técnicos**:

```
 fallo → lectura anómala → alarma (evento) → clasificación → orden de trabajo → técnico → reparación
```

### Por qué este problema sirve para estructuras lineales

Cada etapa del ciclo es, por naturaleza, una estructura lineal distinta. No buscamos un uso para
cada estructura: el dominio las pide.

- Las alarmas se atienden **en el orden en que llegan** → cola FIFO.
- Las órdenes se atienden **por urgencia**, y entre iguales por orden de llegada → cola con
  prioridad.
- Un registrador de sensores tiene **memoria fija** y conserva solo lo más reciente → cola circular.
- Al operador le interesa revertir **lo último que hizo** → pila.
- Las máquinas son un conjunto que se **recorre completo** y se consulta **por posición** → arreglo
  dinámico.

### Resumen

El enunciado pide al menos tres estructuras lineales. **El proyecto usa las cinco.**

| Estructura | Implementación | Uso | Dónde está |
|---|---|---|---|
| Arreglo dinámico | `ArrayList<Maquina>` | Inventario de máquinas | `Planta.inventario` |
| Cola circular | **`ColaCircular<T>`, propia** | Últimas 12 lecturas de cada máquina | `Maquina.lecturas` |
| Cola FIFO | `ArrayDeque`, declarada como `Queue` | Eventos por clasificar y técnicos libres | `Planta.eventos`, `Planta.tecnicosLibres` |
| Cola con prioridad | Arreglo de 4 `ArrayDeque` | Órdenes de trabajo P1–P4 | `ColaPrioridadOrdenes`, en `Planta.ordenes` |
| Pila | `ArrayDeque`, con `push`/`pop` | Deshacer acciones del operador | `Planta.pilaDeshacer` |

### Un criterio que aplicamos en todas

**Una estructura no es solo dónde se guardan datos: también define qué operaciones están
permitidas.** A eso se le llama *tipo abstracto de datos* (TAD). Una cola solo deja entrar por el
final y salir por el frente. Una pila solo deja tocar la cima.

En varias decisiones preferimos **respetar el TAD antes que agregar una función**. Por eso la vista
recibe las estructuras en modo de solo lectura (sección 7). Y por eso algunas acciones no se pueden
deshacer (sección 6).

---

## 2. Arreglo dinámico — inventario de máquinas

**Implementación:** `ArrayList<Maquina>`, en el campo `Planta.inventario`.

### Cómo se usa

El inventario contiene las 6 máquinas. Sobre él hacemos tres operaciones:

1. **Recorrerlo completo en cada tick.** Cuatro de los ocho pasos del tick pasan por todas las
   máquinas: generar fallos, leer sensores, detectar anomalías y revisar preventivos.
2. **Acceder por índice.** Cada `EventoFallo` y cada `OrdenTrabajo` guardan el **índice** de su
   máquina, y con ese índice la recuperamos: `inventario.get(orden.indiceMaquina())`.
3. **Agregar al final**, al construir la planta.

Las máquinas **nunca se eliminan** del inventario. Cuando una falla o se repara, cambia su estado,
pero sigue en la misma posición.

### Por qué un arreglo dinámico

| Operación | Costo en `ArrayList` | Frecuencia en el proyecto |
|---|---|---|
| `get(i)`, acceso por índice | **O(1)** | Cada vez que se clasifica, asigna o termina una orden |
| Recorrer todo | O(n) | Cuatro veces por tick |
| `add` al final | **O(1) amortizado** | Solo al crear la planta |
| Eliminar del medio | O(n) | **Nunca** |

La estructura es rápida justo en lo que más usamos (acceso por índice y recorrido) y lenta solo en
lo que no usamos (eliminar del medio). Además, el arreglo guarda los elementos contiguos en memoria,
y por eso recorrerlo es eficiente.

### Por qué el índice y no una referencia a la máquina

La orden de trabajo guarda `indiceMaquina` en vez de un objeto `Maquina`. El acceso por índice es
O(1), así que no se pierde nada. A cambio, se gana:

- La orden no apunta a la máquina y la máquina no apunta a la orden: no hay referencia circular.
- La orden puede ser **inmutable** (es un `record`), así que se puede entregar a la vista sin
  riesgo.

Esta decisión depende de que las máquinas **nunca se eliminen**. Si se eliminara una, los índices
de las que vienen después cambiarían.

### Por qué agregar es O(1) *amortizado* y no O(1)

Internamente, `ArrayList` es un arreglo normal con más espacio del que usa. Al agregar un elemento
hay dos casos:

- **Hay espacio libre:** se escribe en la siguiente posición. Cuesta O(1).
- **El arreglo está lleno:** se crea uno **1,5 veces más grande** y se copian todos los elementos.
  Ese agregado cuesta O(n).

Como el arreglo crece en proporción a su tamaño, las copias son cada vez menos frecuentes. Si se
reparte el costo total entre todos los agregados, cada uno cuesta en promedio O(1). Eso significa
*amortizado*: algunas operaciones son caras, pero el promedio a largo plazo es constante.

### Por qué no un arreglo fijo (`Maquina[]`)

Con 6 máquinas fijas, un arreglo normal también funcionaría. Elegimos el dinámico por dos razones:

1. El alcance opcional del proyecto (sección 10 de `CLAUDE.md`) incluye **agregar máquinas desde la
   interfaz**. Con `ArrayList` eso no exige reescribir nada.
2. `ArrayList` se integra con el resto de la librería, por ejemplo para entregar la lista a la vista
   como **no modificable**, con `Collections.unmodifiableList`.

### Cómo se ve en la interfaz

El panel **INVENTARIO — ArrayList** dibuja una fila por posición, con el **índice a la izquierda**,
porque el acceso por índice es lo que caracteriza a la estructura. No se dibuja la capacidad
interna del `ArrayList`: Java no la expone, e inventarla sería mostrar una memoria que no conocemos.

![Panel del inventario](img/manual/panel-inventario.png)

---

## 3. Cola circular — últimas lecturas de cada máquina

**Implementación: propia**, la clase genérica `estructuras/ColaCircular<T>`. Cada máquina tiene la
suya en el campo `Maquina.lecturas`, con capacidad para **12 lecturas**.

### El problema que resuelve

Cada máquina genera una lectura por tick, sin fin. No podemos guardarlas todas, porque la memoria
crecería sin límite. Y tampoco hace falta: para vigilar una máquina interesa lo reciente.

Así funcionan los **registradores de datos industriales**: tienen memoria fija y, cuando se llenan,
la lectura nueva **reemplaza a la más antigua**. Ese comportamiento es exactamente una cola circular
que sobrescribe.

### Por qué la implementamos nosotros

Es la **única estructura propia** del proyecto, y no fue por gusto: **Java no ofrece ninguna cola que
sobrescriba al llenarse.**

| Opción de `java.util` | Qué hace cuando se llena | Por qué no sirve |
|---|---|---|
| `ArrayDeque` | **Crece**: copia todo a un arreglo más grande | Es circular por dentro, pero nunca sobrescribe. La memoria crecería sin límite, que es justo lo que queremos evitar |
| `ArrayBlockingQueue` | **Rechaza** el elemento nuevo, o bloquea el hilo hasta que haya espacio | Hace lo contrario de lo que necesitamos: conserva lo viejo y pierde lo nuevo. Además está diseñada para concurrencia entre hilos, y nuestro programa es de un solo hilo |
| `LinkedList` | Crece | Igual que `ArrayDeque`, y además crea un objeto nodo por elemento |

Hay un segundo motivo. **Ninguna de ellas expone sus índices internos**, y para mostrar en pantalla
cómo funciona una cola circular necesitamos ver las celdas, el frente y el final. Con una clase
propia, decidimos qué consultas ofrecer.

### Cómo funciona

La cola tiene un arreglo de tamaño fijo que **nunca se redimensiona**. En lugar de mover los
elementos, se mueven índices:

```java
private final Object[] celdas;   // arreglo fijo: se crea una vez y nunca crece
private int frente;              // posición del elemento más antiguo
private int tamano;              // cuántos elementos hay
private long sobrescrituras;     // cuántos se han perdido por sobrescritura
```

El **final**, la posición donde se escribirá el próximo elemento, no se guarda. Se calcula:

```java
public int indiceFinal() {
    return (frente + tamano) % celdas.length;
}
```

**El operador módulo es lo que convierte el arreglo en un círculo.** Con capacidad 12, la posición
siguiente a la 11 es `12 % 12 = 0`: el índice vuelve al principio.

Encolar tiene dos casos:

```java
public void encolar(T elemento) {
    Objects.requireNonNull(elemento, "La cola circular no acepta elementos nulos");
    if (estaLlena()) {
        // Con la cola llena, la posición del final coincide con la del frente.
        celdas[frente] = elemento;
        frente = siguiente(frente);
        sobrescrituras++;
    } else {
        celdas[indiceFinal()] = elemento;
        tamano++;
    }
}
```

El caso de la cola llena se resuelve sin ninguna operación especial. Si la cola está llena,
`tamano == capacidad`, entonces `(frente + capacidad) % capacidad == frente`: **el final cae sobre el
frente**. Escribir ahí es sobrescribir el elemento más antiguo. Luego el frente avanza una posición,
porque el más antiguo pasa a ser el siguiente. El tamaño no cambia, porque entró uno y salió otro.

### Ejemplo con capacidad 4

| Acción | Celdas `[0, 1, 2, 3]` | frente | tamaño | final | sobrescrituras |
|---|---|---|---|---|---|
| Cola nueva | `[ -, -, -, - ]` | 0 | 0 | 0 | 0 |
| encolar A, B, C, D | `[ A, B, C, D ]` | 0 | 4 | 0 | 0 |
| encolar E | `[ E, B, C, D ]` | 1 | 4 | 1 | 1 |
| encolar F | `[ E, F, C, D ]` | 2 | 4 | 2 | 2 |
| desencolar → C | `[ E, F, -, D ]` | 3 | 3 | 2 | 2 |

En la última fila sale **C**, que es la más antigua que quedaba, aunque no esté en la celda 0. El
orden lógico es D, E, F, distinto del orden físico de las celdas. Esa diferencia entre el orden
lógico y el físico es la idea central de la estructura.

### Complejidad

| Operación | Costo | Por qué |
|---|---|---|
| `encolar`, con o sin sobrescritura | **O(1)** | Una escritura y aritmética de índices |
| `desencolar` | **O(1)** | Avanza el frente; no se desplaza ningún elemento |
| `verFrente`, `verMasReciente` | **O(1)** | Posición calculada: `frente` y `(frente + tamaño - 1) % capacidad` |
| `contenidoCelda(i)`, `indiceFrente`, `indiceFinal`, `tamano` | **O(1)** | Lectura directa |
| `elementosEnOrden` | O(n) | Copia los n elementos para la vista |
| Construir | O(capacidad) | Única reserva de memoria en toda la vida de la cola |

Hay una diferencia importante con las estructuras de `java.util`. **Aquí encolar es O(1) en todos
los casos, no O(1) amortizado**, porque el arreglo nunca crece y nunca hay que copiarlo. Es la
ventaja de tener memoria fija.

### Decisiones de diseño de la clase

- **No acepta `null`.** Una celda en `null` significa "celda vacía", y así la dibuja el panel. Si
  aceptara nulos, no se podría distinguir un elemento nulo de un hueco.
- **Es genérica (`<T>`)** y está en el paquete `estructuras`, que no conoce nada del dominio. No sabe
  qué es una lectura ni una máquina, así que podría reutilizarse en otro proyecto.
- **El arreglo es `Object[]` y no `T[]`.** Java no permite crear arreglos de un tipo genérico. La
  única conversión sin verificar está aislada en un método privado, `elementoEn`, y es segura porque
  solo `encolar(T)` escribe en el arreglo.
- **`verMasReciente()`** existe porque la detección de anomalías compara contra los umbrales la
  **última** lectura, no la más antigua.
- **Interfaz `ColaCircularSoloLectura<T>`.** `Maquina.lecturas()` devuelve la cola con ese tipo, que
  solo tiene métodos de consulta. La vista recibe la cola real, porque necesita ver sus celdas y
  punteros, pero **no tiene ningún método para modificarla**. Esa regla la verifica el compilador.

### Cómo se ve en la interfaz

El panel **COLA CIRCULAR** es el único que dibuja el **arreglo físico**, con sus 12 celdas
numeradas, los huecos y los punteros `frente` y `final`. Puede hacerlo porque la estructura es
nuestra. Cuando la cola se llena, frente y final coinciden y se ve una sola marca, `frente/final`.
El contador de sobrescrituras muestra cuántas lecturas viejas se perdieron.

![Panel de la cola circular](img/manual/panel-cola-circular.png)

---

## 4. Cola FIFO — eventos por clasificar y técnicos disponibles

**Implementación:** `ArrayDeque`, declarada con la interfaz `Queue`:

```java
private final Queue<EventoFallo> eventos = new ArrayDeque<>();
private final Queue<Tecnico> tecnicosLibres = new ArrayDeque<>(tecnicos);
```

Usamos dos colas FIFO, con dos propósitos distintos.

### 4.1 Cola de eventos

Cuando una lectura supera el umbral de advertencia, se crea un `EventoFallo` y se encola:
`eventos.add(...)`. En cada tick se desencola **uno solo**, `eventos.poll()`, y se clasifica.

**Por qué FIFO:** las alarmas detectadas deben pasar a clasificación **en el orden en que
ocurrieron**. Todavía no tienen prioridad: la prioridad la asigna justamente la clasificación. Antes
de clasificarlas no hay ningún criterio para que una pase delante de otra, y el único orden justo es
el de llegada.

**Por qué un solo evento por tick:** si se clasificaran todos de golpe, la cola estaría siempre
vacía y no se vería su comportamiento. Clasificar de a uno representa también una restricción real:
la capacidad de procesamiento del sistema que clasifica es limitada.

### 4.2 Cola de técnicos disponibles

Los técnicos libres esperan en una cola. Al asignar una orden se desencola el primero,
`tecnicosLibres.poll()`. Cuando termina su trabajo, **vuelve al final**, `tecnicosLibres.add(tecnico)`.

**Por qué FIFO:** reparte el trabajo de forma justa. El técnico que lleva más tiempo esperando es el
próximo en recibir trabajo, y quien acaba de terminar se pone de último. Con una lista sin orden, el
mismo técnico podría recibir todas las órdenes mientras el otro espera.

### Por qué `ArrayDeque` y no `LinkedList`

Ambas implementan `Queue` y ofrecen encolar y desencolar en O(1). La diferencia está en cómo
guardan los datos:

| | `ArrayDeque` | `LinkedList` |
|---|---|---|
| Representación | Arreglo circular interno | Un **nodo** por elemento, con referencias al anterior y al siguiente |
| Memoria por elemento | Una referencia en el arreglo | Un objeto nodo completo: encabezado del objeto y tres referencias |
| Basura generada | Casi ninguna | Un objeto nuevo por cada encolado, que luego debe recolectarse |
| Recorrido | Celdas contiguas | Saltos por memoria siguiendo referencias |

La documentación oficial de `ArrayDeque` dice que la clase *"probablemente sea más rápida que
`LinkedList` cuando se usa como cola"*.

### Por qué se declara como `Queue`

La variable es de tipo `Queue`, no `ArrayDeque`, porque `ArrayDeque` también permite sacar por el
final o insertar por el frente. Con el tipo `Queue`, el código solo ve las operaciones de una cola.
Es una forma de **respetar el TAD desde la declaración**.

### Complejidad

| Operación | Método | Costo |
|---|---|---|
| Encolar | `add` | **O(1) amortizado**: el arreglo interno crece cuando se llena, igual que `ArrayList` |
| Desencolar | `poll` | **O(1)** |
| Ver el frente | `peek` | **O(1)** |
| Copia para la vista | `List.copyOf` | O(n) |

### Cómo se ven en la interfaz

Los paneles dibujan **solo la secuencia de elementos**, sin celdas vacías ni capacidad. Es una
decisión deliberada: `ArrayDeque` no expone su arreglo interno, y dibujarlo sería inventar. Las
flechas *sale* y *entra* muestran el comportamiento FIFO.

![Panel de la cola de eventos](img/manual/panel-cola-eventos.png)

![Panel de la cola de técnicos](img/manual/panel-cola-tecnicos.png)

---

## 5. Cola con prioridad — órdenes de trabajo P1 a P4

**Implementación:** la clase `modelo/ColaPrioridadOrdenes`, un **arreglo de cuatro colas FIFO**, una
por nivel de prioridad:

```java
private final Deque<OrdenTrabajo>[] niveles;   // niveles[0] = P1, ..., niveles[3] = P4
```

### El problema que resuelve

Las órdenes de trabajo deben atenderse:

1. **Por urgencia.** Una P1 (paro de máquina) va antes que una P3 (ir a revisar), aunque haya
   llegado después.
2. **Por orden de llegada entre órdenes de la misma prioridad.** Si hay dos P2, se atiende primero
   la más antigua.

El segundo requisito es tan importante como el primero. En mantenimiento real, atender la segunda
alarma antes que la primera, siendo igual de graves, es un error operativo.

### Cómo funciona

El nivel de la prioridad **es** el índice del arreglo.

**Insertar** va directo a la cola del nivel, sin buscar ni comparar:

```java
public void insertar(OrdenTrabajo orden) {
    niveles[orden.prioridad().nivel()].addLast(orden);
}
```

**Extraer** recorre los niveles desde P1 y saca el frente de la primera cola que tenga algo:

```java
public Optional<OrdenTrabajo> extraer() {
    for (Deque<OrdenTrabajo> cola : niveles) {
        if (!cola.isEmpty()) {
            return Optional.of(cola.removeFirst());
        }
    }
    return Optional.empty();
}
```

Los dos requisitos se cumplen **por construcción**:

- La urgencia se respeta porque se revisa P1 antes que P2, P2 antes que P3, y así sucesivamente.
- El orden de llegada se respeta porque **cada nivel es una cola FIFO**. No hubo que programar
  ningún criterio de desempate: sale solo de la estructura.

### Complejidad

| Operación | Costo | Por qué |
|---|---|---|
| Insertar | **O(1)** (amortizado, por `ArrayDeque`) | El nivel da el índice directamente |
| Extraer | **O(k), con k = 4** | En el peor caso revisa las cuatro colas |
| Ver el siguiente | O(k) | Mismo recorrido, sin sacar |
| Tamaño de un nivel | O(1) | `size()` de esa cola |
| Tamaño total | O(k) | Suma los cuatro tamaños |

**Por qué O(k) es, en la práctica, tiempo constante.** k es el número de niveles de prioridad, que
está fijado en 4 por el enum `Prioridad`. **No depende de cuántas órdenes haya.** Con 10 órdenes o
con 10 000, extraer revisa como máximo 4 colas.

### Alternativas descartadas

**`PriorityQueue` de Java.** Es la opción "obvia" y la descartamos por dos razones:

1. **No garantiza el orden de llegada entre elementos de igual prioridad.** Su documentación oficial
   lo dice explícitamente: si varios elementos empatan en la prioridad más alta, *"los empates se
   rompen arbitrariamente"*. Dos P2 podrían salir en orden invertido. Para evitarlo habría que
   agregarle a cada orden un número de secuencia y un comparador que lo use: complejidad extra para
   recuperar algo que nuestra estructura cumple sola.
2. **Internamente es un *heap*** (montículo binario), una estructura de árbol, no lineal. Esta
   entrega se limita a estructuras lineales y el curso aún no ha visto heaps.

Además, sus costos son O(log n) para insertar y extraer, que dependen de la cantidad de órdenes.
Los nuestros son O(1) y O(4).

**Una sola lista ordenada por prioridad.** Es la versión que vimos en clase, con una lista enlazada.
Extraer es O(1), porque la más urgente siempre está en la cabeza. Pero **insertar es O(n)**, y el
motivo depende de cómo esté hecha la lista:

- **Con una lista enlazada**, hay que **recorrer** los nodos desde la cabeza hasta encontrar la
  posición. Una vez ahí, enlazar el nodo nuevo es O(1): no se mueve ningún elemento.
- **Con un arreglo ordenado**, la posición se puede buscar rápido, pero hay que **desplazar** una
  posición todos los elementos que siguen para abrir el hueco.

En los dos casos el costo crece con la cantidad de órdenes. Nuestra versión inserta en O(1).

Además, en la lista ordenada el orden de llegada entre iguales **depende de programarlo bien**: la
orden nueva debe quedar **después** de todas las de su misma prioridad. Si el recorrido se detiene
en la primera de igual prioridad, la nueva queda delante y el orden se invierte. En nuestra versión
ese error no puede ocurrir, porque cada nivel es una cola y solo se puede entrar por el final.

**Nuestra versión no compite con la lista enlazada.** Cada uno de los cuatro niveles es una cola, y
esa cola podría estar hecha con nodos enlazados en lugar de `ArrayDeque`. La idea central del diseño
es **el arreglo indexado por prioridad**, no cómo está implementado cada carril. Elegimos
`ArrayDeque` para los carriles por las razones de la sección 4.

**Una sola cola, buscando la más prioritaria al extraer.** Insertar sería O(1), pero extraer exigiría
recorrer todas las órdenes y sacar una del medio: O(n). Además, sacar del medio no es una operación
de cola.

| Alternativa | Insertar | Extraer | Orden de llegada entre iguales |
|---|---|---|---|
| **Arreglo de 4 colas (nuestra)** | **O(1)** | **O(4)** | **Garantizado por construcción** |
| `PriorityQueue` | O(log n) | O(log n) | **No garantizado** |
| Lista enlazada ordenada (vista en clase) | O(n), por el recorrido | O(1) | Solo si se inserta después de los iguales |
| Arreglo ordenado | O(n), por el desplazamiento | O(1) | Solo si se inserta después de los iguales |
| Una cola sin orden | O(1) | O(n) | Depende de cómo se busque |

### Limitación de la elección

Esta implementación funciona porque **los niveles son pocos y fijos**. Si la prioridad fuera un
número continuo, por ejemplo un puntaje de 0 a 1000, haría falta una cola por valor posible, y
ahí sí convendría un heap. En nuestro dominio no es el caso: la norma ISA-18.2, en la que nos
basamos, recomienda trabajar con pocos niveles de prioridad (típicamente tres o cuatro).

### Detalles de implementación

- El arreglo es de `ArrayDeque`, por las mismas razones de la sección 4.
- `tamano()` **suma los cuatro niveles** en vez de llevar un contador aparte. Cuesta O(4) y evita un
  dato que podría quedar desincronizado con las colas.
- El enum `Prioridad` tiene un bloque estático que **verifica que `nivel == ordinal()`**. Si alguien
  reordena las constantes del enum y olvida corregir los niveles, el programa falla al arrancar en
  vez de mandar órdenes al carril equivocado.
- La vista la recibe como `ColaPrioridadSoloLectura`, una interfaz sin métodos para insertar ni
  extraer. `ordenesDe(prioridad)` entrega una **copia no modificable** de cada carril.

### Cómo se ve en la interfaz

El panel muestra la estructura tal como es: **cuatro carriles horizontales, cada uno una fila FIFO**.
Una flecha marca el carril del que sale la próxima orden. En la imagen, `OT-12 BOMBA-01` llegó
**después** de todas las demás, pero es P1 y sale primero. Dentro del carril P3, `OT-9` sale antes
que `OT-11` porque llegó antes.

![Una P1 que llegó de última sale primero](img/manual/panel-cola-prioridad-p1.png)

---

## 6. Pila — deshacer acciones del operador

**Implementación:** `ArrayDeque`, con `push` y `pop`:

```java
private final Deque<ComandoOperador> pilaDeshacer = new ArrayDeque<>();
```

### El problema que resuelve

El operador puede cambiar la carga de una máquina o pararla y arrancarla a mano. Si se equivoca,
quiere **revertir lo último que hizo**. Si vuelve a pedir deshacer, quiere revertir lo anterior a
eso. Es el orden inverso al de ejecución: **último en entrar, primero en salir (LIFO)**, que es
justamente una pila.

### Cómo funciona: el patrón Comando

Cada acción deshacible es un objeto que implementa `ComandoOperador`, con tres operaciones:
`ejecutar`, `deshacer` y `sePuedeDeshacer`. Hay dos comandos: `CambiarCargaMaquina` y
`PararOArrancarMaquina`.

Al crearse, cada comando **guarda el estado anterior**. Por ejemplo, `CambiarCargaMaquina` guarda la
carga que había antes. Así sabe cómo revertirse.

```java
// Planta.ejecutar
comando.ejecutar();
pilaDeshacer.push(comando);

// Planta.deshacer
ComandoOperador comando = pilaDeshacer.pop();
if (comando.sePuedeDeshacer()) {
    comando.deshacer();
} else {
    registrar("Descartado, la simulación ya cambió el estado: " + comando.descripcion());
}
```

### Complejidad

| Operación | Costo |
|---|---|
| `push` (apilar) | **O(1) amortizado** |
| `pop` (desapilar) | **O(1)** |
| Copia de las descripciones para la vista | O(n) |

### Por qué `ArrayDeque` y no `Stack`

Java tiene una clase llamada `Stack`, y **la descartamos a propósito**:

1. **Hereda de `Vector`**, que **sincroniza todos sus métodos**. Cada `push` y cada `pop` adquieren
   un candado pensado para varios hilos. Nuestro programa corre en un solo hilo, así que ese costo
   no aporta nada.
2. **Rompe el concepto de pila.** Al heredar de `Vector`, `Stack` permite `get(i)`, `add(i, x)` y
   `remove(i)`: leer, insertar y borrar en cualquier posición. Una pila solo debería permitir tocar
   la cima.
3. **La documentación oficial de Java recomienda no usarla.** La propia página de `Stack` dice que
   la interfaz `Deque` ofrece *"un conjunto más completo y consistente de operaciones de pila"* y
   que debe preferirse.

### Y la pila con nodos que vimos en clase

En clase implementamos la pila como una lista enlazada: cada elemento es un nodo que apunta al de
abajo, y la pila guarda una referencia a la cima. Esa versión es correcta y tiene el mismo costo:

| | Lista enlazada (clase) | `ArrayDeque` (proyecto) |
|---|---|---|
| `push` | O(1): se crea un nodo y pasa a ser la cima | O(1) amortizado: se escribe en el arreglo, que crece si se llena |
| `pop` | O(1): la cima pasa a ser el nodo de abajo | O(1) |
| Memoria por elemento | Un objeto nodo completo, con el dato y la referencia al siguiente | Una referencia en el arreglo |
| Ventaja principal | Nunca hay que copiar elementos | Menos memoria y menos objetos que recolectar |

Las dos implementan **el mismo tipo abstracto de datos**: solo se toca la cima, y lo último en
entrar es lo primero en salir. Elegimos `ArrayDeque` porque ya viene probada en la librería
estándar, ocupa menos memoria y la documentación de Java la recomienda para pilas. Implementar
nuestra propia pila con nodos no habría agregado ningún comportamiento nuevo. La única estructura que
implementamos fue la cola circular, porque ninguna clase de Java ofrece lo que necesitábamos.

El "costo" de la versión enlazada, crear un objeto por elemento, no se nota en una pila de unas
pocas acciones del operador. La decisión no se tomó por rendimiento, sino por **usar la herramienta
estándar cuando ya existe**, y dedicar el esfuerzo de implementación a la que no existe.

### Qué se puede deshacer y qué no

**Se puede deshacer:** cambiar la carga, y parar o arrancar una máquina.

**No se puede deshacer:** clasificar un evento, asignar una orden a un técnico ni inyectar un fallo.

Esta es la decisión de diseño más importante de la sección, y **la tomamos por respetar el TAD**.
Veamos qué exigiría deshacer cada una:

| Acción | Para deshacerla habría que... | Qué regla se viola |
|---|---|---|
| Clasificar un evento | Sacar la orden de la cola con prioridad, aunque no sea la siguiente, y **volver a poner el evento al frente** de la cola de eventos | En una cola solo se entra por el **final**, y solo sale el elemento del **frente** |
| Asignar una orden | Quitarle la orden al técnico, **reinsertarla al frente de su carril** y devolver al técnico **al frente** de la cola de técnicos | Lo mismo, en dos colas a la vez |
| Inyectar un fallo | Si el fallo ya generó un evento o una orden, **sacarlos del medio** de su cola | No se puede sacar un elemento del medio de una cola |

Técnicamente, `ArrayDeque` tiene `addFirst`, así que podríamos haberlo hecho. Pero hacerlo
convertiría nuestras colas en algo que ya no son colas, y el resto del sistema perdería las
garantías de orden que justifican usarlas. **Preferimos no ofrecer la función antes que romper el
tipo abstracto de datos.**

### El caso borde: deshacer algo que la simulación ya cambió

Ejemplo: el operador arranca una máquina, una alarma crítica (P1) la detiene, y después el operador
pulsa *Deshacer*. Si el comando se aplicara, la máquina pasaría a "parada manual" e ignoraría la
alarma.

Por eso cada comando verifica, con `sePuedeDeshacer()`, que **el estado actual siga siendo el que él
dejó**. Si no, el comando **se saca de la pila pero no se aplica**, y la bitácora lo registra. La
pila sigue siendo una pila: se hace `pop` de la cima. Lo único que cambia es si la acción se revierte.

### Cómo se ve en la interfaz

Pila vertical, con **la cima arriba**, resaltada, y el botón *Deshacer* debajo. La vista recibe solo
**textos**, no los objetos comando. Si recibiera los comandos, podría ejecutarlos o revertirlos por
su cuenta, y eso saltaría al controlador.

![Panel de la pila](img/manual/panel-pila.png)

---

## 7. Cómo interactúan las estructuras en un tick

El método `Planta.tick()` ejecuta ocho pasos en un orden fijo. La tabla muestra qué estructura toca
cada paso:

| # | Paso | Estructuras | Operación |
|---|---|---|---|
| 1 | Generar fallos | Arreglo dinámico | Recorrer: a cada máquina operando y sin anomalía se le sortea un fallo |
| 2 | Leer sensores | Arreglo dinámico → **cola circular** | Recorrer; cada máquina en marcha **encola** su lectura (sobrescribiendo si está llena) |
| 3 | Detectar | Cola circular → **cola de eventos** | **Ver la más reciente**; si supera el umbral, **encolar** un `EventoFallo` |
| 4 | Clasificar | Cola de eventos → **cola con prioridad** | **Desencolar un evento**, clasificarlo e **insertar** la orden en su nivel |
| 5 | Avanzar trabajos | **Cola de técnicos** | Al terminar, la máquina vuelve a operar y el técnico se **encola al final** |
| 6 | Asignar | **Cola con prioridad** + **cola de técnicos** | Mientras haya ambos: **extraer** la orden más urgente y **desencolar** un técnico |
| 7 | Preventivo | Arreglo dinámico → **cola con prioridad** | Recorrer; si una máquina cumplió su intervalo, **insertar** una orden P4 |
| 8 | Notificar | — | Avisar a los observadores (la vista) para que redibujen |

La **pila** no participa en el tick: solo la tocan las acciones del operador.

### Un ejemplo completo

Al inyectar un fallo severo en la vibración de PRENSA-01, un solo tick recorre cuatro estructuras.
Esta es la bitácora real con la semilla 7:

```
Tick 14: PRENSA-01: Vibración en 12,5 mm/s → evento encolado      ← cola circular → cola de eventos
Tick 14: Clasificado: OT-3 PRENSA-01 → P1 CRÍTICA                 ← cola de eventos → cola con prioridad
Tick 14: PRENSA-01: paro inmediato por alarma crítica
Tick 14: Ana atiende OT-3 PRENSA-01 (P1, 10 ticks)                ← cola con prioridad + cola de técnicos
```

### Por qué el orden de los pasos importa

- La anomalía aparece (paso 1) **antes** de que se lea el sensor (paso 2), para que la lectura la
  refleje en el mismo tick.
- Los trabajos avanzan (paso 5) **antes** de asignar (paso 6), para que un técnico que acaba de
  terminar pueda recibir otra orden en el mismo tick.

### Por qué hay dos colas: `EventoFallo` y `OrdenTrabajo`

Podríamos haber convertido cada alarma en orden de inmediato, con una sola cola. Las separamos
porque **así está dividido el trabajo en la industria**:

| Etapa | En la industria | En el simulador |
|---|---|---|
| Detectar | El PLC compara la lectura con un límite | Paso 3, genera un `EventoFallo` **sin prioridad** |
| Clasificar | El SCADA decide la gravedad de la alarma | El `Clasificador`, paso 4 |
| Gestionar | El sistema de mantenimiento crea y asigna la orden | `OrdenTrabajo` en la cola con prioridad, paso 6 |

Por eso son dos estructuras distintas. **Antes de clasificar no hay prioridad, así que el único orden
posible es el de llegada (FIFO). Después de clasificar sí la hay, así que se usa la cola con
prioridad.** Cada cola corresponde a lo que se sabe del dato en ese momento.

Además, el `Clasificador` **no conoce** la magnitud con que se generó el fallo. Solo compara el valor
leído contra los umbrales del sensor, así que funcionaría igual con datos de una planta real.

### Por qué el modelo nunca entrega las estructuras originales

La vista dibuja las estructuras, pero **no puede modificarlas**. Cada una se entrega de una forma
que lo impide:

| Estructura | Lo que recibe la vista |
|---|---|
| Inventario | `Collections.unmodifiableList(inventario)` |
| Cola de eventos y cola de técnicos | `List.copyOf(...)`: una copia inmutable |
| Cola circular | La interfaz `ColaCircularSoloLectura`, sin métodos de modificación |
| Cola con prioridad | La interfaz `ColaPrioridadSoloLectura`; cada carril como copia inmutable |
| Pila | Una lista no modificable de **textos**, no de comandos |

Así, la única forma de cambiar el estado es a través del controlador, como exige la arquitectura
MVC del proyecto.

### Por qué 6 máquinas y 2 técnicos

Una máquina con un fallo activo no desarrolla otro, así que el número de máquinas pone un **techo**
a los fallos simultáneos. Las órdenes sí pueden superar ese número, porque una máquina puede tener
a la vez una correctiva y un preventivo P4. En una primera versión con 3 máquinas, medimos 1000 ticks y **la cola con
prioridad nunca pasó de una orden**: los paneles se habrían visto vacíos. Con 6 máquinas,
reparaciones de 4 a 10 ticks y 2 técnicos, la cola tiene órdenes en cerca del **38 % de los ticks**,
con un máximo de 4. La planta se ve ocupada pero no saturada. El detalle de esa calibración está en
[`BITACORA_DESARROLLO.md`](BITACORA_DESARROLLO.md).

---

## 8. Pruebas

Hay **60 pruebas JUnit 5**, todas en verde (`./mvnw test`). Están agrupadas con `@Nested` y escritas
con `@DisplayName` en español, para que el informe se lea como una lista de propiedades.

### `ColaCircularTest` — los seis casos obligatorios

| Caso exigido | Qué se verifica |
|---|---|
| **Cola vacía** | No tiene elementos; todas sus celdas están vacías; desencolar y ver el frente lanzan excepción; frente y final apuntan a la misma celda |
| **Llenado exacto** | Sin sobrescrituras; elementos en celdas consecutivas desde la 0; con la cola llena el final vuelve a coincidir con el frente |
| **Sobrescritura** | Al encolar en una cola llena se descarta el más antiguo, y el nuevo ocupa **la celda física** del que salió |
| **Varias vueltas completas** | Tras diez elementos en una cola de tres solo quedan los tres últimos; el frente vuelve a pasar por la celda 0; la capacidad nunca cambia |
| **Desencolar después de dar la vuelta** | Los elementos salen en orden de antigüedad aunque el arreglo esté rotado; se libera la celda; se puede volver a encolar |
| **Capacidad 1** | Cada elemento nuevo reemplaza al anterior; frente y final siempre en la única celda |

Además: el rechazo de capacidad inválida y de nulos, y que las listas que recibe la vista sean
copias no modificables.

### `ColaPrioridadOrdenesTest` — los cuatro casos obligatorios

| Caso exigido | Qué se verifica |
|---|---|
| **Orden por prioridad** | Sale primero la de mayor prioridad, sin importar el orden de inserción |
| **FIFO dentro del mismo nivel** | Dentro de un nivel se respeta el orden de llegada, en los cuatro niveles a la vez |
| **Una P1 insertada de última sale primero** | Exactamente eso |
| **Extraer con la estructura vacía** | Devuelve un `Optional` vacío, sin lanzar excepción |

Además: el contrato del enum `Prioridad` (que el nivel coincida con la posición, porque es el índice
del arreglo) y que los carriles entregados a la vista no se puedan modificar.

### `PlantaTest` — la simulación sin interfaz gráfica

Seis pruebas de cadenas completas del tick, sin abrir ninguna ventana:

- La misma semilla produce exactamente la misma simulación.
- Una anomalía severa termina en una orden P1 asignada a un técnico.
- Se clasifica un solo evento por tick.
- Cuando el técnico termina, la máquina vuelve a operar sin anomalía.
- El preventivo genera una sola orden P4 por máquina, no una por tick.
- Deshacer se descarta si la simulación ya cambió el estado.

Que el modelo se pueda probar sin interfaz demuestra que la separación MVC es real.

### Otras pruebas

- `ConfiguracionPlantaTest`: ningún sensor da alarmas falsas por ruido, y cada magnitud de fallo deja
  la lectura en su zona de umbrales.
- `ControladorPlantaTest`: un paso avanza exactamente un tick, reiniciar repite la simulación, parar
  una máquina se puede deshacer.
- `PanelesVistaTest`: todos los paneles se dibujan sin fallar.

---

## 9. Tabla resumen de complejidades

n = cantidad de elementos de la estructura. k = 4 niveles de prioridad, constante.

| Estructura | Insertar | Extraer | Consultar | Recorrer |
|---|---|---|---|---|
| `ArrayList` (inventario) | `add` al final: **O(1) amortizado** | No se usa; costaría O(n) | `get(i)`: **O(1)** | O(n) |
| `ColaCircular` (lecturas) | `encolar`: **O(1)**, incluso al sobrescribir | `desencolar`: **O(1)** | Frente, más reciente o celda i: **O(1)** | O(n) |
| Cola FIFO (`ArrayDeque`) | `add`: **O(1) amortizado** | `poll`: **O(1)** | `peek`: **O(1)** | O(n) |
| Cola con prioridad | `insertar`: **O(1) amortizado** | `extraer`: **O(k) = O(4)** | Siguiente: O(k); tamaño de un nivel: O(1) | O(n) |
| Pila (`ArrayDeque`) | `push`: **O(1) amortizado** | `pop`: **O(1)** | `peek`: **O(1)** | O(n) |

**Por qué "amortizado" en casi todas.** `ArrayList` y `ArrayDeque` guardan sus elementos en un
arreglo que **crece** cuando se llena, copiando todo. Esa copia ocasional es O(n), pero repartida
entre todas las inserciones da O(1) en promedio.

**La excepción es la `ColaCircular`.** Su arreglo **nunca crece**, así que encolar es O(1) siempre,
sin promedios. Es la consecuencia directa de tener memoria fija.

---

# Segunda parte — Entrega 2

> En la Entrega 2 el proyecto pasó a **Spring Boot**: este repositorio es el backend y la interfaz es
> un frontend web en otro repositorio (`ProyectoFinalED-frontend`). Las capturas de la primera parte
> son de la interfaz Swing de la Entrega 1, que se conserva en la etiqueta de Git `entrega-1`. **Las
> estructuras de la primera parte no cambiaron**: el modelo es el mismo y sus pruebas siguen pasando.
> Lo que sigue son las estructuras **nuevas**, con el mismo formato: qué problema resuelven, por qué
> son las adecuadas, qué alternativas se descartaron y cuánto cuesta cada operación.

---

## 10. Introducción a la Entrega 2

### Qué se agregó

La planta de la Entrega 1 pasa a ser **una compañía** dentro de una **organización** con varias. Un
**superusuario** las ve todas, y el modo manual se convirtió en un **juego de gestión**:

- Se pueden **crear compañías y máquinas**, con nombres que no se repiten.
- Un **ranking** ordena las máquinas por un **puntaje de fallos** y recomienda **overhaul** (una
  reparación mayor, hecha por técnicos del fabricante) o **reemplazo**.
- Las **solicitudes de overhaul** aprobadas esperan a unos pocos técnicos del fabricante compartidos
  por todas las compañías.
- Cada fallo es **mecánico** o **eléctrico**, y cada técnico tiene esa especialidad.
- Un **historial** responde cuántos fallos y cuánto gasto hubo entre dos ticks, y cuál fue el pico.

### Resumen

| Estructura | Implementación | Uso | Dónde está |
|---|---|---|---|
| Arreglo dinámico | `ArrayList<Planta>` | Compañías; el id es el índice | `Organizacion.companias` |
| Conjunto (árbol de búsqueda) | `TreeSet<String>`, declarado como `Set` | Nombres de compañías y etiquetas de máquinas, sin repetir | `Organizacion.nombres`, `Planta.etiquetas` |
| Árbol de búsqueda balanceado | `TreeSet<Maquina>` (rojo-negro) | Ranking por puntaje de fallos, por compañía y global | `RankingFallos.arbol` |
| Montículo | `PriorityQueue<SolicitudOverhaul>` (binario) | Solicitudes de overhaul, la peor máquina primero | `Organizacion.solicitudes` |
| Dos colas FIFO | `ArrayDeque<Tecnico>` en un `EnumMap` | Técnicos internos libres, una cola por especialidad | `Planta.tecnicosLibres` |
| Cola FIFO | `ArrayDeque<TecnicoExterno>` | Técnicos del fabricante libres | `Organizacion.externosLibres` |
| Árbol de segmentos | **`ArbolSegmentos`, propio** | Historial por tick de fallos y de costo | `Planta` y `Organizacion`, dos cada una |

### El mismo criterio de la primera parte

El criterio de la sección 1 se volvió la regla del juego: **el jugador decide _qué_ hacer, _cuándo_
y _con qué recurso_; la estructura decide _cuál_ elemento sale.** El jugador puede elegir a qué cola
de técnicos mandar una orden, pero no elegir la orden ni el técnico: eso sería sacar del medio de una
cola con prioridad o saltarse una FIFO. Cada botón ejecuta la operación del tipo abstracto
(`extraer`, `poll`) y nada más.

---

## 11. Arreglo dinámico — las compañías

**Implementación:** `ArrayList<Planta>`, en el campo `Organizacion.companias`.

Es el mismo argumento del inventario (sección 2), un nivel más arriba:

| Operación | Costo | Cuándo |
|---|---|---|
| `get(id)` | **O(1)** | Cada petición de la API trae el id de la compañía: `/api/companias/{id}` |
| Recorrer todas | O(c) | En cada tick, para ejecutar el tick de cada compañía en orden |
| `add` al final | **O(1) amortizado** | Al crear una compañía; su id es el índice que le tocó |
| Eliminar | O(c) | **Nunca** |

**Por qué no se eliminan compañías ni máquinas.** `remove` en un `ArrayList` cuesta O(n) y, peor,
**corre los índices** de los que vienen después. Los índices son los ids de la API y la clave de las
máquinas en los rankings, así que cambiarían bajo los pies del frontend y de los árboles. Además, una
máquina eliminada podría tener una solicitud en el montículo, y sacarla con
`PriorityQueue.remove(objeto)` cuesta O(n) y extrae un elemento que no es la cima.

**El reemplazo usa `set`.** Cuando una máquina se reemplaza, la nueva entra en **el mismo índice** del
inventario con `inventario.set(indice, nueva)`, O(1). Así se cumple "no se elimina nada" y la clave
`(idCompania, indice)` sigue siendo la misma.

**Por qué no un árbol n-ario.** La jerarquía organización → compañías → máquinas tiene profundidad
fija: crear una compañía o una máquina agrega hermanos, no niveles. Una lista de compañías, cada una
con su lista de máquinas, ya es ese árbol de forma implícita. El árbol n-ario se justificaría si el
usuario pudiera crear niveles (sedes, líneas de producción) con profundidad variable; quedó como
opción para la entrega final.

---

## 12. Conjunto — nombres que no se repiten

**Implementación:** `TreeSet<String>`, declarado como `Set<String>`. Hay uno en la organización
(`Organizacion.nombres`, los nombres de las compañías) y uno por compañía (`Planta.etiquetas`, las
etiquetas de sus máquinas).

### El problema que resuelve

Al crear una compañía o una máquina hay que responder **"¿ya existe este nombre?"**. Un conjunto es
exactamente el tipo abstracto para eso: una colección **sin repetidos** cuya operación principal es
preguntar si un elemento pertenece.

### Cómo funciona

`TreeSet` es un **árbol rojo-negro**: un árbol binario de búsqueda que se rebalancea solo, así que su
altura nunca pasa de 2·log₂(n+1). Buscar es bajar desde la raíz comparando: a la izquierda si el
nombre buscado es menor, a la derecha si es mayor.

| Operación | Costo |
|---|---|
| `contains(nombre)` | **O(log n)** |
| `add(nombre)` | **O(log n)** |

Antes de guardar, el nombre se **normaliza**: sin espacios a los lados, y en minúsculas para las
compañías o en mayúsculas para las etiquetas. Así "planta norte" y "Planta Norte " cuentan como el
mismo. La compañía se muestra como la escribió el usuario; el conjunto guarda solo la clave.

### Por qué una etiqueta es única solo dentro de su compañía

Cada compañía tiene su propio conjunto de etiquetas, así que `PRENSA-01` puede existir en todas. La
identidad de una máquina no es su etiqueta sino su **clave compuesta** `(idCompania, indice)`, como
la clave primaria de una tabla. Que las etiquetas no se repitan dentro de una compañía es una regla
del negocio (para que el operador no se confunda), no algo que necesite una estructura.

### Alternativas descartadas

- **Recorrer la lista de compañías o el inventario:** O(n) en cada creación, contra O(log n).
- **`HashSet`:** también sirve, con O(1) esperado. No lo usamos porque las tablas hash no están en
  los temas del curso, y `TreeSet` da O(log n) garantizado. Con 3 a 20 nombres la diferencia no se
  nota: el argumento no es la velocidad sino usar el tipo abstracto correcto. Como el campo se
  declara `Set<String>`, cambiar a `HashSet` sería una sola palabra.

---

## 13. Árbol de búsqueda balanceado — el ranking de fallos

**Implementación:** `TreeSet<Maquina>` (árbol rojo-negro), envuelto en la clase `RankingFallos`. Cada
compañía tiene el suyo y hay uno **global** con las máquinas de todas.

### El problema que resuelve

Cada vez que una máquina falla, su **puntaje de fallos** sube según la prioridad: P1 suma 3, P2 suma
2, P3 suma 1 (un preventivo P4 no es un fallo y suma 0). El superusuario quiere ver **las máquinas
ordenadas de peor a mejor**, y el frontend lo consulta **en cada sondeo**, cada medio segundo.

### Cómo funciona

El árbol ordena las máquinas con un solo comparador:

```java
Comparator.comparingInt(Maquina::puntajeFallos).reversed()   // mayor puntaje primero
        .thenComparingInt(Maquina::idCompania)               // desempate 1
        .thenComparingInt(Maquina::indice);                  // desempate 2
```

El desempate no es un detalle: un `TreeSet` considera **iguales** a dos elementos que el comparador
dice que son iguales, y guarda solo uno. Sin desempate, dos máquinas con el mismo puntaje se
"fundirían" y una desaparecería del ranking. La clave usa solo números que **nunca cambian** (el id
de la compañía y el índice), y no las etiquetas.

| Operación | Costo | Cuándo |
|---|---|---|
| Insertar o eliminar una máquina | **O(log n)** | En cada fallo, overhaul o reemplazo |
| Recorrer en orden (el ranking completo) | **O(n)** | En cada consulta del ranking |
| Las máquinas con puntaje ≥ k | **O(log n + k)** | Las candidatas a overhaul |

El recorrido **en orden** de un árbol de búsqueda sale ya ordenado: la consulta no ordena nada.

### La trampa de la clave que cambia

Es el error más importante de entender en esta entrega, porque **no lanza ninguna excepción**.

El árbol ubica cada máquina según su puntaje **en el momento en que se insertó**. Si el puntaje
cambia con la máquina adentro, el árbol no se entera: la máquina queda en una posición que ya no le
corresponde. Por ejemplo:

1. B (puntaje 3) está en la raíz y A (puntaje 5) es su hijo izquierdo: en orden descendente, el
   mayor puntaje va antes.
2. Se hace `A.puntaje = 1` **sin sacarla**. A sigue a la izquierda de B, pero con puntaje 1 debería
   estar a la derecha. El ranking ya está mal ordenado.
3. Se intenta `remove(A)`. El árbol compara A (ahora 1) con la raíz B (3), concluye que A va
   **después** y la busca en el subárbol **derecho**... pero A está en el izquierdo. **No la
   encuentra y `remove` devuelve `false`.** A queda para siempre en el árbol con una clave falsa.

Por eso **el puntaje solo cambia en un lugar**, `RankingFallos.sumarFallo`, y siempre en tres pasos:

```java
public void sumarFallo(Maquina maquina, int peso) {
    quitar(maquina);            // 1. sacarla con la clave vieja, que es la que el árbol conoce
    maquina.sumarPuntaje(peso); // 2. cambiar la clave afuera del árbol
    agregar(maquina);           // 3. volverla a meter con la clave nueva
}
```

Y `quitar` **no tolera** que la máquina no esté: si `remove` devuelve `false`, lanza una excepción
que dice que el puntaje cambió por fuera del ranking. Así la trampa, si alguna vez ocurre, se ve.

La misma regla aplica al cerrar un overhaul (el puntaje vuelve a 0) y al reemplazar una máquina (la
vieja sale, la nueva entra).

### Una máquina vive en dos árboles

Cada máquina está en el ranking de su compañía **y** en el global. El ranking de cada compañía
guarda una referencia al global, y `agregar`, `quitar` y `sumarFallo` actúan sobre los dos. Así no
hay forma de actualizar uno y olvidar el otro. Un fallo cuesta O(log n) en cada árbol: O(log n) en
total.

### La consulta por rango

Las candidatas a overhaul son las máquinas con puntaje ≥ `umbralOverhaul`. Como el árbol está
ordenado de mayor a menor, son **un prefijo** del recorrido: `conPuntajeMinimo` recorre en orden y
**se detiene en la primera máquina por debajo del umbral**. Cuesta O(log n) llegar a la primera, más
O(k) por las k que devuelve: no toca el resto del árbol.

`TreeSet` también ofrece `headSet` y `tailSet`, pero reciben un **elemento** del árbol, no un
puntaje: habría que fabricar una máquina ficticia con el puntaje del umbral solo para usarla de
límite. El recorrido con corte da lo mismo y se explica solo.

### Alternativas descartadas

- **Ordenar el inventario en cada consulta:** O(n log n) por consulta, y el frontend consulta en
  cada sondeo. El árbol cambia eso por O(log n) **por fallo**. Es un argumento de **lecturas contra
  escrituras**: hay muchas más consultas que fallos, así que conviene pagar al escribir.
- **Montículo:** da la peor máquina en O(1), pero no la lista ordenada, y `PriorityQueue` no permite
  cambiar la clave de un elemento sin sacarlo (sacar uno que no es la cima cuesta O(n)).
- **AVL propio:** daría las mismas complejidades. Como la librería está permitida, el rojo-negro de
  `TreeSet` es suficiente; escribir un AVL no agregaría nada al proyecto.
- **Árbol B:** pensado para minimizar los accesos a disco con nodos grandes. Aquí todo está en
  memoria y hay pocas máquinas.

---

## 14. Montículo — las solicitudes de overhaul

**Implementación:** `PriorityQueue<SolicitudOverhaul>`, que es un **montículo binario**, en el campo
`Organizacion.solicitudes`. Es uno solo para todas las compañías.

### El problema que resuelve

Cuando el superusuario **aprueba** un overhaul, la solicitud espera a un técnico del fabricante. Hay
solo **2**, compartidos por todas las compañías, así que las solicitudes se acumulan. Cuando un
técnico se libera, debe atender **la máquina en peor estado**, no la que se aprobó primero.

### Cómo funciona

Un montículo binario es un árbol binario **completo** guardado en un arreglo, sin nodos ni
referencias: los hijos de la posición i están en 2i+1 y 2i+2, y su padre en (i−1)/2. La única regla
es que **cada padre es mejor que sus hijos**, así que el mejor de todos está en la raíz. No está
ordenado de izquierda a derecha: solo de arriba abajo.

| Operación | Cómo | Costo |
|---|---|---|
| `add` (aprobar) | Se pone al final y **sube** mientras sea mejor que su padre | **O(log s)** |
| `peek` (ver la cima) | La posición 0 | **O(1)** |
| `poll` (despachar) | El último pasa a la raíz y **baja** cambiando con su mejor hijo | **O(log s)** |

### Por qué ahora sí, si en la Entrega 1 se descartó

En la Entrega 1 se descartó `PriorityQueue` para las órdenes P1–P4 por dos razones (sección 5): era
un montículo, que no se había visto en el curso, y **no garantiza el orden de llegada** entre
elementos con la misma prioridad. Ahora el montículo ya es tema del curso, y la segunda razón se
resuelve con un **número de secuencia**:

```java
Comparator.comparingInt(SolicitudOverhaul::puntajeCongelado).reversed() // peor máquina primero
        .thenComparingLong(SolicitudOverhaul::secuencia);               // en un empate, la que llegó antes
```

Cada aprobación recibe la siguiente secuencia (1, 2, 3...), así que **no hay dos solicitudes
iguales** para el comparador, y entre dos con el mismo puntaje sale primero la más antigua.

Y la razón para no cambiar la cola P1–P4 de la Entrega 1 sigue en pie: allí la clave tiene **solo 4
valores**, y un arreglo de 4 colas da insertar O(1) y extraer O(4), mejor que O(log n). Aquí la clave
es un puntaje que puede tomar muchos valores, y el arreglo de colas no sirve.

### El puntaje se congela

La clave del montículo es el puntaje **en el momento de aprobar**, copiado dentro de la solicitud
(`puntajeCongelado`). Mientras espera, la máquina sigue operando y puede fallar más. Si el montículo
leyera el puntaje vivo de la máquina, la clave cambiaría con la solicitud adentro: es la **misma
trampa** del ranking (sección 13), y `PriorityQueue` tampoco se reordena sola.

### Una solicitud por máquina, sin buscar en el montículo

Una máquina con una solicitud pendiente no puede tener otra. Preguntar `solicitudes.contains(...)`
recorrería todo el arreglo, O(s). En cambio, la máquina guarda una **marca** `overhaulPendiente`: se
pone al aprobar y se quita al despachar. Consultarla es O(1). La API la expone para que el frontend
deshabilite el botón de aprobar.

### La regla de la cima

En cada tick, mientras haya técnicos del fabricante libres, se **mira la cima** (`peek`). Si su
máquina está libre (sin alarma, sin orden pendiente, fuera de mantenimiento), se extrae y se le
asigna un técnico. Si **no** está libre, el camión **espera** al tick siguiente: **no se salta a la
segunda solicitud**, porque sacar un elemento que no es la cima viola el tipo abstracto.

### Cómo se muestra

`PriorityQueue` no expone su arreglo interno: el orden de `toArray()` no está garantizado y
cambia con la historia de inserciones. Por eso el frontend **no dibuja el árbol**; dibujarlo sería
inventar una memoria que no conocemos (el mismo criterio de la sección 2). La API entrega:

- la **cima**, con `peek`, O(1);
- el **orden de salida**: se copia el montículo y se extrae de la copia hasta vaciarla,
  O(s log s). El montículo real no se toca. Con s pequeño (unas pocas solicitudes), es barato.

---

## 15. Colas FIFO — técnicos por especialidad y técnicos del fabricante

### 15.1 Dos colas de técnicos internos

**Implementación:** un `EnumMap<TipoFallo, Queue<Tecnico>>` con un `ArrayDeque` por especialidad
(`MECANICO` y `ELECTRICO`), en el campo `Planta.tecnicosLibres`. Reemplaza la cola única de la
Entrega 1.

Cada sensor declara su **tipo de fallo**, y cada orden hereda el de su sensor: la orden **pide** una
especialidad. Al asignar:

1. Se mira la orden más prioritaria (`verSiguiente`), sin sacarla.
2. Se elige **la cola de su especialidad**: O(1), porque el `EnumMap` es un arreglo indexado por el
   enum.
3. Si esa cola tiene a alguien, se extrae la orden y se desencola su **frente**, O(1).

Al terminar el trabajo, el técnico vuelve **al final de la cola de su especialidad**.

**En automático, la orden espera a su especialidad**, aunque haya técnicos libres de la otra. No se
salta a la siguiente orden, por la misma razón de la regla de la cima.

**En manual, el jugador elige la cola.** Si elige la otra especialidad, la reparación **tarda el
doble**. Es una decisión real: atender ya y pagar más, o esperar.

**Alternativa descartada: una sola cola, buscando un técnico de la especialidad.** Habría que
recorrerla, O(n), y sacar un elemento que **no es el frente**, lo que viola el tipo abstracto. Con
una cola por especialidad la búsqueda desaparece: se elige la cola y se desencola su frente.

### 15.2 Técnicos del fabricante

**Implementación:** `ArrayDeque<TecnicoExterno>`, declarado como `Queue`, en
`Organizacion.externosLibres`. Mismo patrón que los internos: el primero de la fila recibe el
siguiente overhaul y, al terminar, vuelve al final.

**Por qué un número fijo de técnicos.** Si se creara un técnico cada vez que llega una solicitud,
nunca habría fila, y el montículo **no tendría nada que decidir**. Con 2 técnicos fijos y
compartidos, las solicitudes se acumulan y el montículo decide cuál sale. Es también lo realista: el
fabricante tiene pocos especialistas.

---

## 16. Árbol de segmentos — el historial por ventana de ticks

**Implementación: propia**, la clase `estructuras/ArbolSegmentos`. Java no trae un árbol de
segmentos. Cada compañía tiene **dos** (puntaje de fallos y costo por tick), y la organización otros
dos con la suma de todas las compañías.

### El problema que resuelve

El superusuario elige una ventana de ticks [a, b] y pregunta:

- **¿Cuántos** fallos (o cuánto gasto) hubo en la ventana? → la **suma** del rango.
- **¿Cuál fue el peor tick** de la ventana? → el **máximo** del rango.

La partida dura 1000 ticks. Recorrer la ventana cuesta O(b − a), hasta 1000 operaciones por consulta,
y el frontend consulta seguido mientras el usuario mueve la ventana.

### Cómo funciona

Es un árbol binario donde **cada nodo resume un rango** de ticks: guarda la suma y el máximo de ese
rango. La raíz cubre todos los ticks, sus dos hijos cubren cada mitad, y así hasta las hojas, que
cubren un tick cada una.

**Representación:** como el montículo, vive en arreglos y no en nodos con referencias. Se toma
**m = la menor potencia de 2 ≥ n** (con n = 1000, m = 1024) y dos arreglos de tamaño 2m, uno para las
sumas y otro para los máximos:

- el nodo 1 es la raíz; los hijos del nodo i son 2i y 2i+1, y su padre es i/2;
- el tick p está en la hoja m + p.

**Ejemplo con 8 ticks** (m = 8). Los fallos por tick son `[0, 3, 0, 1, 2, 0, 6, 1]`:

```
                      nodo 1  [0..7]  suma 13  máx 6
               /                                      \
     nodo 2 [0..3] s 4 m 3                    nodo 3 [4..7] s 9 m 6
       /              \                         /              \
 nodo 4 [0..1]   nodo 5 [2..3]           nodo 6 [4..5]    nodo 7 [6..7]
   s 3  m 3        s 1  m 1                s 2  m 2         s 7  m 6
  /     \         /     \                 /     \          /      \
 0       3       0       1               2       0        6        1     ← hojas (nodos 8 a 15)
```

**Actualizar el tick p** (al terminar cada tick): se escribe la hoja m + p y se sube hasta la raíz
recalculando cada padre: `suma = izquierdo + derecho`, `máx = max(izquierdo, derecho)`. Un paso por
nivel: **O(log n)**.

**Consultar [a, b]**: se baja desde la raíz con tres casos por nodo:

1. el nodo queda **fuera** del rango → aporta 0 a la suma (y nada al máximo);
2. el nodo queda **dentro** del rango → se usa su valor guardado, **sin bajar más**;
3. el rango **corta** el nodo → se baja a los dos hijos y se combinan sus resultados.

Para la ventana [2, 6] del ejemplo solo se usan tres nodos guardados: el nodo 5 ([2..3], suma 1), el
nodo 6 ([4..5], suma 2) y la hoja del tick 6 (suma 6). **Total 9 y pico 6**, sin leer los ticks uno
por uno. En cada nivel, a lo sumo dos nodos caen en el caso 3, así que la consulta visita
**O(log n)** nodos: con 1000 ticks y 10 niveles, unas decenas de nodos en vez de 1000 ticks.

**Leer un tick** es leer su hoja: O(1). Así sale la serie que dibuja la gráfica.

### Por qué guarda el máximo: el árbol frente a las sumas prefijas

Este es el punto que hay que saber defender. Los fallos **siempre** se registran en el tick actual,
nunca en el pasado. Si solo se pidieran **totales**, bastaría un arreglo de **sumas prefijas**
(`prefijo[i]` = suma de los ticks 0 a i−1): agregar el tick actual es O(1), y la suma de [a, b] es
`prefijo[b+1] − prefijo[a]`, también O(1). Sería **mejor** que el árbol.

Lo que las sumas prefijas no pueden responder es el **máximo** de un rango arbitrario: el máximo no
se puede "restar". Saber el pico de [0, b] y el de [0, a−1] no dice nada del pico de [a, b]. **El
árbol de segmentos se justifica por el pico.** Si algún día el historial solo diera totales, sobraría.

### Detalles de implementación

- Las sumas son `long`: 1000 ticks de gasto pueden pasar el límite de un `int`.
- Las hojas de relleno (ticks 1000 a 1023) quedan en 0, y ninguna consulta válida las incluye.
- El valor de un tick incluye todo lo que pasó desde el registro anterior, también lo que el jugador
  hizo entre dos ticks (un reemplazo, un overhaul despachado a mano). Por eso el acumulador se pone
  en 0 **al registrar**, no al empezar el tick.
- El global guarda en cada tick **la suma** de las compañías: su pico es el peor tick de toda la
  organización, no el peor de una sola.

---

## 17. Cómo interactúan las estructuras en un tick de la organización

`Organizacion.tick()`:

1. **Recorre las compañías** en el orden del `ArrayList` y ejecuta el tick de cada una (los pasos de
   la sección 7). Cuando una compañía clasifica un fallo P1–P3, `RankingFallos.sumarFallo` lo saca y
   lo vuelve a meter en **sus dos árboles**.
2. **Avanza los overhauls:** a cada técnico del fabricante ocupado se le resta un tick. Al terminar,
   la máquina vuelve con puntaje 0 (sale y vuelve a entrar a los dos rankings) y el técnico vuelve
   al final de su **cola FIFO**.
3. **Asigna overhauls** en automático, con la **regla de la cima** del **montículo**.
4. **Registra el tick** en los **árboles de segmentos** de cada compañía y en los globales.
5. Incrementa el tick. A los 1000 ticks la partida termina.

### El descarte perezoso

Hay situaciones en las que un elemento que espera en una estructura **deja de tener sentido**, pero
no es el siguiente en salir:

- una orden **P4** (preventivo) de una máquina que ya recibió un overhaul, que es un servicio
  completo;
- una **solicitud de overhaul** de una compañía en quiebra, o de una máquina que fue reemplazada;
- un **comando de la pila** de deshacer sobre una máquina que ya fue reemplazada.

Sacarlos del medio violaría el tipo abstracto (y costaría O(n)). En cambio, **se quedan donde están**
y se descartan **cuando llegan a la salida**: la P4 cuando sale de la cola con prioridad, la
solicitud cuando llega a la cima del montículo, el comando cuando se intenta deshacer. La bitácora
registra cada descarte ("OT-12 PRENSA-01 descartada: PRENSA-01 ya recibió overhaul o fue
reemplazada").

Es una técnica conocida con colas de prioridad: el algoritmo de Dijkstra la usa con los vértices que
ya se procesaron. Para reconocer lo obsoleto no se busca nada: se miran **marcas** de la máquina en
O(1) (la P4 pendiente, la solicitud pendiente). Una máquina reemplazada no tiene las marcas de la
vieja, y así se reconoce lo que era de la anterior.

---

## 18. Pruebas de la Entrega 2

El proyecto tiene **156 pruebas** en verde (`./mvnw test`): las de la Entrega 1 que siguen vigentes
(se retiraron las de la interfaz Swing), las del modelo nuevo y las de la API.

| Clase de prueba | Qué respalda |
|---|---|
| `ArbolSegmentosTest` | Los cuatro casos exigidos: el rango completo, un rango de un elemento, un máximo a mitad del rango y una actualización |
| `RankingFallosTest` | Un fallo lleva la máquina al primer lugar; dos máquinas con el mismo puntaje se quedan las dos (el desempate); la consulta por umbral |
| `SolicitudOverhaulTest` | El mayor puntaje sale primero y, en un empate, la que llegó antes |
| `OrganizacionTest` | La misma semilla repite la simulación; el ranking global coincide con **ordenar todas las máquinas a mano** después de cientos de ticks; las solicitudes salen por puntaje aunque se aprueben al revés; la regla de la cima; el ciclo del overhaul; el reemplazo y sus descartes; el historial contra un cálculo directo |
| `PlantaTest` | Además de las de la Entrega 1: cada fallo pide la especialidad de su sensor, la orden espera a su especialidad, la penalidad duplica la duración, la economía y la quiebra |
| `Controlador...Test` (MockMvc) | Cada endpoint, con sus errores `400`, `404` y `409` |

La prueba del ranking contra el ordenamiento a mano es la que demuestra que **la trampa de la clave
que cambia no ocurre**: si un puntaje cambiara con la máquina adentro, el recorrido del árbol dejaría
de coincidir con la lista ordenada.

---

## 19. Tabla resumen de complejidades de la Entrega 2

n = máquinas, c = compañías, s = solicitudes de overhaul, T = ticks de la partida (1000),
k = elementos devueltos.

| Estructura | Insertar | Extraer o eliminar | Consultar | Recorrer |
|---|---|---|---|---|
| `ArrayList` (compañías) | `add`: **O(1) amortizado** | No se usa | `get(id)`: **O(1)** | O(c) |
| `TreeSet<String>` (nombres) | `add`: **O(log n)** | No se usa | `contains`: **O(log n)** | — |
| `TreeSet<Maquina>` (ranking) | **O(log n)** | `remove`: **O(log n)** | Puntaje ≥ umbral: **O(log n + k)** | En orden: **O(n)** |
| `PriorityQueue` (overhauls) | `add`: **O(log s)** | `poll`: **O(log s)** | `peek`: **O(1)**; orden de salida: O(s log s) sobre una copia | — |
| Colas por especialidad | `add`: **O(1) amortizado** | `poll`: **O(1)** | Elegir la cola: **O(1)** | O(n) |
| `ArbolSegmentos` (historial) | Actualizar un tick: **O(log T)** | — | Suma o máximo de [a, b]: **O(log T)**; un tick: **O(1)** | Serie de la ventana: O(k) |

**Lo que cambió respecto a la primera parte.** En la Entrega 1 casi todo era O(1), porque las
estructuras lineales solo tocan sus extremos. Los árboles cambian O(1) por **O(log n)** a cambio de
algo que una estructura lineal no da: **el orden siempre listo** (ranking), **el mejor elemento sin
ordenar todo** (montículo) y **resúmenes de cualquier rango sin recorrerlo** (árbol de segmentos).
