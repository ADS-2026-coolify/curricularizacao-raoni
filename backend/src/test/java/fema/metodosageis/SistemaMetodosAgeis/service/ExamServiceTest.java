package fema.metodosageis.SistemaMetodosAgeis.service;


import fema.metodosageis.SistemaMetodosAgeis.config.JwtService;
import fema.metodosageis.SistemaMetodosAgeis.dto.usuario.CriarUsuarioDTO;
import fema.metodosageis.SistemaMetodosAgeis.dto.usuario.UsuarioCriadoDTO;
import fema.metodosageis.SistemaMetodosAgeis.model.Prova;
import fema.metodosageis.SistemaMetodosAgeis.model.TentativaProva;
import fema.metodosageis.SistemaMetodosAgeis.model.Usuario;
import fema.metodosageis.SistemaMetodosAgeis.repository.ProvaRepository;
import fema.metodosageis.SistemaMetodosAgeis.repository.TentativaProvaRepository;
import fema.metodosageis.SistemaMetodosAgeis.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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


}