package com.example.facturaventa.arca;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="arca")
public record ArcaConfig(boolean enabled, String environment, String cuit, String pkcs12Path,
                         String pkcs12Password, String pkcs12Alias) {
    public boolean ready() {
        return enabled && "homologacion".equalsIgnoreCase(environment)
            && cuit != null && cuit.matches("\\d{11}")
            && pkcs12Path != null && !pkcs12Path.isBlank()
            && pkcs12Password != null && !pkcs12Password.isBlank();
    }
}
