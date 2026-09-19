package br.edu.infnet.campeonato_service.endereco.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import br.edu.infnet.campeonato_service.endereco.Endereco;

/**
 * Cliente OpenFeign para a API pública ViaCEP. O endereço vem da propriedade
 * {@code viacep.url} (application.properties), não do código Java.
 */
@FeignClient(name = "viacep", url = "${viacep.url}")
public interface ViaCepClient {

	@GetMapping("/{cep}/json")
	Endereco consultarCep(@PathVariable("cep") String cep);
}
