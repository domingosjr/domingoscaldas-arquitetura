package br.edu.infnet.domingoscaldas_arquitetura.conquista;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConquistaRepository extends JpaRepository<Conquista, Long> {

	List<Conquista> findByAlunoId(Long alunoId);

	List<Conquista> findByMedalha(Medalha medalha);

	List<Conquista> findByCampeonatoId(Long campeonatoId);
}
