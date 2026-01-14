#!/bin/bash

# Script de deploy para Docker Swarm com as correções do JasperReports
# Este script facilita o deploy da nova versão com pre-compilação de relatórios

echo "🚀 Iniciando deploy da versão corrigida do JasperReports..."

# Criar diretório temporário persistente no host
sudo mkdir -p /tmp/jasperreports
sudo chmod 777 /tmp/jasperreports

echo "✅ Diretório temporário criado: /tmp/jasperreports"

# Deploy do stack Docker Swarm
echo "📦 Realizando deploy do stack Docker Swarm..."
docker stack deploy -c docker-stack-v2.yml api-report-jaspersoft

echo "⏳ Aguardando serviço iniciar..."
sleep 30

# Verificar status do serviço
echo "🔍 Verificando status do serviço..."
docker service ls | grep api-report-jaspersoft

echo "✅ Deploy concluído!"
echo ""
echo "📋 Próximos passos:"
echo "1. Aguarde 2-3 minutos para o serviço iniciar completamente"
echo "2. Teste o relatório com: ./testar-relatorio.sh"
echo "3. Monitore os logs com: docker service logs -f api-report-jaspersoft_api-comercial"
echo ""
echo "🔗 URLs importantes:"
echo "- Aplicação: http://hjaspersoft.verderp.com.br"
echo "- Health Check: http://hjaspersoft.verderp.com.br/actuator/health"