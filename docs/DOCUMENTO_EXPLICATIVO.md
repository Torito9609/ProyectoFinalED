# Por qué y cómo se usó cada estructura de datos

> **Estado:** pendiente. Se redacta a medida que se implementa cada estructura, para que lo escrito
> corresponda al código real. Este archivo contiene el índice acordado.

---

## 1. Introducción

- Qué problema modela el simulador y por qué un problema de monitoreo industrial se presta para
  ejercitar estructuras lineales.
- Resumen de las cinco estructuras usadas.

## 2. Arreglo dinámico — inventario de máquinas

- Implementación: `ArrayList<Maquina>`.
- Por qué: la operación más frecuente es recorrer todas las máquinas en cada tick y acceder por
  índice, ambas O(1). Las máquinas nunca se eliminan del inventario, solo cambian de estado.
- Complejidad de cada operación usada, y por qué agregar al final es O(1) *amortizado* y no O(1)
  puro (al llenarse, crea un arreglo 1,5 veces más grande y copia los elementos).

## 3. Cola circular — últimas N lecturas de cada máquina

- Implementación: **propia**, `ColaCircular<T>`.
- Por qué propia: Java no ofrece ninguna cola que **sobrescriba** el elemento más antiguo al
  llenarse. Las de `java.util` crecen, y `ArrayBlockingQueue` rechaza elementos cuando está llena,
  además de estar pensada para concurrencia.
- Por qué el dominio la exige: así funciona un registrador de datos real, con memoria fija.
- Cómo funciona: arreglo de capacidad fija, puntero al frente, tamaño, y el final calculado como
  `(frente + tamaño) % capacidad`. Encolar, desencolar y consultar el frente son O(1).
- Cómo se ve en la interfaz.

## 4. Cola FIFO — eventos por clasificar y técnicos disponibles

- Implementación: `ArrayDeque`, declarada como `Queue`.
- Por qué: el orden de llegada debe respetarse estrictamente; encolar y desencolar son O(1).
- Por qué `ArrayDeque` y no `LinkedList`: la lista enlazada crea un objeto nodo por elemento.
- Por qué los técnicos también forman una cola: al terminar un trabajo, el técnico se encola al
  final, de modo que la carga se reparte de forma justa.

## 5. Cola con prioridad — órdenes de trabajo P1 a P4

- Implementación: arreglo de 4 `ArrayDeque<OrdenTrabajo>`, con el índice igual al nivel de
  prioridad.
- Por qué: insertar es O(1) y extraer recorre 4 niveles fijos, es decir O(k) con k = 4, constante en
  la práctica. El orden de llegada dentro de cada prioridad se cumple solo, porque cada nivel es una
  cola FIFO.
- Por qué **no** `PriorityQueue`: internamente es un heap (estructura no lineal, fuera del alcance de
  esta entrega) y, sobre todo, **no garantiza el orden de llegada** entre elementos de igual
  prioridad.
- Por qué no un arreglo ordenado: insertar costaría O(n) por el desplazamiento de elementos.

## 6. Pila — deshacer acciones del operador

- Implementación: `ArrayDeque` con `push` y `pop`.
- Por qué: la última acción es la primera en revertirse (LIFO), en O(1).
- Por qué **no** `Stack`: hereda de `Vector`, sincroniza todos sus métodos (costo innecesario en un
  programa de un solo hilo) y permite acceso por índice, lo que rompe el concepto de pila. La propia
  documentación de Java recomienda usar `Deque`.
- **Qué acciones no se pueden deshacer y por qué:** clasificar, asignar e inyectar un fallo.
  Revertirlas obligaría a reinsertar un elemento por el frente de una cola FIFO, o a sacar de la cola
  con prioridad un elemento que no es el siguiente. Ambas cosas violan el tipo abstracto de datos, y
  se prefirió respetarlo antes que ofrecer la funcionalidad.

## 7. Cómo interactúan las estructuras en un tick

- Recorrido de los ocho pasos de la simulación, señalando qué estructura se toca en cada uno.
- Por qué `EventoFallo` y `OrdenTrabajo` son clases distintas: refleja la separación industrial real
  entre detección (PLC), clasificación (SCADA) y gestión de mantenimiento, y es lo que justifica que
  existan dos colas separadas.

## 8. Pruebas

- Casos cubiertos para `ColaCircular`: cola vacía, llenado exacto, sobrescritura, varias vueltas
  completas, desencolar después de dar la vuelta, capacidad 1.
- Casos cubiertos para la cola con prioridad: orden por prioridad, FIFO dentro del mismo nivel, una
  P1 insertada de última que sale primero, extraer con la estructura vacía.
- Prueba de la simulación sin interfaz gráfica.

## 9. Tabla resumen de complejidades

| Estructura | Insertar | Extraer | Consultar | Recorrer |
|---|---|---|---|---|
| `ArrayList` | | | | |
| `ColaCircular` | | | | |
| Cola FIFO | | | | |
| Cola con prioridad | | | | |
| Pila | | | | |
