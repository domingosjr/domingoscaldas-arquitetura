package br.edu.infnet.config_server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O servidor sobe e serve as configurações de cada aplicação por profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConfigServerApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void serveAConfiguracaoDaPrincipalEmProdComONomeDoServicoNaRede() throws Exception {
		mockMvc.perform(get("/domingoscaldas-arquitetura/prod"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("domingoscaldas-arquitetura"))
				.andExpect(jsonPath("$.propertySources[0].source['campeonato.service.url']")
						.value("http://campeonato-service:8083"))
				.andExpect(jsonPath("$.propertySources[0].source['server.port']").value("8080"));
	}

	@Test
	void serveAConfiguracaoDoCampeonatoServiceEmDev() throws Exception {
		mockMvc.perform(get("/campeonato-service/dev"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.propertySources[0].source['server.port']").value("8083"));
	}
}
