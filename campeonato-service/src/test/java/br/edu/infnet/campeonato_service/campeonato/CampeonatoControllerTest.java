package br.edu.infnet.campeonato_service.campeonato;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.infnet.campeonato_service.endereco.Endereco;
import br.edu.infnet.campeonato_service.endereco.client.ViaCepClient;
import br.edu.infnet.campeonato_service.mensageria.CampeonatoAlteradoMessage;
import br.edu.infnet.campeonato_service.mensageria.CampeonatoAlteradoProducer;
import feign.Request;
import feign.RetryableException;

/**
 * Contrato HTTP do serviço. O ViaCEP é substituído por um mock: o teste não
 * depende da internet.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CampeonatoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ViaCepClient viaCepClient;

	@MockitoBean
	private CampeonatoAlteradoProducer campeonatoAlteradoProducer;

	@Test
	void campeonatoDoSeedDeveSerObtidoPorId() throws Exception {
		mockMvc.perform(get("/campeonatos/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.nome").value("Copa Rio de Jiu-Jitsu"))
				.andExpect(jsonPath("$.cidade").value("Rio de Janeiro"))
				.andExpect(jsonPath("$.data").value("2026-05-17"));
	}

	@Test
	void campeonatoInexistenteDeve404ComErroResponse() throws Exception {
		mockMvc.perform(get("/campeonatos/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.erro").value("Not Found"))
				.andExpect(jsonPath("$.mensagem").value("Campeonato não encontrado: 999"));
	}

	@Test
	void campeonatoSemNomeESemDataDeve400() throws Exception {
		mockMvc.perform(post("/campeonatos").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("data: A data é obrigatória; nome: O nome é obrigatório"));
	}

	@Test
	void cadastroComCepPreencheACidadePeloViaCepEExcluiDepois() throws Exception {
		Endereco endereco = new Endereco();
		endereco.setLocalidade("Rio de Janeiro");
		endereco.setUf("RJ");
		when(viaCepClient.consultarCep("20271130")).thenReturn(endereco);

		String corpo = mockMvc.perform(post("/campeonatos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\": \"Rio Open\", \"cep\": \"20271130\", \"data\": \"2026-11-07\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cidade").value("Rio de Janeiro - RJ"))
				.andReturn().getResponse().getContentAsString();

		String id = corpo.replaceAll(".*\"id\":(\\d+).*", "$1");

		mockMvc.perform(delete("/campeonatos/" + id)).andExpect(status().isNoContent());
		mockMvc.perform(get("/campeonatos/" + id)).andExpect(status().isNotFound());
	}

	@Test
	void alterarCampeonatoPublicaMensagemComNomeEData() throws Exception {
		String corpo = mockMvc.perform(post("/campeonatos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\": \"Open Niteroi\", \"cidade\": \"Niteroi\", \"data\": \"2026-08-01\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Long id = Long.valueOf(corpo.replaceAll(".*\"id\":(\\d+).*", "$1"));

		mockMvc.perform(put("/campeonatos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\": \"Open Niteroi 2026\", \"cidade\": \"Niteroi\", \"data\": \"2026-08-02\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Open Niteroi 2026"));

		verify(campeonatoAlteradoProducer)
				.enviar(new CampeonatoAlteradoMessage(id, "Open Niteroi 2026", java.time.LocalDate.of(2026, 8, 2)));

		mockMvc.perform(delete("/campeonatos/" + id)).andExpect(status().isNoContent());
	}

	@Test
	void cepInexistenteDeve404() throws Exception {
		Endereco inexistente = new Endereco();
		inexistente.setErro(true);
		when(viaCepClient.consultarCep("99999999")).thenReturn(inexistente);

		mockMvc.perform(get("/enderecos/99999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensagem").value("Nenhum endereço encontrado para o CEP: 99999999"));
	}

	@Test
	void viaCepForaDoArDeve503SemDetalhesTecnicos() throws Exception {
		Request request = Request.create(Request.HttpMethod.GET, "https://viacep.com.br/ws/20271130/json", Map.of(),
				(byte[]) null, StandardCharsets.UTF_8, null);
		when(viaCepClient.consultarCep("20271130")).thenThrow(new RetryableException(-1, "Connection refused",
				Request.HttpMethod.GET, new ConnectException("Connection refused"), (Long) null, request));

		mockMvc.perform(post("/campeonatos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\": \"Rio Open\", \"cep\": \"20271130\", \"data\": \"2026-11-07\"}"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value(503))
				.andExpect(jsonPath("$.mensagem")
						.value("A consulta de CEP está temporariamente indisponível. Tente novamente em instantes."));
	}
}
