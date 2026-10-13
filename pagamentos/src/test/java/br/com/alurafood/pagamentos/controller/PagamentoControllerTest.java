package br.com.alurafood.pagamentos.controller;

import br.com.alurafood.pagamentos.dto.PagamentoDto;
import br.com.alurafood.pagamentos.service.PagamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.persistence.EntityNotFoundException;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PagamentoControllerTest {

    private static final String PAGAMENTO_VALIDO = """
            {
              "valor": 59.90,
              "nome": "Maria Silva",
              "numero": "1234567890123456",
              "expiracao": "10/2030",
              "codigo": "123",
              "formaDePagamentoId": 1,
              "pedidoId": 7
            }
            """;

    @Mock
    private PagamentoService service;

    @InjectMocks
    private PagamentoController controller;

    private MockMvc mvc;

    @BeforeEach
    void criarMockMvc() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TratadorDeErros())
                .build();
    }

    @Test
    void detalharPagamentoInexistenteRetorna404() throws Exception {
        when(service.obterPorId(99L)).thenThrow(new EntityNotFoundException());

        mvc.perform(get("/pagamentos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cadastrarPagamentoInvalidoRetorna400ComOsCampos() throws Exception {
        mvc.perform(post("/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", hasSize(7)));

        verifyNoInteractions(service);
    }

    @Test
    void cadastrarPagamentoValidoRetorna201ComLocation() throws Exception {
        PagamentoDto criado = new PagamentoDto();
        criado.setId(5L);
        when(service.criarPagamento(any(PagamentoDto.class))).thenReturn(criado);

        mvc.perform(post("/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAGAMENTO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/pagamentos/5")));
    }
}
