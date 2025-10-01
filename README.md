# Sistema de Relatórios VERDU ERP

Sistema de geração e gerenciamento de relatórios dinâmicos usando JasperReports, Spring Boot e PostgreSQL.

## 📋 Funcionalidades

- **Upload de Relatórios**: Importação de arquivos `.jrxml` com parâmetros configuráveis
- **Geração Dinâmica**: Criação de relatórios em múltiplos formatos (PDF, HTML, CSV, XML, XLSX)
- **Gerenciamento de Parâmetros**: Configuração de parâmetros obrigatórios e opcionais
- **Busca por Código**: Localização rápida de relatórios através de códigos únicos
- **API RESTful**: Interface completa para integração com outros sistemas
- **Armazenamento Híbrido**: Metadados no PostgreSQL e arquivos no MinIO

## 🛠️ Tecnologias Utilizadas

- **Java 17**
- **Spring Boot 3.x**
- **JasperReports 6.x**
- **PostgreSQL**
- **MinIO** (S3-compatible storage)
- **Maven**
- **JPA/Hibernate**

## 📦 Pré-requisitos

- Java 17 ou superior
- PostgreSQL 12+
- MinIO Server
- Maven 3.6+

## ⚙️ Configuração

### 1. Banco de Dados

Crie um banco PostgreSQL e configure as credenciais no `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/VERDU_ERP
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
spring.jpa.hibernate.ddl-auto=update
```

### 2. MinIO

Configure o MinIO no `application.properties`:

```properties
minio.endpoint=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin
minio.bucket-name=reports
```

### 3. Instalação

```bash
# Clone o repositório
git clone <url-do-repositorio>
cd verdu_erp

# Compile o projeto
mvn clean compile

# Execute a aplicação
mvn spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080`

## 🚀 Uso da API

### Endpoints Principais

| Método | Endpoint | Descrição |
|--------|----------|----------|
| GET | `/health` | Status da aplicação |
| POST | `/import` | Upload de relatório |
| POST | `/generate/{reportName}` | Gerar relatório por nome |
| POST | `/generate` | Gerar relatório com DTO |
| GET | `/list` | Listar todos os relatórios |
| GET | `/search/{code}` | Buscar por código |
| GET | `/search/{code}/with-parameters` | Buscar com parâmetros |
| DELETE | `/{reportName}` | Deletar relatório |

### Exemplos de Uso

#### 1. Upload de Relatório

```bash
curl -X POST http://localhost:8080/import \
  -F "file=@relatorio.jrxml" \
  -F "reportName=meu_relatorio" \
  -F "codigo=REL001" \
  -F "descricao=Relatório de Vendas"
```

#### 2. Geração de Relatório

```bash
curl -X POST "http://localhost:8080/generate/meu_relatorio?format=pdf" \
  -H "Content-Type: application/json" \
  -d '{"parametro1": "valor1", "parametro2": "valor2"}' \
  --output relatorio.pdf
```

#### 3. Listar Relatórios

```bash
curl -X GET http://localhost:8080/list
```

## 📁 Estrutura do Projeto

```
src/main/java/
├── controller/
│   └── ReportsController.java      # Endpoints da API
├── dto/
│   ├── ReportRequestDTO.java       # DTO para requisições
│   ├── ReportInfoDTO.java          # DTO de informações
│   └── ...                         # Outros DTOs
├── entity/
│   ├── Report.java                 # Entidade de relatório
│   └── ReportParameter.java        # Entidade de parâmetro
├── repository/
│   ├── ReportRepository.java       # Repositório de relatórios
│   └── ReportParameterRepository.java
└── service/
    └── ServiceReports.java         # Lógica de negócio
```

## 🔧 Desenvolvimento

### Adicionando Novos Relatórios

1. Crie o arquivo `.jrxml` usando JasperSoft Studio
2. Faça upload via API `/import`
3. Configure parâmetros se necessário
4. Teste a geração via `/generate`

### Parâmetros de Relatório

Os parâmetros são definidos no formato JSON:

```json
[
  {
    "parameterName": "data_inicio",
    "parameterType": "java.util.Date",
    "defaultValue": "2024-01-01",
    "isRequired": true,
    "description": "Data de início do período"
  }
]
```

## 📖 Documentação Adicional

- [Exemplos Completos da API](API_EXAMPLES.md)
- [Documentação da API](API_DOCUMENTATION.md)
- [Guia de Solução de Logos](GUIA_SOLUCAO_LOGOS.md)

## 🐛 Troubleshooting

### Problemas Comuns

1. **Erro de conexão com MinIO**
   - Verifique se o MinIO está rodando
   - Confirme as credenciais no `application.properties`

2. **Erro de compilação JasperReports**
   - Verifique se o arquivo `.jrxml` é válido
   - Confirme se todas as dependências estão no classpath

3. **Erro de parâmetros**
   - Verifique se todos os parâmetros obrigatórios foram fornecidos
   - Confirme os tipos de dados dos parâmetros

## 🤝 Contribuição

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas mudanças (`git commit -am 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença [MIT](LICENSE).

## 📞 Suporte

Para suporte técnico ou dúvidas:
- Abra uma issue no GitHub
- Consulte a documentação da API
- Verifique os logs da aplicação

---

**VERDU ERP** - Sistema de Relatórios Dinâmicos