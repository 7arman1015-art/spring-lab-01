package kz.iitu.springlab.web;

import kz.iitu.springlab.config.AppProperties;
import kz.iitu.springlab.config.EnvironmentBanner;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/lab3")
public class Lab3Controller {

    private final AppProperties props;
    private final EnvironmentBanner banner;
    private final Environment environment;

    public Lab3Controller(AppProperties props,
                          EnvironmentBanner banner,
                          Environment environment) {
        this.props = props;
        this.banner = banner;
        this.environment = environment;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("owner", props.owner());
        result.put("group", props.group());
        result.put("mailFrom", props.mail().from());
        result.put("mailRetryCount", props.mail().retryCount());
        result.put("mailTimeout", props.mail().timeout().toString());
        result.put("mailEnabled", props.mail().enabled());
        result.put("securityTokenTtl", props.security().tokenTtl().toString());
        result.put("securityMinPasswordLength", props.security().minPasswordLength());
        result.put("serverPort", environment.getProperty("server.port"));
        result.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        result.put("banner", banner.describe());
        return result;
    }
}
