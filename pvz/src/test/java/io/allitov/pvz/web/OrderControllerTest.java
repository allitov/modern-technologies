package io.allitov.pvz.web;

import io.allitov.pvz.exception.OrderCreationException;
import io.allitov.pvz.service.OrderService;
import org.jdbi.v3.core.JdbiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Nested
    class Add {

        @Test
        @DisplayName("При ошибке базы данных выполняется редирект на форму с flash-ошибкой, а не 500")
        void redirectsToFormWithErrorMessageWhenServiceThrowsOrderCreationException() throws Exception {
            OrderCreationException exception = new OrderCreationException(
                    "Failed to add order for contact 'ART-123'", new JdbiException("database failure") {
                    });
            when(orderService.addOrder("ART-123", "A-01")).thenThrow(exception);

            mockMvc.perform(post("/orders").param("contact", "ART-123").param("cell", "A-01"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/orders/new"))
                    .andExpect(flash().attribute("msg", "Не удалось принять заказ: ошибка базы данных"))
                    .andExpect(flash().attribute("msgType", "err"));

            verify(orderService).addOrder("ART-123", "A-01");
        }

        @Test
        @DisplayName("При успехе выполняется редирект на список с flash-сообщением о принятом заказе")
        void redirectsToListWithSuccessMessageWhenOrderAccepted() throws Exception {
            when(orderService.addOrder("ART-123", "A-01")).thenReturn("PVZ-000042");

            mockMvc.perform(post("/orders").param("contact", "ART-123").param("cell", "A-01"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"))
                    .andExpect(flash().attribute("msg", "Принят заказ PVZ-000042"))
                    .andExpect(flash().attribute("msgType", "ok"));

            verify(orderService).addOrder("ART-123", "A-01");
        }
    }
}
