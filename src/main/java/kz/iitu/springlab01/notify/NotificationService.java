package kz.iitu.springlab01.notify;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NotificationService {

    private final Notifier primary;
    private final Notifier console;
    private final List<Notifier> all;
    private final Map<String, Notifier> byName;

    public NotificationService(
            Notifier primary,
            @Qualifier("console") Notifier console,
            List<Notifier> all,
            Map<String, Notifier> byName
    ) {
        this.primary = primary;
        this.console = console;
        this.all = all;
        this.byName = byName;
    }

    public String viaPrimary(String message) {
        return primary.send(message);
    }

    public String viaConsole(String message) {
        return console.send(message);
    }

    public List<String> viaAll(String message) {
        return all.stream()
                .map(notifier -> notifier.send(message))
                .toList();
    }

    public Set<String> names() {
        return byName.keySet();
    }
}