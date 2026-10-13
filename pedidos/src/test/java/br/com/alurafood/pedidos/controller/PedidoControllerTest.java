package br.com.alurafood.pedidos.controller;

import br.com.alurafood.pedidos.dto.PedidoDto;
import br.com.alurafood.pedidos.service.PedidoService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private PedidoService service;

    @InjectMocks
    private PedidoController controller;

    private MockMvc mvc;

    @BeforeEach
    void criarMockMvc() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TratadorDeErros())
                .build();
    }

    @Test
    void buscarPedidoInexistenteRetorna404() throws Exception {
        when(service.obterPorId(99L)).thenThrow(new EntityNotFoundException());

        mvc.perform(get("/pedidos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void realizarPedidoSemItensRetorna400() throws Exception {
        mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].campo").value("itens"));

        verifyNoInteractions(service);
    }

    @Test
    void realizarPedidoComQuantidadeInvalidaRetorna400() throws Exception {
        mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\": [{\"quantidade\": 0, \"descricao\": \"Pizza\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].campo").value("itens[0].quantidade"));

        verifyNoInteractions(service);
    }

    @Test
    void realizarPedidoValidoRetorna201ComLocation() throws Exception {
        PedidoDto criado = new PedidoDto();
        criado.setId(3L);
        when(service.criarPedido(any(PedidoDto.class))).thenReturn(criado);

        mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\": [{\"quantidade\": 2, \"descricao\": \"Pizza\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/pedidos/3")));
    }

    @Test
    void atualizarStatusSemStatusRetorna400() throws Exception {
        mvc.perform(put("/pedidos/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
