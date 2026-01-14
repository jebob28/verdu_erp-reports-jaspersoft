#!/bin/bash

# Script para pré-compilar relatórios JasperReports (.jrxml -> .jasper)
# Isso elimina a necessidade de compilação em tempo de execução

echo "🔧 Pré-compilando relatórios JasperReports..."

# Criar diretório para relatórios compilados
mkdir -p compiled-reports

# Função para compilar um relatório
compile_report() {
    local jrxml_file=$1
    local output_name=$2
    
    echo "📄 Compilando: $jrxml_file -> $output_name.jasper"
    
    java -cp "target/verdu_erp-0.0.1-SNAPSHOT.jar:target/lib/*" \
         net.sf.jasperreports.engine.JasperCompileManager \
         "$jrxml_file" \
         "compiled-reports/$output_name.jasper"
    
    if [ $? -eq 0 ]; then
        echo "✅ Sucesso: $output_name.jasper"
    else
        echo "❌ Erro ao compilar: $jrxml_file"
        return 1
    fi
}

# Compilar o relatório que está dando problema
echo "📋 Compilando separacaodecarga.jrxml..."
compile_report "src/main/resources/relatorios/logistica/separacaodecarga.jrxml" "separacaodecarga"

echo "✨ Pré-compilação concluída!"
echo "📁 Arquivos .jasper criados em: compiled-reports/"
ls -la compiled-reports/