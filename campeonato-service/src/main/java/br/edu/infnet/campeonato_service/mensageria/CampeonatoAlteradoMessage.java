package br.edu.infnet.campeonato_service.mensageria;

import java.time.LocalDate;

/**
 * Mensagem publicada quando um campeonato é alterado. Leva só o que os
 * consumidores precisam: o identificador e os dados que eles guardam em cópia.
 */
public record CampeonatoAlteradoMessage(Long campeonatoId, String nome, LocalDate data) {
}
