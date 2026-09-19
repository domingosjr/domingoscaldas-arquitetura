package br.edu.infnet.campeonato_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * campeonato-service — serviço independente do BJJ School (Etapa 2).
 * Responsabilidade: cadastro dos campeonatos em que a escola compete, com a
 * cidade preenchida pelo CEP (consulta à API pública ViaCEP).
 * É consumido pela aplicação principal (módulo conquista) via OpenFeign.
 */
@SpringBootApplication
@EnableFeignClients
public class CampeonatoServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampeonatoServiceApplication.class, args);
	}

}
