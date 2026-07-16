@echo off
title Invest Sentinel - Inicializacao Docker
echo ===================================================
echo      Iniciando Invest Sentinel via Docker
echo ===================================================
echo.

if not exist .env (
    copy .env.example .env
    echo [AVISO] Arquivo .env criado a partir do exemplo. Preencha suas chaves caso necessario.
)

echo Construindo e subindo os containers...
docker compose up --build

echo.
echo Pressione qualquer tecla para encerrar os containers...
pause
docker compose down