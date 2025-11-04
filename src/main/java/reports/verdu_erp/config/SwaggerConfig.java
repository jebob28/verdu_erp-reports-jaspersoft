package reports.verdu_erp.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Relatórios - Verdu ERP")
                        .description("API para geração e gerenciamento de relatórios JasperReports integrada com MinIO")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe de Desenvolvimento")
                                .email("dev@verdu.com")
                                .url("https://verdu.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("https://homo-jasper.verderp.com.br")
                                .description("Servidor de Desenvolvimento"),
                        new Server()
                                .url("https://api.verdu.com")
                                .description("Servidor de Produção")
                ));
    }
}