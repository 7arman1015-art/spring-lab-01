package kz.iitu.springlab01.web;

import kz.iitu.springlab01.lifecycle.LifecycleDemo;
import kz.iitu.springlab01.notify.NotificationService;
import kz.iitu.springlab01.notify.Notifier;
import kz.iitu.springlab01.scope.TicketOffice;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lab2")
public class Lab2Controller {

    private final NotificationService notifications;
    private final LifecycleDemo lifecycle;
    private final TicketOffice ticketOffice;
    private final Notifier customNotifier;

    public Lab2Controller(
            NotificationService notifications,
            LifecycleDemo lifecycle,
            TicketOffice ticketOffice,
            @Qualifier("timestamped") Notifier customNotifier
    ) {
        this.notifications = notifications;
        this.lifecycle = lifecycle;
        this.ticketOffice = ticketOffice;
        this.customNotifier = customNotifier;
    }

    @GetMapping("/notify")
    public Map<String, Object> notify(
            @RequestParam(name = "text", defaultValue = "Hello") String text
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("primary", notifications.viaPrimary(text));
        response.put("console", notifications.viaConsole(text));
        response.put("all", notifications.viaAll(text));
        response.put("beanNames", notifications.names());
        return response;
    }

    @GetMapping("/lifecycle")
    public List<String> lifecycle() {
        return lifecycle.events();
    }

    @GetMapping("/scopes")
    public Map<String, Object> scopes() {
        return ticketOffice.demo();
    }

    @GetMapping("/custom")
    public Map<String, String> custom(
            @RequestParam(name = "text", defaultValue = "Hello") String text
    ) {
        return Map.of(
                "channel", customNotifier.channel(),
                "result", customNotifier.send(text)
        );
    }
}
