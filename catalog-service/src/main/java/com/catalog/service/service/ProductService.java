package com.catalog.service.service;

import com.catalog.service.domain.Product;
import com.catalog.service.dto.ProductRequest;
import com.catalog.service.dto.ProductResponse;
import com.catalog.service.dto.events.ProductUpdatedEvent;
import com.catalog.service.exception.InsufficientStockException;
import com.catalog.service.exception.ProductNotFoundException;
import com.catalog.service.messaging.CatalogEventPublisher;
import com.catalog.service.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CatalogEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository, CatalogEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public Page<ProductResponse> findByCategory(String category, Pageable pageable) {
        return productRepository.findByCategory(category, pageable)
                .map(this::toResponse);
    }

    public Page<ProductResponse> findAvailable(Pageable pageable) {
        return productRepository.findByStockQuantityGreaterThan(0, pageable)
                .map(this::toResponse);
    }

    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .imageUrl(request.imageUrl())
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .category(request.category())
                .build();

        Product saved = productRepository.save(product);
        log.info("Produto criado: id={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setImageUrl(request.imageUrl());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(request.category());

        Product saved = productRepository.save(product);

        eventPublisher.publishProductUpdated(ProductUpdatedEvent.builder()
                .productId(saved.getId())
                .name(saved.getName())
                .price(saved.getPrice())
                .stockQuantity(saved.getStockQuantity())
                .build());

        log.info("Produto atualizado: id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void incrementStock(Long productId, Integer quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);

        log.info("Estoque revertido (compensação): productId={}, quantidade={}, novoEstoque={}",
                productId, quantity, product.getStockQuantity());
    }

    @Transactional
    public void decrementStock(Long productId, Integer quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(productId, product.getStockQuantity(), quantity);
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);

        log.info("Estoque decrementado: productId={}, quantidade={}, novoEstoque={}",
                productId, quantity, product.getStockQuantity());
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
        log.info("Produto deletado: id={}", id);
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
