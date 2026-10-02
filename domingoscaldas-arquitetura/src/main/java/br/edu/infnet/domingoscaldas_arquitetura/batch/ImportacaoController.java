package br.edu.infnet.domingoscaldas_arquitetura.batch;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gatilho do processamento em lote (em produção, poderia ser um agendamento noturno).
 */
@RestController
@RequestMapping("/importacoes")
public class ImportacaoController {

	private final ImportacaoPresencasService importacaoPresencasService;

	public ImportacaoController(ImportacaoPresencasService importacaoPresencasService) {
		this.importacaoPresencasService = importacaoPresencasService;
	}

	@PostMapping("/presencas")
	public ResponseEntity<ImportacaoResponse> importarPresencas() {
		return ResponseEntity.ok(importacaoPresencasService.importar());
	}
}
