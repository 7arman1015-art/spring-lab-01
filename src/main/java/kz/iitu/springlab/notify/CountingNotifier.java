package kz.iitu.springlab.notify;

import jakarta.annotation.PostConstruct;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("counting")
@Order(3)
public class CountingNotifier implements Notifier {

    @PostConstruct
    public void init() {
        System.out.println("COUNTING notifier initialized");
    }


    @Override
    public String send(String message) {

        int words = message.trim().split("\\s+").length;
        int characters = message.length();

        return "counting: words=" + words +
                ", characters=" + characters +
                ", message=" + message;
    }


    @Override
    public String channel() {
        return "counting";
    }
}
