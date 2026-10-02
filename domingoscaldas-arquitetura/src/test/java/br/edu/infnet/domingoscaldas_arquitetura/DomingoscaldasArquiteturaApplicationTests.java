package br.edu.infnet.domingoscaldas_arquitetura;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class DomingoscaldasArquiteturaApplicationTests {

	@Test
	void contextLoads() {
	}

}
