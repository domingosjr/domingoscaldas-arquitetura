package br.edu.infnet.campeonato_service.campeonato.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Dados de um campeonato expostos pela API (nunca a entidade JPA).
 */
@Schema(description = "Campeonato cadastrado")
public record CampeonatoResponse(

		@Schema(description = "Identificador do campeonato", example = "1") Long id,

		@Schema(description = "Nome do campeonato", example = "Copa Rio de Jiu-Jitsu") String nome,

		@Schema(description = "Cidade do evento", example = "Rio de Janeiro") String cidade,

		@Schema(description = "CEP do local, quando informado", example = "20271130", nullable = true) String cep,

		@Schema(description = "Data do evento", example = "2026-05-17") LocalDate data) {
}
