    package fema.metodosageis.SistemaMetodosAgeis.model;

    import jakarta.persistence.*;
    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.NotNull;
    import jakarta.validation.constraints.Size;
    import lombok.Data;
    import lombok.EqualsAndHashCode;
    import lombok.RequiredArgsConstructor;
    import lombok.Setter;

    import java.time.LocalDateTime;
    import java.util.List;

    @Entity
    @Data
    @RequiredArgsConstructor
    @EqualsAndHashCode
    @Setter
    public class Prova {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @NotBlank
        @Size(min = 5)
        private String titulo;

        @NotNull
        private int duracaoMinutos;

        private LocalDateTime createdAt;

        @OneToMany(mappedBy = "prova")
        private List<TentativaProva> tentativasProva;

        @OneToMany(mappedBy = "prova")
        private List<Pergunta> perguntas;

        private boolean ativa;
    }
