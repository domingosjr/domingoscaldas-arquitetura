package br.edu.infnet.campeonato_service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Informações gerais do Swagger do serviço (/swagger-ui.html).
 */
@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI campeonatoServiceOpenApi() {
		return new OpenAPI().info(new Info()
				.title("campeonato-service — BJJ School")
				.version("etapa-2")
				.description("Serviço independente de campeonatos da escola de Jiu-Jitsu: cadastro dos eventos em que "
						+ "a escola compete e consulta de endereço por CEP (ViaCEP). A aplicação principal consulta "
						+ "GET /campeonatos/{id} via OpenFeign ao registrar a conquista de um aluno."));
	}
}
