#!/bin/bash

# Script de Deploy Automatizado para verdu-erp-reports
# Este script aplica todas as correções necessárias para resolver o erro 500

echo "🚀 Iniciando deploy automatizado do verdu-erp-reports..."

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Função para imprimir mensagens
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Verificar se o Maven está instalado
if ! command -v mvn &> /dev/null; then
    print_error "Maven não encontrado. Por favor, instale o Maven primeiro."
    exit 1
fi

# Verificar se o Docker está instalado
if ! command -v docker &> /dev/null; then
    print_error "Docker não encontrado. Por favor, instale o Docker primeiro."
    exit 1
fi

# Parar container antigo se existir
echo "📦 Parando container antigo..."
if docker ps -a | grep -q verdu-erp-reports; then
    docker stop verdu-erp-reports
    docker rm verdu-erp-reports
    print_success "Container antigo removido"
else
    print_warning "Nenhum container antigo encontrado"
fi

# Build da aplicação
echo "🔨 Fazendo build da aplicação..."
if mvn clean package -DskipTests; then
    print_success "Build realizado com sucesso"
else
    print_error "Erro ao fazer build da aplicação"
    exit 1
fi

# Construir imagem Docker
echo "🐳 Construindo imagem Docker..."
if docker build -t verdu-erp-reports .; then
    print_success "Imagem Docker criada com sucesso"
else
    print_error "Erro ao criar imagem Docker"
    exit 1
fi

# Iniciar novo container com configurações otimizadas
echo "🚀 Iniciando novo container..."
if docker run -d \
  --name verdu-erp-reports \
  -p 8015:8015 \
  -e SPRING_PROFILES_ACTIVE=homolog \
  -e JAVA_OPTS="-Djava.awt.headless=true -Djava.io.tmpdir=/tmp -Dnet.sf.jasperreports.compiler.class=net.sf.jasperreports.engine.design.JRJdtCompiler -Dnet.sf.jasperreports.compiler.cache.temp.files=false -Dnet.sf.jasperreports.compiler.java=false -Dnet.sf.jasperreports.compiler.expression.class=false -Dnet.sf.jasperreports.compiler.expression.groovy=false -Dnet.sf.jasperreports.compiler.expression.javascript=false" \
  verdu-erp-reports; then
    print_success "Container iniciado com sucesso"
else
    print_error "Erro ao iniciar container"
    exit 1
fi

# Aguardar container iniciar completamente
echo "⏳ Aguardando container iniciar..."
sleep 10

# Verificar se o container está rodando
if docker ps | grep -q verdu-erp-reports; then
    print_success "Container está rodando"
else
    print_error "Container não está rodando"
    echo "📋 Logs do container:"
    docker logs verdu-erp-reports
    exit 1
fi

# Verificar logs do container
echo "📋 Verificando logs do container..."
if docker logs verdu-erp-reports | grep -q "Started"; then
    print_success "Aplicação iniciou corretamente"
else
    print_warning "Possível problema na inicialização"
fi

# Testar geração de relatório
echo "🧪 Testando geração de relatório..."
sleep 5

RESPONSE=$(curl -s -X POST http://localhost:8015/api/reports/generate \
  -H "Content-Type: application/json" \
  -d '{"reportName":"separacaodecarga.jrxml","format":"pdf","parameters":{"ROTA_ID":5}}' \
  -w "%{http_code}" \
  -o /dev/null)

if [ "$RESPONSE" = "200" ]; then
    print_success "✅ Relatório gerado com sucesso!"
elif [ "$RESPONSE" = "500" ]; then
    print_error "❌ Erro 500 ao gerar relatório"
    echo "📋 Verificando logs detalhados..."
    docker logs verdu-erp-reports --tail 50
else
    print_warning "⚠️  Resposta inesperada: $RESPONSE"
fi

echo ""
echo "🎉 Deploy concluído!"
echo "📊 Status do container:"
docker ps | grep verdu-erp-reports
echo ""
echo "🔗 URL do serviço: http://localhost:8015"
echo "📁 Logs: docker logs -f verdu-erp-reports"