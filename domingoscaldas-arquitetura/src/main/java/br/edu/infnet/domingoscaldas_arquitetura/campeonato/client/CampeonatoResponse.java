package br.edu.infnet.domingoscaldas_arquitetura.campeonato.client;

import java.time.LocalDate;

/**
 * Dados do campeonato devolvidos pelo campeonato-service — apenas o que a
 * aplicação principal precisa.
 */
public record CampeonatoResponse(Long id, String nome, String cidade, LocalDate data) {
}
