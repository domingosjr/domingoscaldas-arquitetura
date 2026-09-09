package br.edu.infnet.domingoscaldas_arquitetura.endereco;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recurso REST do módulo endereço.
 */
@RestController
@RequestMapping("/enderecos")
public class EnderecoController {

	private final EnderecoService enderecoService;

	public EnderecoController(EnderecoService enderecoService) {
		this.enderecoService = enderecoService;
	}

	@GetMapping("/{cep}")
	public ResponseEntity<Endereco> consultarPorCep(@PathVariable String cep) {
		return ResponseEntity.ok(enderecoService.consultarPorCep(cep));
	}
}
