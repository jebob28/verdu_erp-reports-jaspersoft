#!/bin/bash

# Script de compilação de relatórios usando o utilitário Java
# Compila arquivos .jrxml para .jasper para eliminar compilação em tempo de execução

echo "🔧 Pré-compilando relatórios JasperReports..."

# Criar diretório para relatórios compilados
mkdir -p compiled-reports

# Função para compilar um relatório
compile_report() {
    local jrxml_file=$1
    local output_name=$2
    
    echo "📄 Compilando: $jrxml_file -> $output_name.jasper"
    
    # Executar o compilador Java
    java -cp "target/classes:$(find ~/.m2/repository -name "jasperreports*.jar" | head -1)" \
         reports.verdu_erp.util.JasperCompiler \
         "$jrxml_file" \
         "compiled-reports/$output_name.jasper"
    
    if [ $? -eq 0 ]; then
        echo "✅ Relatório compilado com sucesso: $output_name.jasper"
    else
        echo "❌ Erro ao compilar: $jrxml_file"
        return 1
    fi
}

# Compilar o relatório separacaodecarga.jrxml
if [ -f "src/main/resources/relatorios/logistica/separacaodecarga.jrxml" ]; then
    compile_report "src/main/resources/relatorios/logistica/separacaodecarga.jrxml" "separacaodecarga"
else
    echo "⚠️ Arquivo não encontrado: src/main/resources/relatorios/logistica/separacaodecarga.jrxml"
fi

echo "✨ Pré-compilação concluída!"
echo "📁 Arquivos .jasper criados em: compiled-reports/"
ls -la compiled-reports/