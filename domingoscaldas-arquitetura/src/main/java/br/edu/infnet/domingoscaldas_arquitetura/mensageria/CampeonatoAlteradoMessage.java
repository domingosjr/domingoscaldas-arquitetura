package br.edu.infnet.domingoscaldas_arquitetura.mensageria;

import java.time.LocalDate;

/**
 * Mensagem recebida do campeonato-service quando um campeonato é alterado.
 * Mesmo formato publicado pelo produtor (o contrato entre os dois é o JSON).
 */
public record CampeonatoAlteradoMessage(Long campeonatoId, String nome, LocalDate data) {
}
