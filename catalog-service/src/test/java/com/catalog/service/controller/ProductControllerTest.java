package com.catalog.service.controller;

import com.catalog.service.config.TestSecurityConfig;
import com.catalog.service.dto.ProductRequest;
import com.catalog.service.dto.ProductResponse;
import com.catalog.service.exception.ProductNotFoundException;
import com.catalog.service.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(TestSecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductService productService;

    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productResponse = new ProductResponse(
                1L,
                "Teclado Mecânico",
                "Teclado gamer com switches blue",
                new BigDecimal("299.90"),
                50,
                "PERIPHERALS",
                "https://example.com/teclado.jpg",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // -------------------------------------------------------------------------
    // GET /products
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /products")
    class FindAll {

        @Test
        @DisplayName("deve retornar 200 OK com página de produtos sem autenticação")
        void shouldReturn200WithProductPage() throws Exception {
            Page<ProductResponse> page = new PageImpl<>(List.of(productResponse), PageRequest.of(0, 20), 1);
            when(productService.findAll(any())).thenReturn(page);

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("1"))
                    .andExpect(jsonPath("$.content[0].name").value("Teclado Mecânico"))
                    .andExpect(jsonPath("$.content[0].price").value("299.90"))
                    .andExpect(jsonPath("$.page.totalElements").value(1));
        }

        @Test
        @DisplayName("deve retornar página vazia quando não há produtos")
        void shouldReturnEmptyPage() throws Exception {
            when(productService.findAll(any())).thenReturn(Page.empty());

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.page.totalElements").value(0));
        }
    }

    // -------------------------------------------------------------------------
    // GET /products/available
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /products/available")
    class FindAvailable {

        @Test
        @DisplayName("deve retornar 200 OK com produtos disponíveis em estoque")
        void shouldReturn200WithAvailableProducts() throws Exception {
            Page<ProductResponse> page = new PageImpl<>(List.of(productResponse), PageRequest.of(0, 20), 1);
            when(productService.findAvailable(any())).thenReturn(page);

            mockMvc.perform(get("/products/available"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].stockQuantity").value(50));
        }

        @Test
        @DisplayName("deve retornar página vazia quando não há produtos disponíveis")
        void shouldReturnEmptyPageWhenNoneAvailable() throws Exception {
            when(productService.findAvailable(any())).thenReturn(Page.empty());

            mockMvc.perform(get("/products/available"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    // -------------------------------------------------------------------------
    // GET /products/category/{category}
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /products/category/{category}")
    class FindByCategory {

        @Test
        @DisplayName("deve retornar 200 OK com produtos da categoria informada")
        void shouldReturn200WithProductsByCategory() throws Exception {
            Page<ProductResponse> page = new PageImpl<>(List.of(productResponse), PageRequest.of(0, 20), 1);
            when(productService.findByCategory(eq("PERIPHERALS"), any())).thenReturn(page);

            mockMvc.perform(get("/products/category/PERIPHERALS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].category").value("PERIPHERALS"))
                    .andExpect(jsonPath("$.page.totalElements").value(1));
        }

        @Test
        @DisplayName("deve retornar página vazia quando não há produtos na categoria")
        void shouldReturnEmptyPageWhenCategoryIsEmpty() throws Exception {
            when(productService.findByCategory(eq("MONITORS"), any())).thenReturn(Page.empty());

            mockMvc.perform(get("/products/category/MONITORS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    // -------------------------------------------------------------------------
    // GET /products/{id}
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /products/{id}")
    class FindById {

        @Test
        @DisplayName("deve retornar 200 OK com o produto quando encontrado")
        void shouldReturn200WhenProductFound() throws Exception {
            when(productService.findById(1L)).thenReturn(productResponse);

            mockMvc.perform(get("/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("1"))
                    .andExpect(jsonPath("$.name").value("Teclado Mecânico"))
                    .andExpect(jsonPath("$.category").value("PERIPHERALS"));
        }

        @Test
        @DisplayName("deve retornar 404 Not Found quando produto não existe")
        void shouldReturn404WhenProductNotFound() throws Exception {
            when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

            mockMvc.perform(get("/products/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // POST /products
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /products")
    class Create {

        private ProductRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new ProductRequest();
            validRequest.setName("Teclado Mecânico");
            validRequest.setDescription("Teclado gamer com switches blue");
            validRequest.setPrice(new BigDecimal("299.90"));
            validRequest.setStockQuantity(50);
            validRequest.setCategory("PERIPHERALS");
            validRequest.setImageUrl("https://example.com/teclado.jpg");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 201 Created com produto criado quando ADMIN")
        void shouldReturn201WhenAdminCreatesProduct() throws Exception {
            when(productService.create(any(ProductRequest.class))).thenReturn(productResponse);

            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("1"))
                    .andExpect(jsonPath("$.name").value("Teclado Mecânico"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("deve retornar 403 Forbidden quando USER tenta criar produto")
        void shouldReturn403WhenUserTriesToCreate() throws Exception {
            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden());

            verify(productService, never()).create(any());
        }

        @Test
        @DisplayName("deve retornar 401 Unauthorized quando não autenticado")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnauthorized());

            verify(productService, never()).create(any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 400 quando body está vazio")
        void shouldReturn400WhenBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 400 quando nome está em branco")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            validRequest.setName("");

            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 400 quando preço é zero")
        void shouldReturn400WhenPriceIsZero() throws Exception {
            validRequest.setPrice(BigDecimal.ZERO);

            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 400 quando estoque é negativo")
        void shouldReturn400WhenStockIsNegative() throws Exception {
            validRequest.setStockQuantity(-1);

            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 400 quando categoria está em branco")
        void shouldReturn400WhenCategoryIsBlank() throws Exception {
            validRequest.setCategory("");

            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /products/{id}
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("PUT /products/{id}")
    class Update {

        private ProductRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new ProductRequest();
            validRequest.setName("Teclado Mecânico V2");
            validRequest.setPrice(new BigDecimal("349.90"));
            validRequest.setStockQuantity(30);
            validRequest.setCategory("PERIPHERALS");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 200 OK com produto atualizado quando ADMIN")
        void shouldReturn200WhenAdminUpdatesProduct() throws Exception {
            when(productService.update(eq(1L), any(ProductRequest.class))).thenReturn(productResponse);

            mockMvc.perform(put("/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("1"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 404 quando produto não existe")
        void shouldReturn404WhenProductNotFound() throws Exception {
            when(productService.update(eq(99L), any())).thenThrow(new ProductNotFoundException(99L));

            mockMvc.perform(put("/products/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("deve retornar 403 Forbidden quando USER tenta atualizar")
        void shouldReturn403WhenUserTriesToUpdate() throws Exception {
            mockMvc.perform(put("/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden());

            verify(productService, never()).update(any(), any());
        }

        @Test
        @DisplayName("deve retornar 401 Unauthorized quando não autenticado")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(put("/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnauthorized());

            verify(productService, never()).update(any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /products/{id}
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("DELETE /products/{id}")
    class Delete {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 204 No Content quando produto deletado com sucesso")
        void shouldReturn204WhenAdminDeletesProduct() throws Exception {
            doNothing().when(productService).delete(1L);

            mockMvc.perform(delete("/products/1"))
                    .andExpect(status().isNoContent());

            verify(productService).delete(1L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 404 quando produto não existe")
        void shouldReturn404WhenProductNotFound() throws Exception {
            doThrow(new ProductNotFoundException(99L)).when(productService).delete(99L);

            mockMvc.perform(delete("/products/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("deve retornar 403 Forbidden quando USER tenta deletar")
        void shouldReturn403WhenUserTriesToDelete() throws Exception {
            mockMvc.perform(delete("/products/1"))
                    .andExpect(status().isForbidden());

            verify(productService, never()).delete(any());
        }

        @Test
        @DisplayName("deve retornar 401 Unauthorized quando não autenticado")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(delete("/products/1"))
                    .andExpect(status().isUnauthorized());

            verify(productService, never()).delete(any());
        }
    }
}