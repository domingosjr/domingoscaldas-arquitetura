package br.edu.infnet.domingoscaldas_arquitetura.mensageria;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O consumidor atualiza o nome e a data do campeonato copiados nas conquistas.
 * Aqui a mensagem é entregue chamando o método direto (sem broker); o caminho
 * pela fila real é demonstrado no Docker Compose.
 */
@SpringBootTest(properties = { "spring.datasource.url=jdbc:h2:mem:consumidortestdb",
		"spring.rabbitmq.listener.simple.auto-startup=false" })
@AutoConfigureMockMvc
class CampeonatoAlteradoConsumerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CampeonatoAlteradoConsumer consumer;

	@Test
	void atualizaAsCopiasDoCampeonatoNasConquistas() throws Exception {
		mockMvc.perform(get("/conquistas/1"))
				.andExpect(jsonPath("$.campeonatoNome").value("Copa Rio de Jiu-Jitsu"))
				.andExpect(jsonPath("$.campeonatoData").value("2026-05-17"));

		consumer.receber(new CampeonatoAlteradoMessage(1L, "Copa Rio de Jiu-Jitsu 2026", LocalDate.of(2026, 5, 18)));

		// as 3 conquistas do seed são da Copa Rio (campeonato 1)
		for (int id = 1; id <= 3; id++) {
			mockMvc.perform(get("/conquistas/" + id))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.campeonatoNome").value("Copa Rio de Jiu-Jitsu 2026"))
					.andExpect(jsonPath("$.campeonatoData").value("2026-05-18"));
		}
	}
}
