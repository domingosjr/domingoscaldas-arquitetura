package br.edu.infnet.domingoscaldas_arquitetura.turma.dto;

import java.util.List;

/**
 * Contrato de resposta do módulo turma (a entidade não é exposta).
 */
public record TurmaResponse(Long id, String nome, String horario, boolean ativa, List<AlunoResumoResponse> alunos) {
}
