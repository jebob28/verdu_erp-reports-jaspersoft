# Guia Completo de Uso da API de Relatórios

Este documento contém exemplos práticos de como usar todas as rotas disponíveis na API de Relatórios.

## Configuração Base

**Base URL:** `http://localhost:8081/api/reports`

**Nota:** Certifique-se de que o servidor esteja rodando na porta 8081 e que o MinIO esteja configurado corretamente.

---

## 1. Teste de Conectividade

### GET `/health`

Verifica se a API está funcionando corretamente.

#### Exemplo de Requisição
```bash
curl -X GET http://localhost:8081/api/reports/health
```

#### Resposta Esperada
```json
{
  "status": "OK",
  "message": "API de relatórios funcionando",
  "timestamp": "2024-01-15T10:30:00Z",
  "note": "Para usar as funcionalidades completas, configure o MinIO em localhost:9000"
}
```

---

## 2. Importar Relatório

### POST `/import`

Faz upload de um arquivo de relatório (.jasper ou .jrxml) para o sistema.

#### Parâmetros
- `file` (multipart/form-data) - Arquivo do relatório
- `reportName` (form-data) - Nome do relatório
- `codigo` (form-data, opcional) - Código único do relatório
- `descricao` (form-data, opcional) - Descrição do relatório
- `parametros` (form-data, opcional) - Parâmetros em formato JSON

#### Exemplo 1: Upload Simples
```bash
curl -X POST \
  http://localhost:8081/api/reports/import \
  -F 'file=@relatorio_vendas.jrxml' \
  -F 'reportName=relatorio_vendas.jrxml'
```

#### Exemplo 2: Upload com Parâmetros Completos
```bash
curl -X POST \
  http://localhost:8081/api/reports/import \
  -F 'file=@relatorio_vendas.jrxml' \
  -F 'reportName=relatorio_vendas.jrxml' \
  -F 'codigo=REL_VENDAS_001' \
  -F 'descricao=Relatório de vendas mensais' \
  -F 'parametros={"dataInicio": "date", "dataFim": "date", "vendedorId": "integer"}'
```

#### Exemplo 3: Upload com Parâmetros de Teste
```bash
curl -X POST \
  http://localhost:8081/api/reports/import \
  -F 'file=@teste_parametro.jrxml' \
  -F 'reportName=teste_parametro.jrxml' \
  -F 'codigo=REL_PARAM_001' \
  -F 'descricao=Relatório de teste com parâmetros' \
  -F 'parametros={"titulo": "string", "dataInicio": "date", "dataFim": "date"}'
```

#### Resposta de Sucesso
```json
{
  "message": "Relatório enviado com sucesso",
  "reportName": "relatorio_vendas.jrxml",
  "success": true
}
```

#### Resposta de Erro
```json
{
  "message": "Arquivo deve ser .jasper ou .jrxml",
  "reportName": null,
  "success": false
}
```

---

## 3. Gerar Relatório (Método 1)

### POST `/generate/{reportName}`

Gera um relatório usando o nome do arquivo como parâmetro da URL.

#### Parâmetros
- `reportName` (path) - Nome do relatório
- `format` (query, opcional) - Formato de saída (pdf, html, csv, xml, xlsx)
- `parameters` (body JSON, opcional) - Parâmetros dinâmicos

#### Exemplo 1: Gerar PDF sem Parâmetros
```bash
curl -X POST \
  'http://localhost:8081/api/reports/generate/relatorio_teste.jrxml?format=pdf' \
  -H 'Content-Type: application/json' \
  -o relatorio_output.pdf
```

#### Exemplo 2: Gerar PDF com Parâmetros
```bash
curl -X POST \
  'http://localhost:8081/api/reports/generate/relatorio_vendas.jrxml?format=pdf' \
  -H 'Content-Type: application/json' \
  -d '{
    "dataInicio": "2024-01-01",
    "dataFim": "2024-12-31",
    "vendedorId": 123
  }' \
  -o vendas_2024.pdf
```

#### Exemplo 3: Gerar Excel
```bash
curl -X POST \
  'http://localhost:8081/api/reports/generate/relatorio_vendas.jrxml?format=xlsx' \
  -H 'Content-Type: application/json' \
  -d '{
    "dataInicio": "2024-01-01",
    "dataFim": "2024-12-31",
    "vendedorId": 123,
    "incluirDetalhes": true
  }' \
  -o vendas_2024.xlsx
```

#### Exemplo 4: Gerar HTML
```bash
curl -X POST \
  'http://localhost:8081/api/reports/generate/relatorio_vendas.jrxml?format=html' \
  -H 'Content-Type: application/json' \
  -d '{
    "dataInicio": "2024-01-01",
    "dataFim": "2024-12-31"
  }' \
  -o vendas_2024.html
```

#### Exemplo 5: Gerar CSV
```bash
curl -X POST \
  'http://localhost:8081/api/reports/generate/relatorio_vendas.jrxml?format=csv' \
  -H 'Content-Type: application/json' \
  -d '{
    "dataInicio": "2024-01-01",
    "dataFim": "2024-12-31"
  }' \
  -o vendas_2024.csv
```

---

## 4. Gerar Relatório (Método 2 - DTO)

### POST `/generate`

Gera um relatório usando um DTO estruturado no corpo da requisição.

#### Estrutura do DTO
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

#### Exemplo 1: Relatório PDF com DTO
```bash
curl -X POST \
  http://localhost:8081/api/reports/generate \
  -H 'Content-Type: application/json' \
  -d '{
    "reportName": "relatorio_vendas.jrxml",
    "format": "pdf",
    "parameters": {
      "dataInicio": "2024-01-01",
      "dataFim": "2024-12-31",
      "vendedorId": 123
    }
  }' \
  -o vendas_dto.pdf
```

#### Exemplo 2: Relatório Excel com DTO
```bash
curl -X POST \
  http://localhost:8081/api/reports/generate \
  -H 'Content-Type: application/json' \
  -d '{
    "reportName": "relatorio_vendas.jrxml",
    "format": "xlsx",
    "parameters": {
      "dataInicio": "2024-01-01",
      "dataFim": "2024-12-31",
      "vendedorId": 123,
      "incluirDetalhes": true,
      "ordenarPor": "data"
    }
  }' \
  -o vendas_completo.xlsx
```

#### Exemplo 3: Relatório sem Parâmetros
```bash
curl -X POST \
  http://localhost:8081/api/reports/generate \
  -H 'Content-Type: application/json' \
  -d '{
    "reportName": "relatorio_simples.jrxml",
    "format": "pdf"
  }' \
  -o relatorio_simples.pdf
```

---

## 5. Listar Relatórios

### GET `/list`

Retorna a lista de todos os relatórios disponíveis no banco de dados.

#### Exemplo de Requisição
```bash
curl -X GET http://localhost:8081/api/reports/list
```

#### Resposta Esperada
```json
[
  {
    "name": "relatorio_vendas.jrxml",
    "size": 15420,
    "lastModified": "2024-01-15T10:30:00Z",
    "etag": "d41d8cd98f00b204e9800998ecf8427e"
  },
  {
    "name": "relatorio_teste.jrxml",
    "size": 8532,
    "lastModified": "2024-01-14T15:20:00Z",
    "etag": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
  }
]
```

---

## 6. Buscar Relatório por Código

### GET `/search/{code}`

Busca um relatório específico pelo código no banco de dados.

#### Exemplo de Requisição
```bash
curl -X GET http://localhost:8081/api/reports/search/REL_VENDAS_001
```

#### Resposta de Sucesso
```json
{
  "name": "relatorio_vendas.jrxml",
  "size": 15420,
  "lastModified": "2024-01-15T10:30:00Z",
  "etag": "1"
}
```

#### Resposta de Erro (404)
```json
{
  "error": "Relatório não encontrado"
}
```

---

## 7. Buscar Relatório com Parâmetros

### GET `/search/{code}/with-parameters`

Busca um relatório específico pelo código incluindo seus parâmetros.

#### Exemplo de Requisição
```bash
curl -X GET http://localhost:8081/api/reports/search/REL_PARAM_001/with-parameters
```

#### Resposta de Sucesso
```json
{
  "id": 1,
  "name": "teste_parametro.jrxml",
  "codigo": "REL_PARAM_001",
  "description": "Relatório de teste com parâmetros",
  "fileSize": 8532,
  "contentType": "application/xml",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00",
  "parameters": [
    {
      "id": 1,
      "parameterName": "titulo",
      "parameterType": "string",
      "defaultValue": null,
      "isRequired": false,
      "description": null,
      "createdAt": "2024-01-15T10:30:00"
    },
    {
      "id": 2,
      "parameterName": "dataInicio",
      "parameterType": "date",
      "defaultValue": null,
      "isRequired": false,
      "description": null,
      "createdAt": "2024-01-15T10:30:00"
    }
  ]
}
```

#### Exemplo com jq para Formatação
```bash
curl -X GET http://localhost:8081/api/reports/search/REL_PARAM_001/with-parameters | jq .
```

---

## 8. Remover Relatório

### DELETE `/{reportName}`

Remove um relatório específico do sistema.

#### Exemplo de Requisição
```bash
curl -X DELETE http://localhost:8081/api/reports/relatorio_antigo.jrxml
```

#### Resposta de Sucesso
```text
Relatório removido com sucesso
```

#### Resposta de Erro
```text
Erro ao remover relatório: Relatório não encontrado
```

---

## Exemplos de Uso Completo

### Fluxo Completo: Upload → Busca → Geração

#### 1. Fazer Upload do Relatório
```bash
curl -X POST \
  http://localhost:8081/api/reports/import \
  -F 'file=@meu_relatorio.jrxml' \
  -F 'reportName=meu_relatorio.jrxml' \
  -F 'codigo=REL_MEU_001' \
  -F 'descricao=Meu relatório personalizado'
```

#### 2. Verificar se foi Importado
```bash
curl -X GET http://localhost:8081/api/reports/search/REL_MEU_001
```

#### 3. Gerar o Relatório
```bash
curl -X POST \
  'http://localhost:8081/api/reports/generate/meu_relatorio.jrxml?format=pdf' \
  -H 'Content-Type: application/json' \
  -d '{}' \
  -o meu_relatorio_gerado.pdf
```

### Testando Diferentes Formatos

#### PDF
```bash
curl -X POST \
  'http://localhost:8081/api/reports/generate/relatorio_teste.jrxml?format=pdf' \
  -H 'Content-Type: application/json' \
  -o teste.pdf
```

#### HTML
```bash
curl -X POST \
  'http://localhost:8081/api/reports/generate/relatorio_teste.jrxml?format=html' \
  -H 'Content-Type: application/json' \
  -o teste.html
```

#### CSV
```bash
curl -X POST \
  'http://localhost:8081/api/reports/generate/relatorio_teste.jrxml?format=csv' \
  -H 'Content-Type: application/json' \
  -o teste.csv
```

#### Excel
```bash
curl -X POST \
  'http://localhost:8081/api/reports/generate/relatorio_teste.jrxml?format=xlsx' \
  -H 'Content-Type: application/json' \
  -o teste.xlsx
```

---

## Códigos de Status HTTP

- **200 OK**: Operação realizada com sucesso
- **400 Bad Request**: Erro na requisição (parâmetros inválidos)
- **404 Not Found**: Recurso não encontrado
- **500 Internal Server Error**: Erro interno do servidor

---

## Dicas e Observações

1. **Formatos Suportados**: pdf, html, csv, xml, xlsx
2. **Tipos de Arquivo**: .jasper e .jrxml
3. **Parâmetros**: Podem ser passados como JSON no corpo da requisição
4. **Headers**: O Content-Type é definido automaticamente baseado no formato
5. **Download**: Use `-o nome_arquivo.extensao` no curl para salvar o arquivo
6. **Debugging**: Use `-v` no curl para ver detalhes da requisição

### Exemplo de Debug
```bash
curl -v -X GET http://localhost:8081/api/reports/health
```

### Verificar se Arquivo foi Baixado
```bash
ls -la *.pdf *.xlsx *.html *.csv
```

---

## Troubleshooting

### Problemas Comuns

1. **Erro de Conexão**: Verifique se o servidor está rodando na porta 8081
2. **Arquivo não Encontrado**: Verifique se o relatório foi importado corretamente
3. **Erro de Formato**: Certifique-se de usar formatos suportados
4. **Parâmetros Inválidos**: Verifique a sintaxe JSON dos parâmetros

### Verificar Status do Servidor
```bash
curl -X GET http://localhost:8081/api/reports/health
```

### Listar Relatórios Disponíveis
```bash
curl -X GET http://localhost:8081/api/reports/list | jq .
```