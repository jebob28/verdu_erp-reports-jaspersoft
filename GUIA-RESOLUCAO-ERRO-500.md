# 🚀 GUIA PARA RESOLVER ERRO 500 NO DOCKER SWARM

## 📋 Problema
Erro 500 ao gerar relatório `separacaodecarga.jrxml` com parâmetro `ROTA_ID=5`:
```
Error compiling report java source files: /tmp/separacaodecarga_xxx.java
```

## ✅ Solução Completa

### 1. 📦 Stack Docker Swarm Atualizado
Use o arquivo `docker-stack-final.yml` que contém todas as correções necessárias:

```bash
# No servidor Docker Swarm (10.200.0.50)
docker stack deploy -c docker-stack-final.yml api-comercial
```

### 2. 🔧 Configurações Essenciais no Stack

#### Compilador JasperReports (EVITA ERRO DE COMPILAÇÃO)
```yaml
net_sf_jasperreports_compiler_class: "net.sf.jasperreports.engine.design.JRJdtCompiler"
net_sf_jasperreports_compiler_cache_temp_files: "false"
net_sf_jasperreports_compiler_java: "false"
net_sf_jasperreports_compiler_expression_class: "false"
net_sf_jasperreports_compiler_expression_groovy: "false"
net_sf_jasperreports_compiler_expression_javascript: "false"
```

#### Volumes para Fontes e Arquivos Temporários
```yaml
volumes:
  - /usr/share/fonts:/usr/share/fonts:ro
  - jasperreports-tmp:/tmp
  - jasperreports-reports:/app/reports
```

#### Correção do MinIO Endpoint
```yaml
MINIO_ENDPOINT: http://192.168.8.200:9000  # Sem aspas invertidas
```

### 3. 📝 Arquivos Modificados na Aplicação

#### ServiceReports.java
- Adicionado JRJdtCompiler para evitar compilação complexa
- Configurações de segurança para desabilitar expressões perigosas
- Suporte a profile "homolog" para fontes do sistema

#### separacaodecarga.jrxml
- Removido concatenação de strings que causava compilação
- Adicionado textos estáticos separados dos campos dinâmicos
- Simplificado expressões JasperReports

### 4. 🧪 Testar a Solução

```bash
# Executar teste local
./testar-relatorio.sh

# Ou testar manualmente
curl -X POST http://hjaspersoft.verderp.com.br/api/reports/generate \
  -H "Content-Type: application/json" \
  -d '{"reportName":"separacaodecarga.jrxml","format":"pdf","parameters":{"ROTA_ID":5}}'
```

### 5. 🔍 Verificar Logs no Servidor

```bash
# Logs do serviço no Docker Swarm
docker service logs api-comercial_api-comercial --tail 100

# Ou se souber o nome exato do container
docker logs <nome-do-container> --tail 50
```

### 6. ⚠️ Pontos de Atenção

1. **Fontes do Sistema**: Certifique-se que `/usr/share/fonts` existe nos nodes do Swarm
2. **Permissões**: Os containers precisam de permissão para escrever em `/tmp`
3. **Profile**: O profile `homolog` deve estar ativo
4. **Imagem Docker**: Use a imagem com as correções (v2) ou build localmente

### 7. 🔄 Se Ainda Houver Erro

Se o erro persistir, verifique:

1. **Build da Imagem**: 
   ```bash
   ./build-and-push.sh  # Para criar imagem com correções
   ```

2. **Variáveis de Ambiente**: 
   - Todas as variáveis `net_sf_jasperreports_*` estão no stack?
   - O profile `homolog` está configurado?

3. **Volumes**: 
   - Os volumes de fontes estão montados?
   - O diretório `/tmp` é gravável?

4. **Logs Detalhados**:
   ```bash
   # Ver variáveis de ambiente do container
docker exec <container-id> env | grep jasper
   
   # Verificar se fontes estão disponíveis
   docker exec <container-id> ls -la /usr/share/fonts/
   ```

### 8. 🎯 Stack Final Completo

O arquivo `docker-stack-final.yml` contém:
- ✅ Configurações do compilador JasperReports
- ✅ Volumes de fontes e temporários
- ✅ Variáveis de ambiente corretas
- ✅ Health check adequado
- ✅ Resources limits
- ✅ Security options

## 🎉 Sucesso Esperado
Após aplicar estas correções, o relatório `separacaodecarga.jrxml` com `ROTA_ID=5` deve gerar com sucesso (HTTP 200) e retornar o PDF.