package br.edu.infnet.domingoscaldas_arquitetura.campeonato;

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
 * Recurso REST de campeonatos (módulo campeonato).
 */
@RestController
@RequestMapping("/campeonatos")
public class CampeonatoController {

	private final CampeonatoService campeonatoService;

	public CampeonatoController(CampeonatoService campeonatoService) {
		this.campeonatoService = campeonatoService;
	}

	@GetMapping
	public ResponseEntity<List<Campeonato>> obterLista() {
		return ResponseEntity.ok(campeonatoService.obterLista());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Campeonato> obterPorId(@PathVariable Long id) {
		return ResponseEntity.ok(campeonatoService.obterPorId(id));
	}

	@PostMapping
	public ResponseEntity<Campeonato> incluir(@Valid @RequestBody Campeonato campeonato) {
		Campeonato incluido = campeonatoService.incluir(campeonato);

		return ResponseEntity.status(HttpStatus.CREATED).body(incluido);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Campeonato> alterar(@PathVariable Long id, @Valid @RequestBody Campeonato campeonato) {
		return ResponseEntity.ok(campeonatoService.alterar(id, campeonato));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		campeonatoService.excluir(id);

		return ResponseEntity.noContent().build();
	}
}
