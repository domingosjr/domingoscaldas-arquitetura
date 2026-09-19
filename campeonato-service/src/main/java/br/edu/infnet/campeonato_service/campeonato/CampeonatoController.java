package br.edu.infnet.campeonato_service.campeonato;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.infnet.campeonato_service.campeonato.dto.CampeonatoRequest;
import br.edu.infnet.campeonato_service.campeonato.dto.CampeonatoResponse;
import br.edu.infnet.campeonato_service.exception.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Recurso REST de campeonatos. Entrada e saída sempre por DTOs.
 */
@RestController
@RequestMapping("/campeonatos")
@Tag(name = "Campeonatos", description = "Cadastro dos campeonatos em que a escola compete")
public class CampeonatoController {

	private final CampeonatoService campeonatoService;

	public CampeonatoController(CampeonatoService campeonatoService) {
		this.campeonatoService = campeonatoService;
	}

	@GetMapping
	@Operation(summary = "Lista os campeonatos, do mais recente para o mais antigo")
	@ApiResponse(responseCode = "200", description = "Lista de campeonatos (pode ser vazia)",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = CampeonatoResponse.class))))
	public ResponseEntity<List<CampeonatoResponse>> obterLista() {
		return ResponseEntity.ok(campeonatoService.obterLista());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Obtém um campeonato pelo identificador",
			description = "Operação consumida pela aplicação principal (OpenFeign) ao registrar uma conquista.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Campeonato encontrado",
					content = @Content(schema = @Schema(implementation = CampeonatoResponse.class))),
			@ApiResponse(responseCode = "404", description = "Campeonato não encontrado",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))) })
	public ResponseEntity<CampeonatoResponse> obterPorId(@PathVariable Long id) {
		return ResponseEntity.ok(campeonatoService.obterPorId(id));
	}

	@PostMapping
	@Operation(summary = "Cadastra um campeonato",
			description = "Quando o CEP é informado, a cidade é preenchida pela consulta ao ViaCEP.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Campeonato cadastrado",
					content = @Content(schema = @Schema(implementation = CampeonatoResponse.class))),
			@ApiResponse(responseCode = "400", description = "Dados inválidos",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))),
			@ApiResponse(responseCode = "404", description = "CEP inexistente",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))),
			@ApiResponse(responseCode = "503", description = "Consulta de CEP (ViaCEP) indisponível",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))) })
	public ResponseEntity<CampeonatoResponse> incluir(@Valid @RequestBody CampeonatoRequest request) {
		CampeonatoResponse incluido = campeonatoService.incluir(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(incluido);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Altera um campeonato")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Campeonato alterado",
					content = @Content(schema = @Schema(implementation = CampeonatoResponse.class))),
			@ApiResponse(responseCode = "400", description = "Dados inválidos",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))),
			@ApiResponse(responseCode = "404", description = "Campeonato ou CEP não encontrado",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))),
			@ApiResponse(responseCode = "503", description = "Consulta de CEP (ViaCEP) indisponível",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))) })
	public ResponseEntity<CampeonatoResponse> alterar(@PathVariable Long id,
			@Valid @RequestBody CampeonatoRequest request) {
		return ResponseEntity.ok(campeonatoService.alterar(id, request));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Exclui um campeonato")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Campeonato excluído", content = @Content),
			@ApiResponse(responseCode = "404", description = "Campeonato não encontrado",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))) })
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		campeonatoService.excluir(id);

		return ResponseEntity.noContent().build();
	}
}
