package br.edu.infnet.domingoscaldas_arquitetura.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.AlunoService;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.Faixa;
import br.edu.infnet.domingoscaldas_arquitetura.graduacao.GraduacaoService;

/**
 * Regras do processor: normaliza o tipo de treino e ignora (devolve null) as
 * linhas que não podem ser usadas.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresencaProcessorTest {

	@Mock
	private AlunoService alunoService;

	@Mock
	private GraduacaoService graduacaoService;

	@InjectMocks
	private PresencaProcessor processor;

	private static Optional<Aluno> aluno(boolean ativo) {
		return Optional.of(new Aluno("Beatriz Lima", "beatriz@gmail.com", null, LocalDate.of(2000, 11, 25),
				LocalDate.of(2025, 6, 15), 61.0, ativo, Faixa.AZUL, 0));
	}

	@Test
	void normalizaOTipoDeTreino() {
		when(alunoService.buscarPorId(2L)).thenReturn(aluno(true));

		assertThat(processor.process(new PresencaBatch(2L, "2026-08-17", " gi ")))
				.isEqualTo(new PresencaImportada(2L, LocalDate.of(2026, 8, 17), "Gi"));
		assertThat(processor.process(new PresencaBatch(2L, "2026-08-17", "NO GI")).tipoTreino()).isEqualTo("No-Gi");
		assertThat(processor.process(new PresencaBatch(2L, "2026-08-17", "nogi")).tipoTreino()).isEqualTo("No-Gi");
	}

	@Test
	void ignoraAlunoInexistente() {
		when(alunoService.buscarPorId(99L)).thenReturn(Optional.empty());

		assertThat(processor.process(new PresencaBatch(99L, "2026-09-01", "Gi"))).isNull();
	}

	@Test
	void ignoraAlunoInativo() {
		when(alunoService.buscarPorId(3L)).thenReturn(aluno(false));

		assertThat(processor.process(new PresencaBatch(3L, "2026-09-01", "Gi"))).isNull();
	}

	@Test
	void ignoraDataInvalidaDataFuturaETipoVazio() {
		when(alunoService.buscarPorId(1L)).thenReturn(aluno(true));

		assertThat(processor.process(new PresencaBatch(1L, "2026-02-30", "Gi"))).isNull();
		assertThat(processor.process(new PresencaBatch(1L, "2099-01-10", "Gi"))).isNull();
		assertThat(processor.process(new PresencaBatch(1L, "2026-09-06", " "))).isNull();
	}

	@Test
	void ignoraPresencaJaRegistrada() {
		when(alunoService.buscarPorId(1L)).thenReturn(aluno(true));
		when(graduacaoService.existePresenca(anyLong(), any(), anyString())).thenReturn(true);

		assertThat(processor.process(new PresencaBatch(1L, "2026-01-05", "Gi"))).isNull();
	}
}
