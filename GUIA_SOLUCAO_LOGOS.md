# Guia de Solução - Problemas com Logos/Imagens em Relatórios JasperReports

## Problema Identificado
Quando você adiciona logos ou imagens aos relatórios JasperReports, podem ocorrer erros durante a geração.

## Principais Causas e Soluções

### 1. UUID Inválido
**Erro:** `Cannot deserialize value of type java.util.UUID from String "logo-test-uuid"`

**Causa:** O atributo `uuid` do elemento de imagem deve estar no formato UUID padrão (36 caracteres).

**Solução:**
```xml
<!-- ❌ INCORRETO -->
<element kind="image" uuid="logo-test-uuid" ...>

<!-- ✅ CORRETO -->
<element kind="image" uuid="12345678-1234-1234-1234-123456789abc" ...>
```

### 2. Sintaxe Incorreta para Expressão de Imagem
**Erro:** `Unrecognized field "imageExpression"`

**Causa:** No formato XML do Jaspersoft Studio, a expressão da imagem deve usar `<expression>` ao invés de `<imageExpression>`.

**Solução:**
```xml
<!-- ❌ INCORRETO -->
<element kind="image" uuid="12345678-1234-1234-1234-123456789abc" x="20" y="10" width="100" height="60" scaleImage="FillFrame">
    <imageExpression><![CDATA["https://example.com/logo.png"]]></imageExpression>
</element>

<!-- ✅ CORRETO -->
<element kind="image" uuid="12345678-1234-1234-1234-123456789abc" x="20" y="10" width="100" height="60" scaleImage="FillFrame">
    <expression><![CDATA["https://example.com/logo.png"]]></expression>
</element>
```

## Exemplo Completo Funcionando

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jasperReport name="relatorio_com_logo" language="java" pageWidth="595" pageHeight="842" columnWidth="555" leftMargin="20" rightMargin="20" topMargin="20" bottomMargin="20" uuid="1a338fd7-86fc-421d-a02a-52eb91aefc88">
    <title height="120" splitType="Stretch">
        <!-- Logo/Imagem -->
        <element kind="image" uuid="12345678-1234-1234-1234-123456789abc" x="20" y="10" width="100" height="60" scaleImage="FillFrame">
            <expression><![CDATA["https://via.placeholder.com/100x60/0000FF/FFFFFF?text=LOGO"]]></expression>
        </element>
        
        <!-- Título -->
        <element kind="staticText" uuid="431934b2-4157-4286-9cc9-ad6e8aa10fe9" x="191" y="80" width="208" height="30" fontSize="16.0" bold="true" vTextAlign="Middle">
            <text><![CDATA[RELATÓRIO COM LOGO]]></text>
        </element>
    </title>
    <!-- resto do relatório... -->
</jasperReport>
```

## Tipos de Imagem Suportados

1. **URLs externas:** `"https://example.com/logo.png"`
2. **Arquivos locais:** `"logo.png"` (deve estar no classpath)
3. **Parâmetros:** `$P{LOGO_PATH}` (passado como parâmetro)
4. **Base64:** Para imagens embarcadas

## Dicas Importantes

1. **UUID:** Sempre use UUIDs válidos de 36 caracteres
2. **Sintaxe:** Use `<expression>` para definir a fonte da imagem
3. **Escala:** Configure `scaleImage="FillFrame"` para ajustar a imagem ao tamanho definido
4. **Posicionamento:** Defina `x`, `y`, `width` e `height` adequadamente
5. **Teste:** Sempre teste com URLs públicas primeiro para verificar se a funcionalidade está funcionando

## Status da Solução
✅ **RESOLVIDO:** O sistema agora suporta logos/imagens corretamente quando seguindo a sintaxe adequada.