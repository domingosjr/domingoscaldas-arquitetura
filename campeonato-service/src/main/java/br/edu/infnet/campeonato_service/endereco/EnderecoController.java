package br.edu.infnet.campeonato_service.endereco;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.infnet.campeonato_service.exception.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Recurso REST do módulo endereço.
 */
@RestController
@RequestMapping("/enderecos")
@Tag(name = "Endereços", description = "Consulta de endereço por CEP na API pública ViaCEP")
public class EnderecoController {

	private final EnderecoService enderecoService;

	public EnderecoController(EnderecoService enderecoService) {
		this.enderecoService = enderecoService;
	}

	@GetMapping("/{cep}")
	@Operation(summary = "Consulta o endereço de um CEP (8 dígitos)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Endereço encontrado",
					content = @Content(schema = @Schema(implementation = Endereco.class))),
			@ApiResponse(responseCode = "400", description = "CEP mal formado",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))),
			@ApiResponse(responseCode = "404", description = "CEP inexistente",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))),
			@ApiResponse(responseCode = "503", description = "ViaCEP indisponível",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))) })
	public ResponseEntity<Endereco> consultarPorCep(@PathVariable String cep) {
		return ResponseEntity.ok(enderecoService.consultarPorCep(cep));
	}
}
