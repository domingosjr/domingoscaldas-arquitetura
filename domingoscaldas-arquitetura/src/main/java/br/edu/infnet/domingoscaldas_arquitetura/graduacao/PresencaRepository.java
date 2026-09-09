package br.edu.infnet.domingoscaldas_arquitetura.graduacao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PresencaRepository extends JpaRepository<Presenca, Long> {

	List<Presenca> findByAlunoId(Long alunoId);

	List<Presenca> findByDataBetween(LocalDate inicio, LocalDate fim);

	long countByAlunoId(Long alunoId);

	long countByAlunoIdAndDataAfter(Long alunoId, LocalDate data);
}
