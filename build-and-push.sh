#!/bin/bash

# Script para buildar e pushar imagem Docker com correções do JasperReports
# Este script deve ser executado antes de fazer deploy no Docker Swarm

echo "🔨 Build da imagem Docker com correções..."

# Build da aplicação
echo "📦 Fazendo build da aplicação..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Erro no build da aplicação"
    exit 1
fi

# Build da imagem Docker
echo "🐳 Build da imagem Docker..."
docker build -t ghcr.io/jebob28/report-jaspersoft:v2 .

if [ $? -ne 0 ]; then
    echo "❌ Erro no build da imagem Docker"
    exit 1
fi

# Push da imagem (descomente se quiser fazer push)
# echo "📤 Push da imagem..."
# docker push ghcr.io/jebob28/report-jaspersoft:v2

echo "✅ Imagem buildada com sucesso!"
echo "📋 Pronto para deploy no Docker Swarm"
echo ""
echo "Para fazer deploy, use:"
echo "docker stack deploy -c docker-stack-complete.yml api-comercial"
echo ""
echo "Ou atualize a tag no Portainer para: ghcr.io/jebob28/report-jaspersoft:v2"