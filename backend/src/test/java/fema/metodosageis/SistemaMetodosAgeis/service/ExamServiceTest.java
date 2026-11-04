package fema.metodosageis.SistemaMetodosAgeis.service;


import fema.metodosageis.SistemaMetodosAgeis.config.JwtService;
import fema.metodosageis.SistemaMetodosAgeis.dto.pergunta.AlternativaDTO;
import fema.metodosageis.SistemaMetodosAgeis.dto.pergunta.PerguntaDTO;
import fema.metodosageis.SistemaMetodosAgeis.dto.prova.FinalizarProvaDTO;
import fema.metodosageis.SistemaMetodosAgeis.dto.prova.ProvaDTO;
import fema.metodosageis.SistemaMetodosAgeis.dto.usuario.CriarUsuarioDTO;
import fema.metodosageis.SistemaMetodosAgeis.dto.usuario.UsuarioCriadoDTO;
import fema.metodosageis.SistemaMetodosAgeis.enums.StatusProva;
import fema.metodosageis.SistemaMetodosAgeis.model.*;
import fema.metodosageis.SistemaMetodosAgeis.repository.ProvaRepository;
import fema.metodosageis.SistemaMetodosAgeis.repository.TentativaProvaRepository;
import fema.metodosageis.SistemaMetodosAgeis.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.access.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    ProvaRepository provaRepository;

    @Mock
    UsuarioRepository usuarioRepository;

    @Mock
    TentativaProvaRepository tentativaProvaRepository;

    @Mock
    JwtService jwtService;

    @InjectMocks
    ExamService examService;

    CriarUsuarioDTO dto;


    @BeforeEach
    void setUp() {
        dto = new CriarUsuarioDTO("teste", 18, "OUTRO", "SUPERIOR", "NENHUMA");
    }

    @AfterEach
    void tearDown() {SecurityContextHolder.clearContext();}

    @Test
    void isProvaAtiva_deveRetornarTrueQuandoProvaAtiva() {
        Prova prova = new Prova();
        prova.setAtiva(true);
        when(provaRepository.findById(1L)).thenReturn(Optional.of(prova));
        boolean provaAtiva = examService.isProvaAtiva();
        assertTrue(provaAtiva);
    }

    @Test
    void isProvaAtiva_deveRetornarFalseQuandoProvaNaoAtiva() {
        Prova prova = new Prova();
        prova.setAtiva(false);
        when(provaRepository.findById(1L)).thenReturn(Optional.of(prova));
        boolean provaAtiva = examService.isProvaAtiva();
        assertFalse(provaAtiva);
    }

    @Test
    void isProvaAtiva_deveLancarEntityNotFoundQuandoNaoEncontrarProva(){
        when(provaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> examService.isProvaAtiva());
    }

    @Test
    void iniciarProva_deveLancarEntityNotFoundQuandoNaoEncontrarProva(){
        when(provaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> examService.iniciarProva(dto));
        verify(usuarioRepository, never()).save(any());
        verify(tentativaProvaRepository, never()).save(any());
    }

    @Test
    void iniciarProva_deveLancarIllegalArgumentQuandoProvaInativa () {

        Prova prova = new Prova();
        prova.setAtiva(false);

        when(provaRepository.findById(1L)).thenReturn(Optional.of(prova));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> examService.iniciarProva(dto));
        assertEquals("Impossivel iniciar essa prova, ela não está ativa", exception.getMessage());

        verify(usuarioRepository, never()).save(any());
        verify(tentativaProvaRepository, never()).save(any());
    }


    @Test
    void iniciarProva_deveRetornarTokenEIdTentativaQuandoProvaAtiva () {

        Prova prova = new Prova();
        prova.setAtiva(true);
        when(provaRepository.findById(1L)).thenReturn(Optional.of(prova));

        Usuario usuario = new Usuario();
        when(usuarioRepository.save(any())).thenReturn(usuario);

        TentativaProva tentativa = new TentativaProva();
        tentativa.setId(10L);
        when(tentativaProvaRepository.save(any())).thenReturn(tentativa);

        when(jwtService.gerarTokenDefinitivo(any())).thenReturn("token definitivo");

        UsuarioCriadoDTO dtoResponse = examService.iniciarProva(dto);

        //preciso rodar o metodo e só depois chamar verify e assertions
        verify(usuarioRepository).save(any());
        verify(tentativaProvaRepository).save(any());

        assertNotNull(dtoResponse);

        //lembrar sempre (esperado,obtido)
        assertEquals(tentativa.getId(), dtoResponse.idTentativa());
        assertEquals("token definitivo", dtoResponse.token() );
    }

    @ParameterizedTest //teste parametrizado
    @ValueSource(longs = {-1,0}) //testar uma vez com " -1 " e outra vez com " 0 "
    @NullSource //pra testar uma vez com null
    void carregarProva_deveLancarIllegalArgumentQuandoIdInvalido (Long id) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> examService.carregarProva(id));
        assertEquals("Id inválido", exception.getMessage());
        verify(tentativaProvaRepository, never()).findById(any());
    }

    @Test
    void carregarProva_deveLancarIllegalArgumentQuandoUsuarioNaoLogado () {
      IllegalArgumentException exception =  assertThrows(IllegalArgumentException.class, () -> examService.carregarProva(1L));
      assertEquals("Id de usuario não existe", exception.getMessage());
      verify(tentativaProvaRepository, never()).findById(any());
    }

    @Test
    void carregarProva_deveLancarEntityNotFoundQuandoTentativaNaoEncontrada () {
        var token = new UsernamePasswordAuthenticationToken("1", null); // instancio um token interno do spring security
        SecurityContextHolder.getContext().setAuthentication(token); //depois adiciono esse token falso no context holder

        when(tentativaProvaRepository.findById(1L)).thenReturn(Optional.empty()); //quando chamar tentativa prova por esse id "1" retorne um optional vazio
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> examService.carregarProva(1L)); //verificar se a exception foi lançada nesse metodo
        verify(tentativaProvaRepository).findById(1L); // verificar se o find by id foi chamado com esse parametro
        assertEquals("Impossivel encontrar essa tentativa", exception.getMessage()); // compara a mensagem da exception, se foi essa mesma que lançou
    }


    @Test
    void carregarProva_deveLancarRunTimeExceptionQuandoUsuarioNaoForDonoDaTentativa(){
        var token = new UsernamePasswordAuthenticationToken("1", null);
        SecurityContextHolder.getContext().setAuthentication(token);

        Usuario usuario = new Usuario();
        usuario.setId(2L);

        TentativaProva tentativa = new TentativaProva();
        tentativa.setUsuario(usuario);

        when(tentativaProvaRepository.findById(1L)).thenReturn(Optional.of(tentativa));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> examService.carregarProva(1L)); //assertThrows permite classes filhas
        //verifico se a mensagem do erro é a mesma que a do service
        assertEquals("Você não pertence a essa tentativa", exception.getMessage()); // por isso se faz importante o uso do assertEquals para garantir que o erro é esse mesmo

        //verifico se o findById foi chamado com esse exato parametro
        verify(tentativaProvaRepository).findById(1L);
    }

    @ParameterizedTest
    @EnumSource(value = StatusProva.class, names = {"FINALIZADA", "EXPIRADA"})
    void carregarProva_deveLancarRuntTimeExceptionQuandoStatusForDiferenteDeEmAndamento (StatusProva statusProva) {
        var token = new UsernamePasswordAuthenticationToken("1", null);
        SecurityContextHolder.getContext().setAuthentication(token);

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        TentativaProva tentativa = new TentativaProva();
        tentativa.setUsuario(usuario);
        tentativa.setStatus(statusProva);

        when(tentativaProvaRepository.findById(1L)).thenReturn(Optional.of(tentativa));

       RuntimeException exception = assertThrows(RuntimeException.class, () -> examService.carregarProva(1L));
       assertEquals("essa prova ja foi expirada ou finalizada",  exception.getMessage());

        //verifico se o findById foi chamado com esse exato parametro
        verify(tentativaProvaRepository).findById(1L);
    }

    @Test
    void carregarProva_deveRetornarProvaDtoQuandoTudoOcorrerCerto (){
        var token = new UsernamePasswordAuthenticationToken("1", null);
        SecurityContextHolder.getContext().setAuthentication(token);

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Prova prova = new Prova();
        prova.setId(1L);
        prova.setTitulo("Prova Teste");
        prova.setDuracaoMinutos(45);

        TentativaProva tentativa = new TentativaProva();
        tentativa.setUsuario(usuario);
        tentativa.setStatus(StatusProva.EM_ANDAMENTO);
        tentativa.setProva(prova);
        List<Alternativa> alternativas = List.of(new Alternativa(1L, "descricao alternativa teste"));
        List<Pergunta> perguntas = List.of(new Pergunta(1L, "titulo teste", "enunciado teste", alternativas ));
        tentativa.getProva().setPerguntas(perguntas);

        ProvaDTO esperado = new ProvaDTO(1L, "Prova Teste", 45,
                List.of(new PerguntaDTO(1L, "titulo teste", "enunciado teste",
                        List.of(new AlternativaDTO(1L, "descricao alternativa teste")))));

        when(tentativaProvaRepository.findById(1L)).thenReturn(Optional.of(tentativa));

        ProvaDTO obtido = examService.carregarProva(1L);

        assertEquals(esperado,obtido);

        verify(tentativaProvaRepository).findById(1L);

    }

    @ParameterizedTest
    @ValueSource(longs = {-1,0})
    @NullSource
    void finalizarProva_deveLancarIllegalArgumentQuandoIdInvalido (Long id) {
        FinalizarProvaDTO request = new FinalizarProvaDTO(id, null);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> examService.finalizarProva(request));
        assertEquals("Id inválido", exception.getMessage());
        verify(tentativaProvaRepository, never()).findById(any());
    }


    @Test
    void finalizarProva_deveLancarIllegalArgumentQuandoUsuarioNaoLogado () {
        FinalizarProvaDTO request = new FinalizarProvaDTO(1L, null);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> examService.finalizarProva(request));
        assertEquals("Id de usuario não existe", exception.getMessage());
        verify(tentativaProvaRepository, never()).findById(any());
    }

    @Test
    void finalizarProva_deveLancarEntityNotFoundQuandoTentativaNaoEncontrada () {
        var token = new UsernamePasswordAuthenticationToken("1", null);
        SecurityContextHolder.getContext().setAuthentication(token); //mocko um falso user logado

        FinalizarProvaDTO request = new FinalizarProvaDTO(10L, null);

        when(tentativaProvaRepository.findById(10L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> examService.finalizarProva(request));
        assertEquals("Impossivel encontrar essa tentativa", exception.getMessage());
        verify(tentativaProvaRepository).findById(any());
    }

    @Test
    void finalizarProva_deveLancarAccessDeniedQuandoUsuarioLogadoNaoParticipaDaTentativa () {
        var token = new UsernamePasswordAuthenticationToken("1", null);
        SecurityContextHolder.getContext().setAuthentication(token);

        FinalizarProvaDTO request = new FinalizarProvaDTO(1L, null);

        TentativaProva tentativa = new TentativaProva();
        Usuario usuario = new Usuario();
        usuario.setId(2L);
        tentativa.setUsuario(usuario);

        when(tentativaProvaRepository.findById(1L)).thenReturn(Optional.of(tentativa)); //imports diferentes da mesma exception podem dar erro
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> examService.finalizarProva(request));
        assertEquals("Você não pertence a essa tentativa", exception.getMessage());
        verify(tentativaProvaRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusProva.class, names = "EM_ANDAMENTO", mode = EnumSource.Mode.EXCLUDE)
    void finalizarProva_deveLancarIllegalArgumentQuandoStatusTentativaForDiferenteDeEmAndamento (StatusProva statusProva) {
        var token = new UsernamePasswordAuthenticationToken("1", null);
        SecurityContextHolder.getContext().setAuthentication(token);

        FinalizarProvaDTO request = new FinalizarProvaDTO(1L, null);

        TentativaProva tentativa = new TentativaProva();
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        tentativa.setUsuario(usuario);
        tentativa.setStatus(statusProva);

        when(tentativaProvaRepository.findById(1L)).thenReturn(Optional.of(tentativa));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> examService.finalizarProva(request));
        assertEquals("Esta prova foi encerrada pelo organizador", exception.getMessage());

        verify(tentativaProvaRepository, never()).save(any());
    }
}