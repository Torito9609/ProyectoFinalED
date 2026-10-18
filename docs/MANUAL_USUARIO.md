# Manual de usuario

> **Estado:** pendiente. Se escribe cuando la interfaz esté terminada (etapa de vista y
> controlador). Este archivo contiene el índice acordado para no dejar nada por fuera.

---

## 1. Requisitos e instalación

- JDK necesario y cómo verificarlo.
- Cómo obtener el proyecto y construirlo (`./mvnw package`).
- Cómo ejecutarlo (`java -jar target/simulador-planta.jar`) y desde IntelliJ IDEA.

## 2. La ventana principal

- Captura de pantalla completa con las zonas numeradas.
- Qué significa cada panel y a qué estructura de datos corresponde.

## 3. Controles de la simulación

- **Iniciar / Pausar:** arranca y detiene el reloj.
- **Paso:** avanza un solo tick, para observar con calma qué cambió.
- **Reiniciar:** vuelve al estado inicial con la misma semilla.
- Qué es la semilla y por qué la misma semilla repite exactamente la misma simulación.

## 4. Lectura de cada panel

- **Inventario:** una celda por índice; qué significa cada color de estado.
- **Cola circular:** índices, punteros de frente y final, zonas de color del sensor y contador de
  sobrescrituras. Qué se ve cuando el final da la vuelta.
- **Cola de eventos:** por dónde entran y por dónde salen los eventos.
- **Cola de técnicos:** técnicos libres en fila y técnicos trabajando con sus ticks restantes.
- **Cola con prioridad:** los cuatro carriles P1–P4 y cómo se resalta el siguiente en salir.
- **Pila de deshacer:** la cima arriba.
- **Bitácora:** cómo leer una línea de registro.

## 5. Acciones del operador

- Seleccionar una máquina.
- Cambiar la carga y su efecto sobre la probabilidad de fallo.
- Parar y arrancar una máquina manualmente.
- Inyectar un fallo: elegir sensor y magnitud.
- Deshacer: qué acciones se pueden deshacer y por qué las demás no.

## 6. Guía para la demostración

Secuencia sugerida de pasos para ver cada estructura en acción, incluida la generación de una
alarma P1 y su atención por un técnico.

## 7. Preguntas frecuentes

- Por qué una máquina no genera una segunda alarma mientras tiene una activa.
- Por qué solo se clasifica un evento por tick.
- Qué pasa si se deshace una acción que la simulación ya modificó por su cuenta.
