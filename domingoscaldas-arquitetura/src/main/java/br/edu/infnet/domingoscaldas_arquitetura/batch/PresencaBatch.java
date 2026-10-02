package br.edu.infnet.domingoscaldas_arquitetura.batch;

/**
 * Uma linha do arquivo CSV da lista de presença, do jeito que foi lida
 * (ainda sem validação: a data vem como texto e o tipo de treino sem padrão).
 */
public record PresencaBatch(Long alunoId, String data, String tipoTreino) {
}
