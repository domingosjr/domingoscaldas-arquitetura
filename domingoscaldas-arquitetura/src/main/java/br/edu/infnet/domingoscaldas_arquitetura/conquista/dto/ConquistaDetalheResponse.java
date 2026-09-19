package br.edu.infnet.domingoscaldas_arquitetura.conquista.dto;

import java.time.LocalDate;

import br.edu.infnet.domingoscaldas_arquitetura.conquista.Medalha;

/**
 * Conquista com os dados ATUAIS do campeonato, consultados no
 * campeonato-service no momento da requisição.
 */
public record ConquistaDetalheResponse(Long id, String categoria, Medalha medalha, Long alunoId, String alunoNome,
		Long campeonatoId, String campeonatoNome, String campeonatoCidade, LocalDate campeonatoData) {
}
