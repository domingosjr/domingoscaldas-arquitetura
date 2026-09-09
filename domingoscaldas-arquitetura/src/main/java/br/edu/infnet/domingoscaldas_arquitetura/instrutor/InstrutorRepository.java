package br.edu.infnet.domingoscaldas_arquitetura.instrutor;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrutorRepository extends JpaRepository<Instrutor, Long> {

	List<Instrutor> findByAtivoTrue();
}
