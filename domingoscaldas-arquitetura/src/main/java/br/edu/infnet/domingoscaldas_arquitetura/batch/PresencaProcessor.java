package br.edu.infnet.domingoscaldas_arquitetura.batch;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.AlunoService;
import br.edu.infnet.domingoscaldas_arquitetura.graduacao.GraduacaoService;

/**
 * Regra aplicada a cada linha da lista de presença:
 * normaliza o tipo de treino ("gi", " GI " → "Gi"; "nogi", "NO GI" → "No-Gi") e
 * ignora (devolve null) as linhas que não podem ser usadas — aluno inexistente
 * ou inativo, data inválida ou futura, tipo de treino vazio e presença já
 * registrada. Linha ignorada não é erro: ela só não chega ao writer.
 */
@Component
public class PresencaProcessor implements ItemProcessor<PresencaBatch, PresencaImportada> {

	private static final Logger log = LoggerFactory.getLogger(PresencaProcessor.class);

	private final AlunoService alunoService;
	private final GraduacaoService graduacaoService;

	public PresencaProcessor(AlunoService alunoService, GraduacaoService graduacaoService) {
		this.alunoService = alunoService;
		this.graduacaoService = graduacaoService;
	}

	@Override
	public PresencaImportada process(PresencaBatch linha) {

		Optional<Aluno> aluno = alunoService.buscarPorId(linha.alunoId());
		if (aluno.isEmpty()) {
			return ignorar(linha, "aluno inexistente");
		}
		if (!aluno.get().isAtivo()) {
			return ignorar(linha, "aluno inativo");
		}

		LocalDate data;
		try {
			data = LocalDate.parse(linha.data().trim());
		} catch (DateTimeParseException | NullPointerException e) {
			return ignorar(linha, "data inválida");
		}
		if (data.isAfter(LocalDate.now())) {
			return ignorar(linha, "data futura");
		}

		String tipoTreino = normalizarTipoTreino(linha.tipoTreino());
		if (tipoTreino.isEmpty()) {
			return ignorar(linha, "tipo de treino vazio");
		}
		if (graduacaoService.existePresenca(linha.alunoId(), data, tipoTreino)) {
			return ignorar(linha, "presença já registrada");
		}

		PresencaImportada importada = new PresencaImportada(linha.alunoId(), data, tipoTreino);
		log.info("Presença processada: {} -> {}", linha, importada);

		return importada;
	}

	/** "gi", " GI " → "Gi"; "nogi", "no-gi", "NO GI" → "No-Gi"; outros valores só sem espaços nas pontas. */
	static String normalizarTipoTreino(String tipoTreino) {

		if (tipoTreino == null) {
			return "";
		}

		String simplificado = tipoTreino.trim().toLowerCase().replace(" ", "").replace("-", "");

		return switch (simplificado) {
			case "gi" -> "Gi";
			case "nogi" -> "No-Gi";
			default -> tipoTreino.trim();
		};
	}

	private PresencaImportada ignorar(PresencaBatch linha, String motivo) {
		log.warn("Linha ignorada ({}): {}", motivo, linha);

		return null;
	}
}
