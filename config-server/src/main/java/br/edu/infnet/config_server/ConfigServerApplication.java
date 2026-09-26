package br.edu.infnet.config_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * config-server — configuração centralizada do BJJ School (Etapa 3).
 * Serve, por HTTP, as configurações de ambiente (portas e URLs) da aplicação
 * principal e do campeonato-service, lidas dos arquivos em
 * src/main/resources/config (backend "native": sem repositório git).
 *
 * Exemplo: GET /domingoscaldas-arquitetura/prod devolve as propriedades da
 * aplicação principal para o profile prod.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}

}
