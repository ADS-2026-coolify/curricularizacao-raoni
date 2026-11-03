package fema.metodosageis.SistemaMetodosAgeis.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@EqualsAndHashCode
public class Pergunta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 5)
    private String titulo;

    @NotBlank
    @Size(min = 10)
    private String enunciado;

    @ManyToOne
    @JoinColumn(name = "prova_ID")
    private Prova prova;

    @OneToMany(mappedBy = "pergunta")
    private List<Alternativa> alternativas;

    @OneToMany (mappedBy = "pergunta")
    private List<Resposta> respostas;

    public Pergunta( Long i, String tituloTeste, String enunciadoTeste, List<Alternativa> alternativas) {
        this.id = i;
        this.titulo = tituloTeste;
        this.enunciado = enunciadoTeste;
        this.alternativas = alternativas;
    }
}
