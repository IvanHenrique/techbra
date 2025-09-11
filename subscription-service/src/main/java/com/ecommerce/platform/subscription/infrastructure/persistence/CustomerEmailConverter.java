package com.ecommerce.platform.subscription.infrastructure.persistence;

import com.ecommerce.platform.shared.domain.ValueObjects.CustomerEmail;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

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