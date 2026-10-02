package br.edu.infnet.domingoscaldas_arquitetura.batch;

import java.time.LocalDate;

/**
 * Presença validada e normalizada pelo processor, pronta para ser gravada.
 */
public record PresencaImportada(Long alunoId, LocalDate data, String tipoTreino) {
}
