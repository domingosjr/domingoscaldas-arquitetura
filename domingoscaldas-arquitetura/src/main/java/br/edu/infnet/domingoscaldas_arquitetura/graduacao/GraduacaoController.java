package br.edu.infnet.domingoscaldas_arquitetura.graduacao;

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

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import jakarta.validation.Valid;

/**
 * Recurso REST de graduações e da regra de pontos (módulo graduação).
 */
@RestController
@RequestMapping("/graduacoes")
public class GraduacaoController {

	private final GraduacaoService graduacaoService;

	public GraduacaoController(GraduacaoService graduacaoService) {
		this.graduacaoService = graduacaoService;
	}

	@GetMapping
	public ResponseEntity<List<Graduacao>> obterLista() {
		return ResponseEntity.ok(graduacaoService.obterGraduacoes());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Graduacao> obterPorId(@PathVariable Long id) {
		return ResponseEntity.ok(graduacaoService.obterGraduacaoPorId(id));
	}

	@GetMapping("/aptos")
	public ResponseEntity<List<Aluno>> obterAptos() {
		return ResponseEntity.ok(graduacaoService.obterAptos());
	}

	@GetMapping("/alunos/{alunoId}")
	public ResponseEntity<List<Graduacao>> obterPorAluno(@PathVariable Long alunoId) {
		return ResponseEntity.ok(graduacaoService.obterGraduacoesDoAluno(alunoId));
	}

	@GetMapping("/alunos/{alunoId}/pontos")
	public ResponseEntity<Long> obterPontos(@PathVariable Long alunoId) {
		return ResponseEntity.ok(graduacaoService.calcularPontos(alunoId));
	}

	@PostMapping("/alunos/{alunoId}")
	public ResponseEntity<Graduacao> registrar(@PathVariable Long alunoId, @Valid @RequestBody Graduacao graduacao) {
		Graduacao registrada = graduacaoService.registrarGraduacao(alunoId, graduacao);

		return ResponseEntity.status(HttpStatus.CREATED).body(registrada);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Graduacao> alterar(@PathVariable Long id, @Valid @RequestBody Graduacao graduacao) {
		return ResponseEntity.ok(graduacaoService.alterarGraduacao(id, graduacao));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		graduacaoService.excluirGraduacao(id);

		return ResponseEntity.noContent().build();
	}
}
