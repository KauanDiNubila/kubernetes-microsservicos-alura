package br.com.alurafood.pagamentos.service;

import br.com.alurafood.pagamentos.dto.PagamentoDto;
import br.com.alurafood.pagamentos.http.PedidoClient;
import br.com.alurafood.pagamentos.model.Pagamento;
import br.com.alurafood.pagamentos.model.Status;
import br.com.alurafood.pagamentos.repository.PagamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private PagamentoRepository repository;

    @Mock
    private PedidoClient pedido;

    private PagamentoService service;

    @BeforeEach
    void criarService() {
        service = new PagamentoService(repository, new ModelMapper(), pedido);
    }

    @Test
    void criarPagamentoComecaComStatusCriado() {
        PagamentoDto criado = service.criarPagamento(dto());

        assertEquals(Status.CRIADO, criado.getStatus());
        verify(repository).save(any(Pagamento.class));
    }

    @Test
    void confirmarPagamentoAtualizaStatusEAvisaPedidos() {
        Pagamento pagamento = pagamento(Status.CRIADO);
        when(repository.findById(1L)).thenReturn(Optional.of(pagamento));

        service.confirmarPagamento(1L);

        assertEquals(Status.CONFIRMADO, pagamento.getStatus());
        verify(repository).save(pagamento);
        verify(pedido).atualizaPagamento(7L);
    }

    @Test
    void confirmarPagamentoInexistenteNaoChamaPedidos() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.confirmarPagamento(1L));

        verifyNoInteractions(pedido);
    }

    @Test
    void alteraStatusMarcaComoConfirmadoSemIntegracao() {
        Pagamento pagamento = pagamento(Status.CRIADO);
        when(repository.findById(1L)).thenReturn(Optional.of(pagamento));

        service.alteraStatus(1L);

        assertEquals(Status.CONFIRMADO_SEM_INTEGRACAO, pagamento.getStatus());
    }

    @Test
    void atualizarPagamentoSemStatusMantemOStatusAtual() {
        when(repository.findById(1L)).thenReturn(Optional.of(pagamento(Status.CONFIRMADO)));
        when(repository.save(any(Pagamento.class))).thenAnswer(chamada -> chamada.getArgument(0));

        PagamentoDto atualizado = service.atualizarPagamento(1L, dto());

        assertEquals(Status.CONFIRMADO, atualizado.getStatus());
        assertEquals(1L, atualizado.getId());
    }

    @Test
    void excluirPagamentoInexistenteLancaNaoEncontrado() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.excluirPagamento(1L));

        verify(repository, never()).delete(any(Pagamento.class));
    }

    private PagamentoDto dto() {
        PagamentoDto dto = new PagamentoDto();
        dto.setValor(new BigDecimal("59.90"));
        dto.setNome("Maria Silva");
        dto.setNumero("1234567890123456");
        dto.setExpiracao("10/2030");
        dto.setCodigo("123");
        dto.setFormaDePagamentoId(1L);
        dto.setPedidoId(7L);
        return dto;
    }

    private Pagamento pagamento(Status status) {
        return new Pagamento(1L, new BigDecimal("59.90"), "Maria Silva", "1234567890123456",
                "10/2030", "123", status, 7L, 1L);
    }
}
