package school.sptech;

import java.io.InputStream;
import java.nio.file.Path;

public abstract class LeitorArquivo {

    public abstract void processar(InputStream caminho, Conexao conexao, LogService logService) throws Exception;

    protected boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    protected String safe(String m, int max) {
        return (m == null || m.length() <= max) ? (m == null ? "" : m) : m.substring(0, max);
    }
}