#!/bin/bash

# Script de teste para validar as correções do JasperReports
# Este script testa a geração do relatório separacaodecarga.jrxml com ROTA_ID=5

echo "🧪 Testando geração de relatório com correções..."

# URL do servidor (substitua pelo IP do seu servidor Docker Swarm)
SERVER_URL="http://hjaspersoft.verderp.com.br"
# Ou use o IP direto se preferir:
# SERVER_URL="http://10.200.0.50:8013"

echo "📡 Conectando ao servidor: $SERVER_URL"

# Testar health check primeiro
echo "🔍 Verificando health check..."
HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$SERVER_URL/actuator/health")

if [ "$HEALTH_RESPONSE" = "200" ]; then
    echo "✅ Servidor está saudável"
else
    echo "⚠️  Health check retornou: $HEALTH_RESPONSE"
fi

# Testar geração do relatório
echo "📊 Testando geração do relatório separacaodecarga.jrxml..."

echo "📝 Payload: {\"reportName\":\"separacaodecarga.jrxml\",\"format\":\"pdf\",\"parameters\":{\"ROTA_ID\":5}}"

RESPONSE=$(curl -s -X POST "$SERVER_URL/api/reports/generate" \
  -H "Content-Type: application/json" \
  -d '{"reportName":"separacaodecarga.jrxml","format":"pdf","parameters":{"ROTA_ID":5}}' \
  -w "\nHTTP_STATUS:%{http_code}" \
  -o /tmp/relatorio_teste.pdf)

# Extrair status HTTP
HTTP_STATUS=$(echo "$RESPONSE" | grep "HTTP_STATUS:" | cut -d: -f2)

# Verificar resultado
if [ "$HTTP_STATUS" = "200" ]; then
    echo "✅ SUCESSO! Relatório gerado com sucesso!"
    echo "📄 PDF salvo em: /tmp/relatorio_teste.pdf"
    echo "📊 Tamanho do arquivo: $(ls -lh /tmp/relatorio_teste.pdf | awk '{print $5}')"
elif [ "$HTTP_STATUS" = "500" ]; then
    echo "❌ ERRO 500 - Falha na geração do relatório"
    echo "📋 Verificando logs do servidor..."
    
    # Tentar pegar logs do container (se tiver acesso)
    echo "🔍 Logs recentes do servidor:"
    curl -s "$SERVER_URL/actuator/logfile" | tail -50 || echo "Não foi possível acessar logs via actuator"
    
else
    echo "⚠️  Resposta inesperada: HTTP $HTTP_STATUS"
    echo "📋 Resposta completa: $RESPONSE"
fi

echo ""
echo "🎯 Teste concluído!"
echo ""
echo "Se ainda houver erro 500, verifique:"
echo "1. 📝 As variáveis de ambiente do compilador JasperReports estão configuradas?"
echo "2. 📁 Os volumes de fontes estão montados corretamente?"
echo "3. 🔧 O profile 'homolog' está ativo?"
echo "4. 📋 Os logs do container para mais detalhes"
echo ""
echo "Para ver logs no servidor Docker Swarm:"
echo "docker service logs api-comercial_api-comercial --tail 100"