package com.ecommerce.platform.subscription.infrastructure.persistence;

import com.ecommerce.platform.shared.domain.ValueObjects.*;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

/**
 * AttributeConverters para Value Objects no Subscription Service
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