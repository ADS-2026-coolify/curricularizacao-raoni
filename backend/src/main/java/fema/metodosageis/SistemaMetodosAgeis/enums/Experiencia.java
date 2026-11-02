package fema.metodosageis.SistemaMetodosAgeis.enums;

import lombok.Getter;

@Getter
public enum Experiencia {
    NENHUMA("Nunca ouvi falar"),
    BASICA("Já ouvi falar, conheço por cima"),
    INTERMEDIARIA("Já estudei ou usei em trabalhos da faculdade"),
    AVANCADA("Entendo bem (Scrum, Kanban, papéis, cerimônias)"),
    PROFISSIONAL("Uso no meu trabalho");

    private final String descricao;

    Experiencia(String descricao) {
        this.descricao = descricao;
    }

}
