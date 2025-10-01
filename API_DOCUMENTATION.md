# Documentação das APIs - Servidor de Relatórios

Este documento descreve todas as APIs disponíveis no servidor de relatórios integrado com JasperReports e MinIO.

## Configuração Base

**Base URL:** `http://localhost:8080/api/reports`

## Endpoints Disponíveis

### 1. Importar Relatório

**POST** `/import`

Faz upload de um arquivo de relatório (.jasper ou .jrxml) para o bucket MinIO.

#### Parâmetros
- `file` (multipart/form-data) - Arquivo do relatório (.jasper ou .jrxml)
- `reportName` (form-data) - Nome do relatório no bucket

#### Exemplo de Requisição
```bash
curl -X POST \
  http://localhost:8080/api/reports/import \
  -F 'file=@relatorio_vendas.jrxml' \
  -F 'reportName=relatorio_vendas.jrxml'
```

#### Resposta de Sucesso (200)
```json
{
  "message": "Relatório enviado com sucesso",
  "reportName": "relatorio_vendas.jrxml",
  "success": true
}
```

#### Resposta de Erro (400/500)
```json
{
  "message": "Arquivo deve ser .jasper ou .jrxml",
  "reportName": null,
  "success": false
}
```

---

### 2. Gerar Relatório (Método 1 - Path + Query)

**POST** `/generate/{reportName}`

Gera um relatório em formato específico com parâmetros dinâmicos.

#### Parâmetros
- `reportName` (path) - Nome do relatório no bucket
- `format` (query, opcional) - Formato de saída (pdf, html, csv, xml, xlsx). Padrão: pdf
- `parameters` (body JSON, opcional) - Parâmetros dinâmicos do relatório

#### Exemplo de Requisição
```bash
curl -X POST \
  'http://localhost:8080/api/reports/generate/relatorio_vendas.jrxml?format=pdf' \
  -H 'Content-Type: application/json' \
  -d '{
    "dataInicio": "2024-01-01",
    "dataFim": "2024-12-31",
    "vendedorId": 123
  }'
```

#### Resposta
Retorna o arquivo binário do relatório com headers apropriados:
- `Content-Type`: Baseado no formato (application/pdf, text/html, etc.)
- `Content-Disposition`: attachment; filename="relatorio_vendas.pdf"

---

### 3. Gerar Relatório (Método 2 - DTO)

**POST** `/generate`

Gera um relatório usando um DTO estruturado.

#### Parâmetros (Body JSON)
```json
{
  "reportName": "string",
  "format": "string",
  "parameters": {
    "chave1": "valor1",
    "chave2": "valor2"
  }
}
```

#### Exemplo de Requisição
```bash
curl -X POST \
  http://localhost:8080/api/reports/generate \
  -H 'Content-Type: application/json' \
  -d '{
    "reportName": "relatorio_vendas.jrxml",
    "format": "xlsx",
    "parameters": {
      "dataInicio": "2024-01-01",
      "dataFim": "2024-12-31",
      "vendedorId": 123,
      "incluirDetalhes": true
    }
  }'
```

---

### 4. Listar Relatórios

**GET** `/list`

Retorna a lista de todos os relatórios disponíveis no bucket.

#### Exemplo de Requisição
```bash
curl -X GET http://localhost:8080/api/reports/list
```

#### Resposta de Sucesso (200)
```json
[
  {
    "name": "relatorio_vendas.jrxml",
    "size": 15420,
    "lastModified": "2024-01-15T10:30:00Z",
    "etag": "d41d8cd98f00b204e9800998ecf8427e"
  },
  {
    "name": "relatorio_estoque.jasper",
    "size": 8932,
    "lastModified": "2024-01-10T14:20:00Z",
    "etag": "098f6bcd4621d373cade4e832627b4f6"
  }
]
```

---

### 5. Remover Relatório

**DELETE** `/{reportName}`

Remove um relatório do bucket.

#### Parâmetros
- `reportName` (path) - Nome do relatório a ser removido

#### Exemplo de Requisição
```bash
curl -X DELETE http://localhost:8080/api/reports/relatorio_vendas.jrxml
```

#### Resposta de Sucesso (200)
```
Relatório removido com sucesso
```

---

## Formatos Suportados

| Formato | Extensão | Content-Type |
|---------|----------|-------------|
| PDF | .pdf | application/pdf |
| HTML | .html | text/html |
| CSV | .csv | text/csv |
| XML | .xml | application/xml |
| Excel | .xlsx | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet |

---

## Parâmetros Dinâmicos

O sistema suporta parâmetros completamente dinâmicos. Você pode passar qualquer parâmetro no objeto `parameters`, e eles serão repassados diretamente para o JasperReports.

### Tipos de Parâmetros Suportados
- **String**: Textos simples
- **Number**: Inteiros e decimais
- **Boolean**: true/false
- **Date**: Strings no formato ISO (serão convertidas automaticamente)

### Exemplo de Parâmetros Complexos
```json
{
  "reportName": "relatorio_completo.jrxml",
  "format": "pdf",
  "parameters": {
    "titulo": "Relatório de Vendas 2024",
    "dataInicio": "2024-01-01",
    "dataFim": "2024-12-31",
    "vendedorId": 123,
    "incluirGraficos": true,
    "valorMinimo": 1000.50,
    "regiao": "Sul",
    "ativo": true
  }
}
```

---

## Códigos de Status HTTP

| Código | Descrição |
|--------|----------|
| 200 | Sucesso |
| 400 | Requisição inválida (parâmetros incorretos) |
| 404 | Relatório não encontrado |
| 413 | Arquivo muito grande (limite: 50MB) |
| 500 | Erro interno do servidor |

---

## Configuração do MinIO

O sistema utiliza as seguintes configurações do MinIO (definidas em `application.properties`):

```properties
minio.endpoint=http://192.168.8.80:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin
```

**Bucket utilizado:** `relatorios`

---

## Configuração do Banco de Dados

Os relatórios podem acessar dados do PostgreSQL configurado:

```properties
spring.datasource.url=jdbc:postgresql://192.168.8.80:5432/VERDU_ERP
spring.datasource.username=postgres
spring.datasource.password=Solnascente@2025.
```

---

## Exemplos de Uso Completos

### 1. Workflow Completo - Upload e Geração

```bash
# 1. Upload do relatório
curl -X POST \
  http://localhost:8080/api/reports/import \
  -F 'file=@meu_relatorio.jrxml' \
  -F 'reportName=meu_relatorio.jrxml'

# 2. Listar relatórios para confirmar
curl -X GET http://localhost:8080/api/reports/list

# 3. Gerar relatório em PDF
curl -X POST \
  'http://localhost:8080/api/reports/generate/meu_relatorio.jrxml?format=pdf' \
  -H 'Content-Type: application/json' \
  -d '{
    "parametro1": "valor1",
    "parametro2": 123
  }' \
  --output relatorio_gerado.pdf
```

### 2. Geração em Diferentes Formatos

```bash
# PDF
curl -X POST 'http://localhost:8080/api/reports/generate/relatorio.jrxml?format=pdf' \
  -H 'Content-Type: application/json' -d '{}' --output relatorio.pdf

# Excel
curl -X POST 'http://localhost:8080/api/reports/generate/relatorio.jrxml?format=xlsx' \
  -H 'Content-Type: application/json' -d '{}' --output relatorio.xlsx

# CSV
curl -X POST 'http://localhost:8080/api/reports/generate/relatorio.jrxml?format=csv' \
  -H 'Content-Type: application/json' -d '{}' --output relatorio.csv
```

---

## Tratamento de Erros

O sistema possui tratamento global de exceções que retorna respostas estruturadas:

### Erro de Arquivo Muito Grande
```json
{
  "message": "Arquivo muito grande. Tamanho máximo permitido excedido.",
  "reportName": null,
  "success": false
}
```

### Erro de Formato Não Suportado
```json
{
  "error": "Argumento inválido",
  "message": "Formato não suportado: xyz"
}
```

### Erro Interno
```json
{
  "error": "Erro interno do servidor",
  "message": "Detalhes do erro..."
}
```

---

## Notas Importantes

1. **Tamanho máximo de arquivo**: 50MB
2. **Formatos aceitos para upload**: .jasper e .jrxml
3. **Bucket criado automaticamente**: O bucket "relatorios" é criado automaticamente se não existir
4. **Parâmetros opcionais**: Todos os parâmetros são opcionais - se não fornecidos, o relatório será gerado sem parâmetros
5. **Conexão com banco**: Os relatórios têm acesso automático à conexão do banco de dados configurado
6. **CORS habilitado**: As APIs aceitam requisições de qualquer origem

---

## Suporte

Para dúvidas ou problemas, verifique:
1. Se o MinIO está rodando e acessível
2. Se o banco PostgreSQL está conectado
3. Se o arquivo de relatório está no formato correto
4. Se os parâmetros estão sendo passados corretamente