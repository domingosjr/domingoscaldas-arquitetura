package br.edu.infnet.domingoscaldas_arquitetura.turma;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

	List<Turma> findByAtivaTrue();
}
