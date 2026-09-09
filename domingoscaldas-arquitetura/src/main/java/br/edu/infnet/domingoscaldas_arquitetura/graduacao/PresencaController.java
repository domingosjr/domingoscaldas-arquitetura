package br.edu.infnet.domingoscaldas_arquitetura.graduacao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
 * Recurso REST de presenças (módulo graduação).
 */
@RestController
@RequestMapping("/presencas")
public class PresencaController {

	private final GraduacaoService graduacaoService;

	public PresencaController(GraduacaoService graduacaoService) {
		this.graduacaoService = graduacaoService;
	}

	@GetMapping
	public ResponseEntity<List<Presenca>> obterLista() {
		return ResponseEntity.ok(graduacaoService.obterPresencas());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Presenca> obterPorId(@PathVariable Long id) {
		return ResponseEntity.ok(graduacaoService.obterPresencaPorId(id));
	}

	@GetMapping(params = { "inicio", "fim" })
	public ResponseEntity<List<Presenca>> obterPorPeriodo(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
		return ResponseEntity.ok(graduacaoService.obterPresencasPorPeriodo(inicio, fim));
	}

	@GetMapping("/alunos/{alunoId}")
	public ResponseEntity<List<Presenca>> obterPorAluno(@PathVariable Long alunoId) {
		return ResponseEntity.ok(graduacaoService.obterPresencasDoAluno(alunoId));
	}

	@PostMapping("/alunos/{alunoId}")
	public ResponseEntity<Presenca> registrar(@PathVariable Long alunoId, @Valid @RequestBody Presenca presenca) {
		Presenca registrada = graduacaoService.registrarPresenca(alunoId, presenca);

		return ResponseEntity.status(HttpStatus.CREATED).body(registrada);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Presenca> alterar(@PathVariable Long id, @Valid @RequestBody Presenca presenca) {
		return ResponseEntity.ok(graduacaoService.alterarPresenca(id, presenca));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		graduacaoService.excluirPresenca(id);

		return ResponseEntity.noContent().build();
	}
}
