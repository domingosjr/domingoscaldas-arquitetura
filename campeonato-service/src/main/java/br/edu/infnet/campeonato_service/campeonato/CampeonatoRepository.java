package br.edu.infnet.campeonato_service.campeonato;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CampeonatoRepository extends JpaRepository<Campeonato, Long> {

	List<Campeonato> findAllByOrderByDataDesc();
}
