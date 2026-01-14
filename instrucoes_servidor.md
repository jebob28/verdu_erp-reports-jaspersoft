# Instruções para Deploy no Servidor Remoto

## 1. Build e Deploy

```bash
# No servidor, navegue até o diretório do projeto
cd /caminho/do/seu/projeto

# Faça o build da aplicação
mvn clean package -DskipTests

# Construa a imagem Docker
docker build -t verdu-erp-reports .

# Pare o container antigo (se estiver rodando)
docker stop verdu-erp-reports

# Inicie o novo container
docker run -d \
  --name verdu-erp-reports \
  -p 8015:8015 \
  -e SPRING_PROFILES_ACTIVE=homolog \
  -e JAVA_OPTS="-Djava.awt.headless=true -Djava.io.tmpdir=/tmp" \
  verdu-erp-reports
```

## 2. Verificações no Container

```bash
# Verifique se o container está rodando
docker ps

# Verifique os logs
docker logs verdu-erp-reports

# Entre no container para verificar permissões
docker exec -it verdu-erp-reports bash

# Dentro do container, verifique:
ls -la /tmp/
java -version
```

## 3. Teste o Relatório

Use o comando curl para testar:

```bash
curl -X POST http://localhost:8015/api/reports/generate \
  -H "Content-Type: application/json" \
  -d '{"reportName":"separacaodecarga.jrxml","format":"pdf","parameters":{"ROTA_ID":5}}'
```

## 4. Se ainda houver erro de compilação

Se o erro persistir, adicione estas variáveis de ambiente no Docker:

```bash
docker run -d \
  --name verdu-erp-reports \
  -p 8015:8015 \
  -e SPRING_PROFILES_ACTIVE=homolog \
  -e JAVA_OPTS="-Djava.awt.headless=true -Djava.io.tmpdir=/tmp -Dnet.sf.jasperreports.compiler.class=net.sf.jasperreports.engine.design.JRJdtCompiler -Dnet.sf.jasperreports.compiler.cache.temp.files=false" \
  verdu-erp-reports
```

## 5. Verificação de Fontes

Dentro do container, verifique se as fontes estão instaladas:

```bash
# Liste as fontes disponíveis
fc-list | grep -i dejavu

# Se não houver fontes, instale:
apt-get update && apt-get install -y fonts-dejavu-core
```