package kz.iitu.springlab.web;

import kz.iitu.springlab.lifecycle.LifecycleDemo;
import kz.iitu.springlab.notify.NotificationService;
import kz.iitu.springlab.notify.Notifier;
import kz.iitu.springlab.scope.TicketOffice;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lab2")
public class Lab2Controller {

    private final NotificationService notifications;
    private final LifecycleDemo lifecycle;
    private final Notifier counting;
    private final TicketOffice ticketOffice;


    public Lab2Controller(
            NotificationService notifications,
            LifecycleDemo lifecycle,
            @Qualifier("counting") Notifier counting,
            TicketOffice ticketOffice
    ) {
        this.notifications = notifications;
        this.lifecycle = lifecycle;
        this.counting = counting;
        this.ticketOffice = ticketOffice;
    }


    @GetMapping("/notify")
    public Map<String, Object> notify(
            @RequestParam(defaultValue = "Hello") String text
    ) {

        return Map.of(
                "primary", notifications.viaPrimary(text),
                "console", notifications.viaConsole(text),
                "all", notifications.viaAll(text),
                "beanNames", notifications.names()
        );
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
    public String custom(
            @RequestParam(defaultValue = "Hello") String text
    ) {
        return counting.send(text);
    }
}
