@echo off
rem Doble clic para levantar el backend y el frontend en Windows: llama a levantar.ps1.
rem Al pulsar Ctrl+C, Windows puede preguntar si termina el trabajo por lotes: cualquier respuesta sirve,
rem porque levantar.ps1 ya detuvo los dos servidores.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0levantar.ps1" %*
rem Si algo falla, la ventana espera una tecla para que se pueda leer el mensaje.
if errorlevel 1 pause
