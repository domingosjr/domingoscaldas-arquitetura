package br.edu.infnet.domingoscaldas_arquitetura.conquista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Aluno;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.AlunoNaoEncontradoException;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.AlunoService;
import br.edu.infnet.domingoscaldas_arquitetura.aluno.Faixa;
import br.edu.infnet.domingoscaldas_arquitetura.campeonato.client.CampeonatoGateway;
import br.edu.infnet.domingoscaldas_arquitetura.campeonato.client.CampeonatoResponse;
import br.edu.infnet.domingoscaldas_arquitetura.campeonato.exception.CampeonatoRemotoNaoEncontradoException;
import br.edu.infnet.domingoscaldas_arquitetura.campeonato.exception.CampeonatoServiceIndisponivelException;

/**
 * Registro de conquista: aluno validado aqui, campeonato validado no
 * campeonato-service; nada é gravado quando a validação falha.
 */
@ExtendWith(MockitoExtension.class)
class ConquistaServiceTest {

	@Mock
	private ConquistaRepository conquistaRepository;

	@Mock
	private CampeonatoGateway campeonatoGateway;

	@Mock
	private AlunoService alunoService;

	@InjectMocks
	private ConquistaService conquistaService;

	private static Aluno anderson() {
		Aluno aluno = new Aluno("Anderson Souza", "anderson@gmail.com", null, LocalDate.of(1995, 3, 10),
				LocalDate.of(2024, 2, 1), 82.5, true, Faixa.AZUL, 1);
		aluno.setId(1L);

		return aluno;
	}

	@Test
	void registrarCopiaNomeEDataDoCampeonatoRemoto() {
		when(alunoService.obterPorId(1L)).thenReturn(anderson());
		when(campeonatoGateway.obterPorId(1L)).thenReturn(
				new CampeonatoResponse(1L, "Copa Rio de Jiu-Jitsu", "Rio de Janeiro", LocalDate.of(2026, 5, 17)));
		when(conquistaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

		Conquista registrada = conquistaService.registrar(1L, 1L, new Conquista("Adulto Azul Pena", Medalha.OURO));

		assertThat(registrada.getAluno().getNome()).isEqualTo("Anderson Souza");
		assertThat(registrada.getCampeonatoId()).isEqualTo(1L);
		assertThat(registrada.getCampeonatoNome()).isEqualTo("Copa Rio de Jiu-Jitsu");
		assertThat(registrada.getCampeonatoData()).isEqualTo(LocalDate.of(2026, 5, 17));
	}

	@Test
	void naoChamaOServicoQuandoOAlunoNaoExiste() {
		when(alunoService.obterPorId(999L)).thenThrow(new AlunoNaoEncontradoException(999L));

		assertThatThrownBy(() -> conquistaService.registrar(999L, 1L, new Conquista("Adulto", Medalha.OURO)))
				.isInstanceOf(AlunoNaoEncontradoException.class);

		verifyNoInteractions(campeonatoGateway);
		verify(conquistaRepository, never()).save(any());
	}

	@Test
	void naoGravaQuandoOCampeonatoNaoExisteNoServico() {
		when(alunoService.obterPorId(1L)).thenReturn(anderson());
		when(campeonatoGateway.obterPorId(999L)).thenThrow(new CampeonatoRemotoNaoEncontradoException(999L));

		assertThatThrownBy(() -> conquistaService.registrar(1L, 999L, new Conquista("Adulto", Medalha.OURO)))
				.isInstanceOf(CampeonatoRemotoNaoEncontradoException.class);

		verify(conquistaRepository, never()).save(any());
	}

	@Test
	void naoGravaQuandoOServicoEstaIndisponivel() {
		when(alunoService.obterPorId(1L)).thenReturn(anderson());
		when(campeonatoGateway.obterPorId(1L)).thenThrow(new CampeonatoServiceIndisponivelException());

		assertThatThrownBy(() -> conquistaService.registrar(1L, 1L, new Conquista("Adulto", Medalha.OURO)))
				.isInstanceOf(CampeonatoServiceIndisponivelException.class);

		verify(conquistaRepository, never()).save(any());
	}
}
