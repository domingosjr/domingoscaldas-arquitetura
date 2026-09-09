package br.edu.infnet.domingoscaldas_arquitetura.campeonato;

import java.util.List;

import org.springframework.stereotype.Service;

import br.edu.infnet.domingoscaldas_arquitetura.endereco.Endereco;
import br.edu.infnet.domingoscaldas_arquitetura.endereco.EnderecoService;

/**
 * Regras de negócio dos campeonatos. Quando o CEP é informado, a cidade é
 * preenchida pelo módulo endereço (colaboração via EnderecoService).
 */
@Service
public class CampeonatoService {

	private final CampeonatoRepository campeonatoRepository;
	private final EnderecoService enderecoService;

	public CampeonatoService(CampeonatoRepository campeonatoRepository, EnderecoService enderecoService) {
		this.campeonatoRepository = campeonatoRepository;
		this.enderecoService = enderecoService;
	}

	public Campeonato incluir(Campeonato campeonato) {
		preencherCidadePeloCep(campeonato);

		return campeonatoRepository.save(campeonato);
	}

	public List<Campeonato> obterLista() {
		return campeonatoRepository.findAllByOrderByDataDesc();
	}

	public Campeonato obterPorId(Long id) {
		return campeonatoRepository.findById(id).orElseThrow(() -> new CampeonatoNaoEncontradoException(id));
	}

	public Campeonato alterar(Long id, Campeonato campeonato) {
		preencherCidadePeloCep(campeonato);

		Campeonato existente = obterPorId(id);
		existente.setNome(campeonato.getNome());
		existente.setCidade(campeonato.getCidade());
		existente.setCep(campeonato.getCep());
		existente.setData(campeonato.getData());

		return campeonatoRepository.save(existente);
	}

	public void excluir(Long id) {
		Campeonato existente = obterPorId(id);

		campeonatoRepository.delete(existente);
	}

	private void preencherCidadePeloCep(Campeonato campeonato) {

		if (campeonato.getCep() == null || campeonato.getCep().isBlank()) {
			return;
		}

		Endereco endereco = enderecoService.consultarPorCep(campeonato.getCep());
		campeonato.setCidade(endereco.getLocalidade() + " - " + endereco.getUf());
	}
}
