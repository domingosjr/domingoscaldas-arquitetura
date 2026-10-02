package br.edu.infnet.domingoscaldas_arquitetura.batch;

/**
 * Resultado de uma execução da importação de presenças.
 */
public record ImportacaoResponse(Long execucaoId, String status, long lidas, long ignoradas, long gravadas) {
}
