@echo off
title DaVE Player — Sincronizador de Musica del Celular
chcp 65001 >nul
cls

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0sincronizar_celular.ps1"

echo.
echo Presiona cualquier tecla para salir...
pause >nul
