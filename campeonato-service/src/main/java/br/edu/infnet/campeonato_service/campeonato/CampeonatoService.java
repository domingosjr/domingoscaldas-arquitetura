package br.edu.infnet.campeonato_service.campeonato;

import java.util.List;

import org.springframework.stereotype.Service;

import br.edu.infnet.campeonato_service.campeonato.dto.CampeonatoRequest;
import br.edu.infnet.campeonato_service.campeonato.dto.CampeonatoResponse;
import br.edu.infnet.campeonato_service.endereco.Endereco;
import br.edu.infnet.campeonato_service.endereco.EnderecoService;

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

	public CampeonatoResponse incluir(CampeonatoRequest request) {
		Campeonato campeonato = new Campeonato(request.nome(), obterCidade(request), request.cep(), request.data());

		return converterParaResponse(campeonatoRepository.save(campeonato));
	}

	public List<CampeonatoResponse> obterLista() {
		return campeonatoRepository.findAllByOrderByDataDesc().stream().map(this::converterParaResponse).toList();
	}

	public CampeonatoResponse obterPorId(Long id) {
		return converterParaResponse(obterEntidade(id));
	}

	public CampeonatoResponse alterar(Long id, CampeonatoRequest request) {
		Campeonato existente = obterEntidade(id);
		existente.setNome(request.nome());
		existente.setCidade(obterCidade(request));
		existente.setCep(request.cep());
		existente.setData(request.data());

		return converterParaResponse(campeonatoRepository.save(existente));
	}

	public void excluir(Long id) {
		campeonatoRepository.delete(obterEntidade(id));
	}

	/** Cidade informada ou, quando há CEP, a cidade devolvida pelo ViaCEP. */
	private String obterCidade(CampeonatoRequest request) {

		if (request.cep() == null || request.cep().isBlank()) {
			return request.cidade();
		}

		Endereco endereco = enderecoService.consultarPorCep(request.cep());

		return endereco.getLocalidade() + " - " + endereco.getUf();
	}

	private Campeonato obterEntidade(Long id) {
		return campeonatoRepository.findById(id).orElseThrow(() -> new CampeonatoNaoEncontradoException(id));
	}

	private CampeonatoResponse converterParaResponse(Campeonato campeonato) {
		return new CampeonatoResponse(campeonato.getId(), campeonato.getNome(), campeonato.getCidade(),
				campeonato.getCep(), campeonato.getData());
	}
}
