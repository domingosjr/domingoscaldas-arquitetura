package br.edu.infnet.domingoscaldas_arquitetura.batch;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Executa o Job de verdade (com um banco H2 só deste teste, para não mexer nos
 * números dos outros testes): 69 linhas lidas, 9 ignoradas, 60 gravadas, e a
 * Beatriz passa a estar apta. Uma segunda execução não grava nada de novo.
 */
@SpringBootTest(properties = { "spring.datasource.url=jdbc:h2:mem:importacaotestdb",
		"spring.rabbitmq.listener.simple.auto-startup=false" })
@AutoConfigureMockMvc
class ImportacaoPresencasJobTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void importaAListaDePresencaEMudaOsAptos() throws Exception {
		mockMvc.perform(get("/graduacoes/alunos/2/pontos")).andExpect(content().string("5"));

		mockMvc.perform(post("/importacoes/presencas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.lidas").value(69))
				.andExpect(jsonPath("$.ignoradas").value(9))
				.andExpect(jsonPath("$.gravadas").value(60));

		// Beatriz: 5 + 55 presenças = 60 pontos, o mínimo da faixa azul
		mockMvc.perform(get("/graduacoes/alunos/2/pontos")).andExpect(content().string("60"));
		mockMvc.perform(get("/graduacoes/aptos"))
				.andExpect(jsonPath("$[*].nome", containsInAnyOrder("Anderson Souza", "Beatriz Lima")));

		// segunda execução: tudo já foi importado, nada é gravado de novo
		mockMvc.perform(post("/importacoes/presencas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lidas").value(69))
				.andExpect(jsonPath("$.ignoradas").value(69))
				.andExpect(jsonPath("$.gravadas").value(0));
	}
}
