package io.allitov.pvz.service.impl;

import io.allitov.pvz.repository.OrderRepository;
import io.allitov.pvz.exception.OrderCreationException;
import org.jdbi.v3.core.JdbiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Nested
    class AddOrder {

        @ParameterizedTest
        @MethodSource("provideContactsAndCells")
        @DisplayName("Передаёт контакт в колонку телефона или артикула в зависимости от вида контакта")
        void passesContactToPhoneOrArticleColumnDependingOnContactKind(String contact, String cell, boolean phone) {
            when(orderRepository.insert(anyString(), any(), any(), any())).thenReturn(42L);

            String result = orderService.addOrder(contact, cell);

            assertThat(result).isEqualTo("PVZ-000042");
            if (phone) {
                verify(orderRepository).insert(
                        argThat(OrderServiceImplTest::isTemporaryCode), eq(contact), isNull(), eq(cell));
            } else {
                verify(orderRepository).insert(
                        argThat(OrderServiceImplTest::isTemporaryCode), isNull(), eq(contact), eq(cell));
            }
            verify(orderRepository).updateCode(42L, "PVZ-000042");
        }

        static Stream<Arguments> provideContactsAndCells() {
            return Stream.of(
                    Arguments.of("+79991234567", "A-01", true),
                    Arguments.of("8 (999) 123-45-67", null, true),
                    Arguments.of("ART-123", "A-01", false),
                    Arguments.of("ABC123", null, false));
        }

        @Test
        @DisplayName("Временный код из первого аргумента insert состоит из 30 шестнадцатеричных символов")
        void passesThirtyHexCharacterTemporaryCodeToInsert() {
            when(orderRepository.insert(anyString(), any(), any(), any())).thenReturn(7L);

            orderService.addOrder("ART-123", "B-02");

            ArgumentCaptor<String> temporaryCode = ArgumentCaptor.forClass(String.class);
            verify(orderRepository).insert(temporaryCode.capture(), any(), any(), any());
            assertThat(temporaryCode.getValue()).matches("[0-9a-f]{30}");
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("Оборачивает JdbiException от insert и updateCode в OrderCreationException с исходной причиной")
        void wrapsJdbiExceptionFromInsertAndUpdateCodeIntoOrderCreationException(boolean failOnUpdate) {
            JdbiException cause = new JdbiException("database failure") {
            };
            if (failOnUpdate) {
                when(orderRepository.insert(anyString(), any(), any(), any())).thenReturn(42L);
                doThrow(cause).when(orderRepository).updateCode(42L, "PVZ-000042");
            } else {
                doThrow(cause).when(orderRepository).insert(anyString(), any(), any(), any());
            }

            assertThatThrownBy(() -> orderService.addOrder("+79991234567", "A-01"))
                    .isInstanceOf(OrderCreationException.class)
                    .hasCause(cause);
        }
    }

    private static boolean isTemporaryCode(String value) {
        return value != null && value.matches("[0-9a-f]{30}");
    }
}
