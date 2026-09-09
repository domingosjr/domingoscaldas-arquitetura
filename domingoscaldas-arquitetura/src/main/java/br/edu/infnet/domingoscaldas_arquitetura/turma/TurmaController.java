package br.edu.infnet.domingoscaldas_arquitetura.turma;

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

import br.edu.infnet.domingoscaldas_arquitetura.turma.dto.TurmaResponse;
import jakarta.validation.Valid;

/**
 * Recurso REST do módulo turma.
 */
@RestController
@RequestMapping("/turmas")
public class TurmaController {

	private final TurmaService turmaService;

	public TurmaController(TurmaService turmaService) {
		this.turmaService = turmaService;
	}

	@GetMapping
	public ResponseEntity<List<Turma>> obterLista() {
		return ResponseEntity.ok(turmaService.obterLista());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Turma> obterPorId(@PathVariable Long id) {
		return ResponseEntity.ok(turmaService.obterPorId(id));
	}

	@GetMapping("/{id}/detalhes")
	public ResponseEntity<TurmaResponse> obterDetalhes(@PathVariable Long id) {
		return ResponseEntity.ok(turmaService.obterDetalhes(id));
	}

	@GetMapping("/ativas")
	public ResponseEntity<List<Turma>> obterAtivas() {
		return ResponseEntity.ok(turmaService.obterAtivas());
	}

	@PostMapping
	public ResponseEntity<Turma> incluir(@Valid @RequestBody Turma turma) {
		Turma incluida = turmaService.incluir(turma);

		return ResponseEntity.status(HttpStatus.CREATED).body(incluida);
	}

	@PostMapping("/{turmaId}/alunos/{alunoId}")
	public ResponseEntity<Turma> matricular(@PathVariable Long turmaId, @PathVariable Long alunoId) {
		return ResponseEntity.ok(turmaService.matricularAluno(turmaId, alunoId));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Turma> alterar(@PathVariable Long id, @Valid @RequestBody Turma turma) {
		return ResponseEntity.ok(turmaService.alterar(id, turma));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		turmaService.excluir(id);

		return ResponseEntity.noContent().build();
	}
}
