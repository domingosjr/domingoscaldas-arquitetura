package br.edu.infnet.campeonato_service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import br.edu.infnet.campeonato_service.campeonato.CampeonatoService;
import br.edu.infnet.campeonato_service.campeonato.dto.CampeonatoRequest;

/**
 * Carga de dados de demonstração no H2 do serviço (desative com
 * app.runner.habilitado=false). Sem CEP, para não depender da internet na
 * subida. O campeonato de id 1 (Copa Rio) é o mesmo usado nas conquistas
 * semeadas pela aplicação principal.
 */
@Component
public class ProjectRunner implements CommandLineRunner {

	private final CampeonatoService campeonatoService;
	private final boolean habilitado;

	public ProjectRunner(CampeonatoService campeonatoService,
			@Value("${app.runner.habilitado:true}") boolean habilitado) {
		this.campeonatoService = campeonatoService;
		this.habilitado = habilitado;
	}

	@Override
	public void run(String... args) {

		if (!habilitado || !campeonatoService.obterLista().isEmpty()) {
			return;
		}

		campeonatoService.incluir(
				new CampeonatoRequest("Copa Rio de Jiu-Jitsu", "Rio de Janeiro", null, LocalDate.of(2026, 5, 17)));
		campeonatoService.incluir(new CampeonatoRequest("Campeonato Brasileiro de Jiu-Jitsu", "Barueri - SP", null,
				LocalDate.of(2026, 10, 24)));

		System.out.println("campeonato-service: dados de demonstração carregados — campeonatos: "
				+ campeonatoService.obterLista().size());
	}
}
