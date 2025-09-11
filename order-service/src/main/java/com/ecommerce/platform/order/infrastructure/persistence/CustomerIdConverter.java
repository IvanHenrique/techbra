package com.ecommerce.platform.order.infrastructure.persistence;

import com.ecommerce.platform.shared.domain.ValueObjects.CustomerEmail;
import com.ecommerce.platform.shared.domain.ValueObjects.CustomerId;
import com.ecommerce.platform.shared.domain.ValueObjects.ProductId;
import com.ecommerce.platform.shared.domain.ValueObjects.Quantity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

/**
 * AttributeConverters para Value Objects no Order Service
 * Colocados diretamente no módulo para garantir que sejam detectados
 */

@Converter(autoApply = true)
public class CustomerIdConverter implements AttributeConverter<CustomerId, UUID> {
    @Override
    public UUID convertToDatabaseColumn(CustomerId customerId) {
        return customerId != null ? customerId.value() : null;
    }

    @Override
    public CustomerId convertToEntityAttribute(UUID uuid) {
        return uuid != null ? new CustomerId(uuid) : null;
    }
}

@Converter(autoApply = true)
class CustomerEmailConverter implements AttributeConverter<CustomerEmail, String> {
    @Override
    public String convertToDatabaseColumn(CustomerEmail customerEmail) {
        return customerEmail != null ? customerEmail.value() : null;
    }

    @Override
    public CustomerEmail convertToEntityAttribute(String email) {
        return email != null ? new CustomerEmail(email) : null;
    }
}

@Converter(autoApply = true)
class ProductIdConverter implements AttributeConverter<ProductId, UUID> {
    @Override
    public UUID convertToDatabaseColumn(ProductId productId) {
        return productId != null ? productId.value() : null;
    }

    @Override
    public ProductId convertToEntityAttribute(UUID uuid) {
        return uuid != null ? new ProductId(uuid) : null;
    }
}

@Converter(autoApply = true)
class QuantityConverter implements AttributeConverter<Quantity, Integer> {
    @Override
    public Integer convertToDatabaseColumn(Quantity quantity) {
        return quantity != null ? quantity.value() : null;
    }

    @Override
    public Quantity convertToEntityAttribute(Integer value) {
        return value != null ? new Quantity(value) : null;
    }
}