package br.edu.infnet.domingoscaldas_arquitetura.turma.dto;

import br.edu.infnet.domingoscaldas_arquitetura.aluno.Faixa;

/**
 * Visão resumida do aluno que o contexto turma expõe no seu contrato de
 * resposta (não é o DTO oficial do módulo aluno).
 */
public record AlunoResumoResponse(Long id, String nome, String email, Faixa faixa) {
}
