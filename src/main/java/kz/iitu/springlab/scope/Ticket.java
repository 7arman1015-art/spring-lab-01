package kz.iitu.springlab.scope;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Scope("prototype")
public class Ticket {

    private final String id =
            UUID.randomUUID().toString().substring(0, 8);

    public String id() {
        return id;
    }
}
