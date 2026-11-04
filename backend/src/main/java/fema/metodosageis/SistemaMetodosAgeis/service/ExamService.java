package fema.metodosageis.SistemaMetodosAgeis.service;

import fema.metodosageis.SistemaMetodosAgeis.config.JwtService;
import fema.metodosageis.SistemaMetodosAgeis.dto.pergunta.AlternativaDTO;
import fema.metodosageis.SistemaMetodosAgeis.dto.pergunta.PerguntaDTO;
import fema.metodosageis.SistemaMetodosAgeis.dto.prova.FinalizarProvaDTO;
import fema.metodosageis.SistemaMetodosAgeis.dto.prova.ProvaDTO;
import fema.metodosageis.SistemaMetodosAgeis.dto.usuario.CriarUsuarioDTO;
import fema.metodosageis.SistemaMetodosAgeis.dto.usuario.UsuarioCriadoDTO;
import fema.metodosageis.SistemaMetodosAgeis.enums.*;
import fema.metodosageis.SistemaMetodosAgeis.model.*;
import fema.metodosageis.SistemaMetodosAgeis.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final ProvaRepository provaRepository;
    private final TentativaProvaRepository tentativaProvaRepository;
    private final PerguntaRepository perguntaRepository;
    private final AlternativaRepository alternativaRepository;
    private final RespostaRepository respostaRepository;


    private Long obterIdUsuarioLogado () {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null){
            return Long.parseLong(authentication.getName());
        }else {
            throw new IllegalArgumentException("Id de usuario não existe");
        }
    }


    @Transactional
    public UsuarioCriadoDTO iniciarProva (CriarUsuarioDTO dto){
        Prova prova = provaRepository.findById(1L).orElseThrow(() -> new EntityNotFoundException("Prova não encontrada"));
        if (prova.isAtiva()){
            Usuario usuario = new Usuario();
            usuario.setNome(dto.nome().toUpperCase());
            usuario.setEscolaridade(Escolaridade.valueOf(dto.escolaridade()));
            usuario.setExperiencia(Experiencia.valueOf(dto.experiencia()));
            usuario.setIdade(dto.idade());
            usuario.setSexo(Sexo.valueOf(dto.sexo()));
            usuario.setTipoUsuario(TipoUsuario.ALUNO);
            usuario = usuarioRepository.save(usuario);

            TentativaProva tentativaProva = new TentativaProva();
            tentativaProva.setUsuario(usuario);
            tentativaProva.setProva(prova);
            tentativaProva.setInicio(LocalDateTime.now());
            tentativaProva.setStatus(StatusProva.EM_ANDAMENTO);
            tentativaProva.setFim(null);
            tentativaProva = tentativaProvaRepository.save(tentativaProva);

            String token = jwtService.gerarTokenDefinitivo(usuario);
            return new UsuarioCriadoDTO(token, tentativaProva.getId());
        }else {
            throw new IllegalArgumentException("Impossivel iniciar essa prova, ela não está ativa");
        }
    }

    @Transactional
    public ProvaDTO carregarProva (Long idTentativa) {
        if (idTentativa == null || idTentativa <= 0){
            throw new IllegalArgumentException("Id inválido");
        }
        Long id = obterIdUsuarioLogado();
        TentativaProva tentativaProva = tentativaProvaRepository.findById(idTentativa).orElseThrow(() -> new EntityNotFoundException("Impossivel encontrar essa tentativa"));
        if (!tentativaProva.getUsuario().getId().equals(id)){
            throw new RuntimeException("Você não pertence a essa tentativa");
        }
        if (tentativaProva.getStatus() != StatusProva.EM_ANDAMENTO){
            throw new RuntimeException("essa prova ja foi expirada ou finalizada");
        }
        Prova prova = tentativaProva.getProva();
        List<Pergunta> perguntas = prova.getPerguntas();
        List<PerguntaDTO> dtoPerguntas = perguntas.stream().map(p -> {
            return new PerguntaDTO(p.getId(),p.getTitulo(),p.getEnunciado(), p.getAlternativas().stream().map(a -> new AlternativaDTO(a.getId(), a.getDescricao())).toList());
        }).toList();
        return new ProvaDTO(prova.getId(),prova.getTitulo(),prova.getDuracaoMinutos(),dtoPerguntas);
    }


    @Transactional
    public Long finalizarProva(FinalizarProvaDTO dto){
        if (dto.idTentativa() == null || dto.idTentativa() <= 0){
            throw new IllegalArgumentException("Id inválido");
        }
        Long id = obterIdUsuarioLogado();
        TentativaProva tentativaProva = tentativaProvaRepository.findById(dto.idTentativa()).orElseThrow(() -> new EntityNotFoundException("Impossivel encontrar essa tentativa"));
        if (!tentativaProva.getUsuario().getId().equals(id)){
            throw new AccessDeniedException("Você não pertence a essa tentativa");
        }
        if (tentativaProva.getStatus() != StatusProva.EM_ANDAMENTO){
            throw new IllegalArgumentException("Esta prova foi encerrada pelo organizador");
        }
        Prova prova = tentativaProva.getProva();
        for (Long idPergunta : dto.respostas().keySet()){
            Pergunta pergunta = perguntaRepository.findById(idPergunta).orElseThrow(() -> new EntityNotFoundException("Impossivel encontrar essa pergunta"));
            if (!prova.getPerguntas().contains(pergunta)){
                throw new IllegalArgumentException("Essa pergunta não pertence a essa prova");
            }
            Long idAlternativa = dto.respostas().get(idPergunta);
            Alternativa alternativa = alternativaRepository.findById(idAlternativa)
                    .orElseThrow(() -> new EntityNotFoundException("Impossível encontrar essa alternativa"));
            if(!pergunta.getAlternativas().contains(alternativa)){
                throw new IllegalArgumentException("Essa alternativa não pertence a essa pergunta");
            }

            Resposta resposta = new Resposta();
            resposta.setUsuario(usuarioRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Impossivel encontrar esse usuario")));
            resposta.setTentativaProva(tentativaProva);
            resposta.setPergunta(pergunta);
            resposta.setCorreta(alternativa.isCorreta());
            resposta.setRespondidoEm(LocalDateTime.now());
            resposta.setAlternativa(alternativa);
            respostaRepository.save(resposta);
        }
        tentativaProva.setStatus(StatusProva.FINALIZADA);
        tentativaProva.setFim(LocalDateTime.now());
        tentativaProvaRepository.save(tentativaProva);
        return respostaRepository.countByTentativaProvaAndCorretaIsTrue(tentativaProva);

    }

    public boolean isProvaAtiva () {
        Prova prova = provaRepository.findById(1L).orElseThrow(() -> new EntityNotFoundException("Prova não encontrada"));
        return prova.isAtiva();
    }

}
