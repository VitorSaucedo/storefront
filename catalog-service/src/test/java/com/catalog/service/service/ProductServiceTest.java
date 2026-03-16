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
        mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setName("Teclado Mecânico");
        mockProduct.setDescription("Teclado gamer com switches blue");
        mockProduct.setPrice(new BigDecimal("299.90"));
        mockProduct.setStockQuantity(50);
        mockProduct.setCategory("PERIPHERALS");
        mockProduct.setImageUrl("https://example.com/teclado.jpg");
        mockProduct.setCreatedAt(LocalDateTime.now());
        mockProduct.setUpdatedAt(LocalDateTime.now());
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------
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
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getName()).isEqualTo("Teclado Mecânico");
            assertThat(dto.getPrice()).isEqualByComparingTo("299.90");
            assertThat(dto.getCategory()).isEqualTo("PERIPHERALS");
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

    // -------------------------------------------------------------------------
    // findAvailable()
    // -------------------------------------------------------------------------
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
            assertThat(result.getContent().get(0).getStockQuantity()).isGreaterThan(0);
            verify(productRepository).findByStockQuantityGreaterThan(0, pageable);
        }
    }

    // -------------------------------------------------------------------------
    // findByCategory()
    // -------------------------------------------------------------------------
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
            assertThat(result.getContent().get(0).getCategory()).isEqualTo("PERIPHERALS");
            verify(productRepository).findByCategory("PERIPHERALS", pageable);
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

    // -------------------------------------------------------------------------
    // findById()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar ProductResponse quando produto existe")
        void shouldReturnProductWhenFound() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

            ProductResponse result = productService.findById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Teclado Mecânico");
            assertThat(result.getPrice()).isEqualByComparingTo("299.90");
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(99L))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("create()")
    class Create {

        private ProductRequest request;

        @BeforeEach
        void setUp() {
            request = new ProductRequest();
            request.setName("Teclado Mecânico");
            request.setDescription("Teclado gamer com switches blue");
            request.setPrice(new BigDecimal("299.90"));
            request.setStockQuantity(50);
            request.setCategory("PERIPHERALS");
            request.setImageUrl("https://example.com/teclado.jpg");
        }

        @Test
        @DisplayName("deve salvar e retornar ProductResponse com os dados do request")
        void shouldSaveAndReturnProductResponse() {
            when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

            ProductResponse result = productService.create(request);

            assertThat(result.getName()).isEqualTo("Teclado Mecânico");
            assertThat(result.getPrice()).isEqualByComparingTo("299.90");
            assertThat(result.getStockQuantity()).isEqualTo(50);
            assertThat(result.getCategory()).isEqualTo("PERIPHERALS");
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
            assertThat(saved.getPrice()).isEqualByComparingTo("299.90");
            assertThat(saved.getStockQuantity()).isEqualTo(50);
            assertThat(saved.getCategory()).isEqualTo("PERIPHERALS");
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

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("update()")
    class Update {

        private ProductRequest request;

        @BeforeEach
        void setUp() {
            request = new ProductRequest();
            request.setName("Teclado Mecânico V2");
            request.setDescription("Nova versão");
            request.setPrice(new BigDecimal("349.90"));
            request.setStockQuantity(30);
            request.setCategory("PERIPHERALS");
            request.setImageUrl("https://example.com/teclado-v2.jpg");
        }

        @Test
        @DisplayName("deve atualizar os campos e retornar ProductResponse")
        void shouldUpdateFieldsAndReturnResponse() {
            Product updated = new Product();
            updated.setId(1L);
            updated.setName("Teclado Mecânico V2");
            updated.setPrice(new BigDecimal("349.90"));
            updated.setStockQuantity(30);
            updated.setCategory("PERIPHERALS");
            updated.setImageUrl("https://example.com/teclado-v2.jpg");
            updated.setCreatedAt(LocalDateTime.now());
            updated.setUpdatedAt(LocalDateTime.now());

            when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
            when(productRepository.save(any(Product.class))).thenReturn(updated);

            ProductResponse result = productService.update(1L, request);

            assertThat(result.getName()).isEqualTo("Teclado Mecânico V2");
            assertThat(result.getPrice()).isEqualByComparingTo("349.90");
        }

        @Test
        @DisplayName("deve publicar ProductUpdatedEvent após atualizar")
        void shouldPublishProductUpdatedEvent() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
            when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

            productService.update(1L, request);

            ArgumentCaptor<ProductUpdatedEvent> captor = ArgumentCaptor.forClass(ProductUpdatedEvent.class);
            verify(eventPublisher).publishProductUpdated(captor.capture());
            ProductUpdatedEvent event = captor.getValue();
            assertThat(event.productId()).isEqualTo(1L);
            assertThat(event.name()).isEqualTo("Teclado Mecânico V2");
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(99L, request))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(productRepository, never()).save(any());
            verify(eventPublisher, never()).publishProductUpdated(any());
        }
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // incrementStock()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("incrementStock()")
    class IncrementStock {

        @Test
        @DisplayName("deve somar a quantidade ao estoque atual")
        void shouldAddQuantityToCurrentStock() {
            mockProduct.setStockQuantity(10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
            when(productRepository.save(any())).thenReturn(mockProduct);

            productService.incrementStock(1L, 5);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertThat(captor.getValue().getStockQuantity()).isEqualTo(15);
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.incrementStock(99L, 5))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(productRepository, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // decrementStock()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("decrementStock()")
    class DecrementStock {

        @Test
        @DisplayName("deve subtrair a quantidade do estoque atual")
        void shouldSubtractQuantityFromCurrentStock() {
            mockProduct.setStockQuantity(20);
            when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
            when(productRepository.save(any())).thenReturn(mockProduct);

            productService.decrementStock(1L, 8);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertThat(captor.getValue().getStockQuantity()).isEqualTo(12);
        }

        @Test
        @DisplayName("deve lançar InsufficientStockException quando estoque é insuficiente")
        void shouldThrowWhenStockIsInsufficient() {
            mockProduct.setStockQuantity(3);
            when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

            assertThatThrownBy(() -> productService.decrementStock(1L, 10))
                    .isInstanceOf(InsufficientStockException.class);

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve decrementar quando quantidade solicitada é igual ao estoque disponível")
        void shouldDecrementWhenQuantityEqualsStock() {
            mockProduct.setStockQuantity(5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
            when(productRepository.save(any())).thenReturn(mockProduct);

            productService.decrementStock(1L, 5);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertThat(captor.getValue().getStockQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.decrementStock(99L, 5))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(productRepository, never()).save(any());
        }
    }
}