package br.edu.infnet.campeonato_service.campeonato.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada para cadastrar ou alterar um campeonato.
 */
@Schema(description = "Dados de um campeonato a cadastrar ou alterar")
public record CampeonatoRequest(

		@Schema(description = "Nome do campeonato", example = "Copa Rio de Jiu-Jitsu")
		@NotBlank(message = "O nome é obrigatório")
		@Size(max = 120, message = "O nome deve possuir no máximo 120 caracteres")
		String nome,

		@Schema(description = "Cidade do evento; é substituída pela cidade do CEP quando o CEP é informado",
				example = "Rio de Janeiro")
		@Size(max = 80, message = "A cidade deve possuir no máximo 80 caracteres")
		String cidade,

		@Schema(description = "CEP do local (8 dígitos, opcional). Quando informado, a cidade vem do ViaCEP",
				example = "20271130")
		@Pattern(regexp = "\\d{8}", message = "O CEP deve possuir exatamente 8 dígitos numéricos")
		String cep,

		@Schema(description = "Data do evento", example = "2026-05-17")
		@NotNull(message = "A data é obrigatória")
		LocalDate data) {
}
