package br.com.alurafood.pedidos.service;

import br.com.alurafood.pedidos.dto.ItemDoPedidoDto;
import br.com.alurafood.pedidos.dto.PedidoDto;
import br.com.alurafood.pedidos.dto.StatusDto;
import br.com.alurafood.pedidos.model.Pedido;
import br.com.alurafood.pedidos.model.Status;
import br.com.alurafood.pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import javax.persistence.EntityNotFoundException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository repository;

    private PedidoService service;

    @BeforeEach
    void criarService() {
        service = new PedidoService(repository, new ModelMapper());
    }

    @Test
    void criarPedidoComecaRealizadoEAssociaOsItens() {
        PedidoDto dto = new PedidoDto();
        dto.setItens(List.of(new ItemDoPedidoDto(null, 2, "Pizza"), new ItemDoPedidoDto(null, 1, "Suco")));

        PedidoDto criado = service.criarPedido(dto);

        ArgumentCaptor<Pedido> salvo = ArgumentCaptor.forClass(Pedido.class);
        verify(repository).save(salvo.capture());
        Pedido pedido = salvo.getValue();
        assertEquals(Status.REALIZADO, criado.getStatus());
        assertNotNull(criado.getDataHora());
        assertEquals(2, pedido.getItens().size());
        pedido.getItens().forEach(item -> assertSame(pedido, item.getPedido()));
    }

    @Test
    void aprovaPagamentoMarcaOPedidoComoPago() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(Status.REALIZADO);
        when(repository.porIdComItens(1L)).thenReturn(pedido);

        service.aprovaPagamentoPedido(1L);

        assertEquals(Status.PAGO, pedido.getStatus());
        verify(repository).atualizaStatus(Status.PAGO, pedido);
    }

    @Test
    void aprovaPagamentoDePedidoInexistenteLancaNaoEncontrado() {
        when(repository.porIdComItens(1L)).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> service.aprovaPagamentoPedido(1L));

        verify(repository, never()).atualizaStatus(any(), any());
    }

    @Test
    void atualizaStatusDePedidoInexistenteLancaNaoEncontrado() {
        when(repository.porIdComItens(1L)).thenReturn(null);

        assertThrows(EntityNotFoundException.class,
                () -> service.atualizaStatus(1L, new StatusDto(Status.PRONTO)));
    }
}
