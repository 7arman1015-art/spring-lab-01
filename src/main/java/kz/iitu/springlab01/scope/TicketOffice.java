package kz.iitu.springlab01.scope;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TicketOffice {

    private final Ticket direct;
    private final ObjectProvider<Ticket> provider;

    public TicketOffice(Ticket direct, ObjectProvider<Ticket> provider) {
        this.direct = direct;
        this.provider = provider;
    }

    public Map<String, Object> demo() {
        String direct1 = direct.id();
        String direct2 = direct.id();
        String fresh1 = provider.getObject().id();
        String fresh2 = provider.getObject().id();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("injectedDirectly", List.of(direct1, direct2));
        result.put("viaProvider", List.of(fresh1, fresh2));
        result.put("office", System.identityHashCode(this));
        return result;
    }
}
