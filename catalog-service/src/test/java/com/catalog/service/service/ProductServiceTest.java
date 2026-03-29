package com.catalog.service.service;

import com.catalog.service.domain.Product;
import com.catalog.service.dto.ProductRequest;
import com.catalog.service.dto.ProductResponse;
import com.catalog.service.dto.events.ProductUpdatedEvent;
import com.catalog.service.exception.InsufficientStockException;
import com.catalog.service.exception.ProductNotFoundException;
import com.catalog.service.messaging.CatalogEventPublisher;
import com.catalog.service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CatalogEventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    private Product mockProduct;

    @BeforeEach
    void setUp() {
        mockProduct = Product.builder()
                .id(1L)
                .name("Teclado Mecânico")
                .description("Teclado gamer com switches blue")
                .price(new BigDecimal("299.90"))
                .stockQuantity(50)
                .category("PERIPHERALS")
                .imageUrl("https://example.com/teclado.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("deve retornar página de ProductResponse mapeada corretamente")
        void shouldReturnMappedPage() {
            Pageable pageable = PageRequest.of(0, 20);
            when(productRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(mockProduct), pageable, 1));

            Page<ProductResponse> result = productService.findAll(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            ProductResponse dto = result.getContent().get(0);
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("Teclado Mecânico");
            assertThat(dto.price()).isEqualByComparingTo("299.90");
        }

        @Test
        @DisplayName("deve retornar página vazia quando não há produtos")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            when(productRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

            Page<ProductResponse> result = productService.findAll(pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("findAvailable()")
    class FindAvailable {

        @Test
        @DisplayName("deve buscar produtos com estoque maior que zero")
        void shouldQueryProductsWithStockGreaterThanZero() {
            Pageable pageable = PageRequest.of(0, 20);
            when(productRepository.findByStockQuantityGreaterThan(0, pageable))
                    .thenReturn(new PageImpl<>(List.of(mockProduct)));

            Page<ProductResponse> result = productService.findAvailable(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).stockQuantity()).isGreaterThan(0);
            verify(productRepository).findByStockQuantityGreaterThan(0, pageable);
        }
    }

    @Nested
    @DisplayName("findByCategory()")
    class FindByCategory {

        @Test
        @DisplayName("deve retornar produtos filtrados pela categoria informada")
        void shouldReturnProductsForGivenCategory() {
            Pageable pageable = PageRequest.of(0, 20);
            when(productRepository.findByCategory("PERIPHERALS", pageable))
                    .thenReturn(new PageImpl<>(List.of(mockProduct)));

            Page<ProductResponse> result = productService.findByCategory("PERIPHERALS", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).category()).isEqualTo("PERIPHERALS");
        }

        @Test
        @DisplayName("deve retornar página vazia quando categoria não tem produtos")
        void shouldReturnEmptyPageForUnknownCategory() {
            Pageable pageable = PageRequest.of(0, 20);
            when(productRepository.findByCategory("UNKNOWN", pageable))
                    .thenReturn(Page.empty(pageable));

            Page<ProductResponse> result = productService.findByCategory("UNKNOWN", pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar ProductResponse quando produto existe")
        void shouldReturnProductWhenFound() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

            ProductResponse result = productService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Teclado Mecânico");
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(99L))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create()")
    class Create {

        private ProductRequest request;

        @BeforeEach
        void setUp() {
            request = ProductRequest.builder()
                    .name("Teclado Mecânico")
                    .description("Teclado gamer com switches blue")
                    .price(new BigDecimal("299.90"))
                    .stockQuantity(50)
                    .category("PERIPHERALS")
                    .imageUrl("https://example.com/teclado.jpg")
                    .build();
        }

        @Test
        @DisplayName("deve salvar e retornar ProductResponse com os dados do request")
        void shouldSaveAndReturnProductResponse() {
            when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

            ProductResponse result = productService.create(request);

            assertThat(result.name()).isEqualTo("Teclado Mecânico");
            assertThat(result.price()).isEqualByComparingTo("299.90");
        }

        @Test
        @DisplayName("deve persistir todos os campos do request no produto salvo")
        void shouldPersistAllFieldsFromRequest() {
            when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

            productService.create(request);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            Product saved = captor.getValue();

            assertThat(saved.getName()).isEqualTo("Teclado Mecânico");
            assertThat(saved.getImageUrl()).isEqualTo("https://example.com/teclado.jpg");
        }

        @Test
        @DisplayName("não deve publicar nenhum evento ao criar produto")
        void shouldNotPublishAnyEventOnCreate() {
            when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

            productService.create(request);

            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        private ProductRequest request;

        @BeforeEach
        void setUp() {
            request = ProductRequest.builder()
                    .name("Teclado Mecânico V2")
                    .price(new BigDecimal("349.90"))
                    .stockQuantity(30)
                    .category("PERIPHERALS")
                    .build();
        }

        @Test
        @DisplayName("deve atualizar os campos e retornar ProductResponse")
        void shouldUpdateFieldsAndReturnResponse() {
            Product updated = Product.builder()
                    .id(1L)
                    .name("Teclado Mecânico V2")
                    .price(new BigDecimal("349.90"))
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
            when(productRepository.save(any(Product.class))).thenReturn(updated);

            ProductResponse result = productService.update(1L, request);

            assertThat(result.name()).isEqualTo("Teclado Mecânico V2");
        }

        @Test
        @DisplayName("deve publicar ProductUpdatedEvent após atualizar")
        void shouldPublishProductUpdatedEvent() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
            when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

            productService.update(1L, request);

            verify(eventPublisher).publishProductUpdated(any(ProductUpdatedEvent.class));
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(99L, request))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar o produto quando ele existe")
        void shouldDeleteProductWhenExists() {
            when(productRepository.existsById(1L)).thenReturn(true);

            productService.delete(1L);

            verify(productRepository).deleteById(1L);
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> productService.delete(99L))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(productRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("incrementStock()")
    class IncrementStock {

        @Test
        @DisplayName("deve somar a quantidade ao estoque atual")
        void shouldAddQuantityToCurrentStock() {
            mockProduct.setStockQuantity(10);
            when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockProduct));
            when(productRepository.save(any())).thenReturn(mockProduct);

            productService.incrementStock(1L, 5);

            assertThat(mockProduct.getStockQuantity()).isEqualTo(15);
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound_Increment() {
            when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.incrementStock(99L, 5))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("decrementStock()")
    class DecrementStock {

        @Test
        @DisplayName("deve subtrair a quantidade do estoque atual")
        void shouldSubtractQuantityFromCurrentStock() {
            mockProduct.setStockQuantity(20);
            when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockProduct));
            when(productRepository.save(any())).thenReturn(mockProduct);

            productService.decrementStock(1L, 8);

            assertThat(mockProduct.getStockQuantity()).isEqualTo(12);
        }

        @Test
        @DisplayName("deve lançar InsufficientStockException quando estoque é insuficiente")
        void shouldThrowWhenStockIsInsufficient() {
            mockProduct.setStockQuantity(3);
            when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockProduct));

            assertThatThrownBy(() -> productService.decrementStock(1L, 10))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("deve decrementar quando quantidade solicitada é igual ao estoque disponível")
        void shouldDecrementWhenQuantityEqualsStock() {
            mockProduct.setStockQuantity(5);
            when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockProduct));
            when(productRepository.save(any())).thenReturn(mockProduct);

            productService.decrementStock(1L, 5);

            assertThat(mockProduct.getStockQuantity()).isZero();
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound_Decrement() {
            when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.decrementStock(99L, 5))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }
}