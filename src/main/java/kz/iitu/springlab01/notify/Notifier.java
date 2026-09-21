package kz.iitu.springlab01.notify;

public interface Notifier {

    String send(String message);

    String channel();
}