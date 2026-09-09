package br.edu.infnet.domingoscaldas_arquitetura.aluno;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

	List<Aluno> findByAtivoTrue();

	List<Aluno> findByNomeContainingIgnoreCase(String nome);

	List<Aluno> findByFaixa(Faixa faixa);

	List<Aluno> findAllByOrderByNomeAsc();
}
