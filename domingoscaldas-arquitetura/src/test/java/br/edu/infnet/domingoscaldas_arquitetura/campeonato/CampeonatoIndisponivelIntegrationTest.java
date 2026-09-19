package br.edu.infnet.domingoscaldas_arquitetura.campeonato;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.infnet.domingoscaldas_arquitetura.campeonato.exception.CampeonatoServiceIndisponivelException;

/**
 * campeonato-service DESLIGADO: a URL aponta para uma porta local livre
 * (reservada e fechada antes do teste), então a conexão é recusada. Usa o
 * OpenFeign de verdade. Só o que depende do serviço responde 503; o resto da
 * aplicação — inclusive a regra de pontos — continua funcionando.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CampeonatoIndisponivelIntegrationTest {

	@DynamicPropertySource
	static void servicoDesligado(DynamicPropertyRegistry registry) {
		registry.add("campeonato.service.url", () -> "http://127.0.0.1:" + portaLivreFechada());
	}

	private static int portaLivreFechada() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void registrarConquistaDeve503ENaoGravarNada() throws Exception {
		mockMvc.perform(post("/conquistas/alunos/1/campeonatos/1").contentType(MediaType.APPLICATION_JSON)
				.content("{\"categoria\": \"Adulto Azul Leve\", \"medalha\": \"PRATA\"}"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value(503))
				.andExpect(jsonPath("$.erro").value("Service Unavailable"))
				.andExpect(jsonPath("$.mensagem").value(CampeonatoServiceIndisponivelException.MENSAGEM));

		mockMvc.perform(get("/conquistas")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
	}

	@Test
	void detalhesDaConquistaDeve503() throws Exception {
		mockMvc.perform(get("/conquistas/1/detalhes")).andExpect(status().isServiceUnavailable());
	}

	@Test
	void restoDaAplicacaoContinuaDisponivel() throws Exception {
		mockMvc.perform(get("/alunos/1")).andExpect(status().isOk());
		mockMvc.perform(get("/turmas")).andExpect(status().isOk());
		mockMvc.perform(get("/conquistas/1")).andExpect(status().isOk())
				.andExpect(jsonPath("$.campeonatoNome").value("Copa Rio de Jiu-Jitsu"));

		// a regra de pontos usa a data do campeonato copiada na conquista: não depende do serviço
		mockMvc.perform(get("/graduacoes/alunos/1/pontos")).andExpect(status().isOk()).andExpect(content().string("68"));
		mockMvc.perform(get("/graduacoes/aptos")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void alunoInexistenteDeve404LocalAntesDaChamadaRemota() throws Exception {
		mockMvc.perform(post("/conquistas/alunos/999/campeonatos/1").contentType(MediaType.APPLICATION_JSON)
				.content("{\"categoria\": \"Adulto\", \"medalha\": \"OURO\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensagem").value("Aluno não encontrado: 999"));
	}
}
