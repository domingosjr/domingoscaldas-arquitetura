package br.edu.infnet.domingoscaldas_arquitetura.instrutor;

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

import jakarta.validation.Valid;

/**
 * Recurso REST do módulo instrutor.
 */
@RestController
@RequestMapping("/instrutores")
public class InstrutorController {

	private final InstrutorService instrutorService;

	public InstrutorController(InstrutorService instrutorService) {
		this.instrutorService = instrutorService;
	}

	@GetMapping
	public ResponseEntity<List<Instrutor>> obterLista() {
		return ResponseEntity.ok(instrutorService.obterLista());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Instrutor> obterPorId(@PathVariable Long id) {
		return ResponseEntity.ok(instrutorService.obterPorId(id));
	}

	@GetMapping("/ativos")
	public ResponseEntity<List<Instrutor>> obterAtivos() {
		return ResponseEntity.ok(instrutorService.obterAtivos());
	}

	@PostMapping
	public ResponseEntity<Instrutor> incluir(@Valid @RequestBody Instrutor instrutor) {
		Instrutor incluido = instrutorService.incluir(instrutor);

		return ResponseEntity.status(HttpStatus.CREATED).body(incluido);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Instrutor> alterar(@PathVariable Long id, @Valid @RequestBody Instrutor instrutor) {
		return ResponseEntity.ok(instrutorService.alterar(id, instrutor));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		instrutorService.excluir(id);

		return ResponseEntity.noContent().build();
	}
}
