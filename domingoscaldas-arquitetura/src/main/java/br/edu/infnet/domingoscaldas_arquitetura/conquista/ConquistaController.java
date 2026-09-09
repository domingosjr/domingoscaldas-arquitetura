package br.edu.infnet.domingoscaldas_arquitetura.conquista;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Recurso REST de conquistas (módulo campeonato).
 */
@RestController
@RequestMapping("/conquistas")
public class ConquistaController {

	private final ConquistaService conquistaService;

	public ConquistaController(ConquistaService conquistaService) {
		this.conquistaService = conquistaService;
	}

	@GetMapping
	public ResponseEntity<List<Conquista>> obterLista() {
		return ResponseEntity.ok(conquistaService.obterLista());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Conquista> obterPorId(@PathVariable Long id) {
		return ResponseEntity.ok(conquistaService.obterPorId(id));
	}

	@GetMapping(params = "medalha")
	public ResponseEntity<List<Conquista>> obterPorMedalha(@RequestParam Medalha medalha) {
		return ResponseEntity.ok(conquistaService.obterPorMedalha(medalha));
	}

	@GetMapping("/quadro-medalhas")
	public ResponseEntity<Map<Medalha, Long>> obterQuadroDeMedalhas() {
		return ResponseEntity.ok(conquistaService.obterQuadroDeMedalhas());
	}

	@GetMapping("/alunos/{alunoId}")
	public ResponseEntity<List<Conquista>> obterPorAluno(@PathVariable Long alunoId) {
		return ResponseEntity.ok(conquistaService.obterPorAluno(alunoId));
	}

	@PostMapping("/alunos/{alunoId}/campeonatos/{campeonatoId}")
	public ResponseEntity<Conquista> registrar(@PathVariable Long alunoId, @PathVariable Long campeonatoId,
			@Valid @RequestBody Conquista conquista) {
		Conquista registrada = conquistaService.registrar(alunoId, campeonatoId, conquista);

		return ResponseEntity.status(HttpStatus.CREATED).body(registrada);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Conquista> alterar(@PathVariable Long id, @Valid @RequestBody Conquista conquista) {
		return ResponseEntity.ok(conquistaService.alterar(id, conquista));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		conquistaService.excluir(id);

		return ResponseEntity.noContent().build();
	}
}
