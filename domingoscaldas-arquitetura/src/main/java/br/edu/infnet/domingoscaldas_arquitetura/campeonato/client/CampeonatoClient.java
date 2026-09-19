package br.edu.infnet.domingoscaldas_arquitetura.campeonato.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente OpenFeign do campeonato-service. O endereço vem da propriedade
 * {@code campeonato.service.url} (application.properties), não do código Java.
 */
@FeignClient(name = "campeonato-service", url = "${campeonato.service.url}")
public interface CampeonatoClient {

	@GetMapping("/campeonatos/{id}")
	CampeonatoResponse obterPorId(@PathVariable("id") Long id);
}
