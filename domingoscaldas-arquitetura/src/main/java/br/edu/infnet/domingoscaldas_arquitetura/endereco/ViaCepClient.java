package br.edu.infnet.domingoscaldas_arquitetura.endereco;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente OpenFeign para a API pública ViaCEP.
 */
@FeignClient(name = "viacep", url = "https://viacep.com.br/ws")
public interface ViaCepClient {

	@GetMapping("/{cep}/json")
	Endereco consultarCep(@PathVariable String cep);
}
