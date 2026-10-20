package kz.iitu.springlab.lab8;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Records statements on the current test thread, excluding setup and other requests. */
public class SqlCapture implements StatementInspector {

    private static final ThreadLocal<List<String>> STATEMENTS = new ThreadLocal<>();

    public static void begin() {
        STATEMENTS.set(new ArrayList<>());
    }

    public static List<String> finish() {
        List<String> statements = STATEMENTS.get();
        STATEMENTS.remove();
        return statements == null ? List.of() : List.copyOf(statements);
    }

    @Override
    public String inspect(String sql) {
        List<String> statements = STATEMENTS.get();
        if (statements != null) {
            statements.add(sql);
        }
        return sql;
    }
}
