package br.edu.infnet.domingoscaldas_arquitetura.campeonato.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.edu.infnet.domingoscaldas_arquitetura.campeonato.exception.CampeonatoRemotoNaoEncontradoException;
import br.edu.infnet.domingoscaldas_arquitetura.campeonato.exception.CampeonatoServiceIndisponivelException;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;

/**
 * O gateway traduz as falhas do OpenFeign em exceções do domínio.
 */
class CampeonatoGatewayTest {

	private static final Request REQUEST = Request.create(Request.HttpMethod.GET,
			"http://localhost:8083/campeonatos/1", Map.of(), (byte[]) null, StandardCharsets.UTF_8, null);

	private final CampeonatoClient client = mock(CampeonatoClient.class);
	private final CampeonatoGateway gateway = new CampeonatoGateway(client);

	@Test
	void deveDevolverOCampeonatoQuandoOServicoResponde() {
		CampeonatoResponse copaRio = new CampeonatoResponse(1L, "Copa Rio de Jiu-Jitsu", "Rio de Janeiro",
				LocalDate.of(2026, 5, 17));
		when(client.obterPorId(1L)).thenReturn(copaRio);

		assertThat(gateway.obterPorId(1L)).isEqualTo(copaRio);
	}

	@Test
	void deveTraduzir404RemotoEmCampeonatoNaoEncontrado() {
		when(client.obterPorId(999L)).thenThrow(new FeignException.NotFound("Not Found", REQUEST, null, Map.of()));

		assertThatThrownBy(() -> gateway.obterPorId(999L))
				.isInstanceOf(CampeonatoRemotoNaoEncontradoException.class)
				.hasMessage("O campeonato de ID 999 não foi encontrado no campeonato-service.");
	}

	@Test
	void deveTraduzirServicoForaDoArEmIndisponivelSemDetalhesTecnicos() {
		when(client.obterPorId(1L)).thenThrow(new RetryableException(-1, "Connection refused",
				Request.HttpMethod.GET, new ConnectException("Connection refused"), (Long) null, REQUEST));

		assertThatThrownBy(() -> gateway.obterPorId(1L))
				.isInstanceOf(CampeonatoServiceIndisponivelException.class)
				.hasMessage(CampeonatoServiceIndisponivelException.MENSAGEM);

		assertThat(CampeonatoServiceIndisponivelException.MENSAGEM).doesNotContainIgnoringCase("localhost")
				.doesNotContainIgnoringCase("8083").doesNotContainIgnoringCase("connection");
	}
}
