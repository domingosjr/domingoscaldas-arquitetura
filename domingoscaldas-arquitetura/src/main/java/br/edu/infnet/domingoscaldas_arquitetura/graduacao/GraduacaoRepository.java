package br.edu.infnet.domingoscaldas_arquitetura.graduacao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GraduacaoRepository extends JpaRepository<Graduacao, Long> {

	List<Graduacao> findByAlunoIdOrderByDataDesc(Long alunoId);

	Optional<Graduacao> findTopByAlunoIdOrderByDataDesc(Long alunoId);
}
