package br.com.alurafood.pedidos.dto;

import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemDoPedidoDto {

    private Long id;

    @NotNull
    @Positive
    private Integer quantidade;

    private String descricao;
}
