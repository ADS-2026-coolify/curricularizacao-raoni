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
public class Alternativa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 5)
    private String descricao;

    private boolean correta;

    @ManyToOne
    @JoinColumn(name = "pergunta_ID")
    private Pergunta pergunta;

    @OneToMany(mappedBy = "alternativa")
    private List<Resposta> respostas;

    public Alternativa(long id, String descricaoAlternativaTeste) {
        this.id = id;
        this.descricao = descricaoAlternativaTeste;
    }
}
