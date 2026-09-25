package kz.iitu.springlab.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank
        String owner,
        @NotBlank
        String group,
        @Valid
        Mail mail,
        @NotNull @Valid Security security
) {
    public record Mail(
            @NotBlank @Email String from,
            @Min(1) @Max(10) @DefaultValue("3") int retryCount,
            @DefaultValue("5s") Duration timeout,
            @DefaultValue("true") boolean enabled
    ) {
    }

    public record Security(
            @NotNull @DefaultValue("30m") Duration tokenTtl,
            @Min(8) @Max(128) @DefaultValue("12") int minPasswordLength
    ) {
    }
}
