package com.ecommerce.platform.shared.domain;

import jakarta.validation.constraints.NotBlank;

/**
 * Value Object para representar endereços
 * Encapsula dados de endereço com validações de domínio
 * e funcionalidades específicas para o contexto de e-commerce.
 * Características:
 * - Imutável (record)
 * - Validações na construção
 * - Formatação para exibição
 * - Comparação por valor
 * Utilizado para:
 * - Endereços de entrega
 * - Endereços de cobrança
 * - Endereços de cliente
 * - Cálculo de frete
 */
public record Address(
    @NotBlank String street,
    String number,
    String complement,
    @NotBlank String neighborhood,
    @NotBlank String city,
    @NotBlank String state,
    @NotBlank String zipCode,
    @NotBlank String country
) {
    
    /**
     * Constructor com validações
     */
    public Address {
        if (street == null || street.trim().isEmpty()) {
            throw new IllegalArgumentException("Street cannot be null or empty");
        }
        if (neighborhood == null || neighborhood.trim().isEmpty()) {
            throw new IllegalArgumentException("Neighborhood cannot be null or empty");
        }
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be null or empty");
        }
        if (state == null || state.trim().isEmpty()) {
            throw new IllegalArgumentException("State cannot be null or empty");
        }
        if (zipCode == null || zipCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Zip code cannot be null or empty");
        }
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Country cannot be null or empty");
        }
        
        // Normalizar dados
        street = street.trim();
        number = number != null ? number.trim() : "S/N";
        complement = complement != null ? complement.trim() : null;
        neighborhood = neighborhood.trim();
        city = city.trim();
        state = state.trim().toUpperCase();
        zipCode = zipCode.trim().replaceAll("[^0-9]", ""); // Remove caracteres não numéricos
        country = country.trim();
    }
    
    /**
     * Factory method para endereço brasileiro
     */
    public static Address createBrazilian(
            String street,
            String number,
            String complement,
            String neighborhood,
            String city,
            String state,
            String zipCode) {
        return new Address(street, number, complement, neighborhood, city, state, zipCode, "Brasil");
    }
    
    /**
     * Factory method para endereço padrão (placeholder)
     */
    public static Address createDefault() {
        return new Address(
            "Endereço padrão do cliente",
            "S/N",
            null,
            "Centro",
            "São Paulo",
            "SP",
            "01000000",
            "Brasil"
        );
    }
    
    /**
     * Retorna endereço formatado para exibição
     */
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(street);
        
        if (number != null && !number.equals("S/N")) {
            sb.append(", ").append(number);
        }
        
        if (complement != null && !complement.isEmpty()) {
            sb.append(" - ").append(complement);
        }
        
        sb.append(", ").append(neighborhood);
        sb.append(", ").append(city).append(" - ").append(state);
        sb.append(", CEP: ").append(getFormattedZipCode());
        
        return sb.toString();
    }
    
    /**
     * Retorna CEP formatado (00000-000)
     */
    public String getFormattedZipCode() {
        if (zipCode.length() == 8) {
            return zipCode.substring(0, 5) + "-" + zipCode.substring(5);
        }
        return zipCode;
    }
    
    /**
     * Verifica se é endereço brasileiro
     */
    public boolean isBrazilian() {
        return "Brasil".equalsIgnoreCase(country) || "Brazil".equalsIgnoreCase(country) || "BR".equalsIgnoreCase(country);
    }
    
    /**
     * Verifica se CEP é válido (formato brasileiro)
     */
    public boolean hasValidBrazilianZipCode() {
        return isBrazilian() && zipCode.matches("\\d{8}");
    }
    
    /**
     * Obtém cidade e estado formatados
     */
    public String getCityState() {
        return city + " - " + state;
    }
    
    /**
     * Verifica se endereço está completo
     */
    public boolean isComplete() {
        return street != null && !street.isEmpty() &&
               neighborhood != null && !neighborhood.isEmpty() &&
               city != null && !city.isEmpty() &&
               state != null && !state.isEmpty() &&
               zipCode != null && !zipCode.isEmpty() &&
               country != null && !country.isEmpty();
    }
    
    /**
     * Getter methods para compatibilidade
     */
    public String getStreet() { return street; }
    public String getNumber() { return number; }
    public String getComplement() { return complement; }
    public String getNeighborhood() { return neighborhood; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getZipCode() { return zipCode; }
    public String getCountry() { return country; }
}