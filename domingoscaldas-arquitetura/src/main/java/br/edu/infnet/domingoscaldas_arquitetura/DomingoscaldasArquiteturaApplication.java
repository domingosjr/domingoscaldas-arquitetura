package br.edu.infnet.domingoscaldas_arquitetura;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DomingoscaldasArquiteturaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DomingoscaldasArquiteturaApplication.class, args);
	}

}
