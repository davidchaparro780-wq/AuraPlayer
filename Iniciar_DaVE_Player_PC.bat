@echo off
title DaVE Player Desktop
cd /d "%~dp0\desktop\src"

set HTML_PATH=%cd%\index.html

:: Try launching in Edge App Mode (Chromeless window)
where msedge >nul 2>nul
if %errorlevel% equ 0 (
    start msedge --app="file:///%HTML_PATH:\=/%" --window-size=1200,800
    exit
)

:: Try launching in Chrome App Mode
where chrome >nul 2>nul
if %errorlevel% equ 0 (
    start chrome --app="file:///%HTML_PATH:\=/%" --window-size=1200,800
    exit
)

:: Fallback to default browser
start "" "%HTML_PATH%"
exit
