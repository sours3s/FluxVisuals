@echo off
title FluxVisuals AuthServer
echo ==========================================
echo  FluxVisuals AuthServer (Windows)
echo  http://0.0.0.0:5001
echo ==========================================
cd /d %~dp0
AuthServer.exe
pause
