@echo off
title FluxVisuals AuthServer
echo Starting FluxVisuals AuthServer on http://0.0.0.0:5001
cd /d %~dp0
AuthServer.exe --urls "http://0.0.0.0:5001"
pause
