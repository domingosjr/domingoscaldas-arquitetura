package br.edu.infnet.domingoscaldas_arquitetura.campeonato;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CampeonatoRepository extends JpaRepository<Campeonato, Long> {

	List<Campeonato> findAllByOrderByDataDesc();
}
