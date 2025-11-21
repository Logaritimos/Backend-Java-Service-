package school.sptech;

import java.io.InputStream;

public abstract class LeitorArquivo {

    public abstract void processar(InputStream caminho, Conexao conexao, LogService logService) throws Exception;

    protected boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    protected String safe(String m, int max) {
        return (m == null || m.length() <= max) ? (m == null ? "" : m) : m.substring(0, max);
    }

    public static String semAcento(String s) {
        if (s == null) return null;

        // NFD para decompor (á -> a +  ́)
        String nfd = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);

        // Remove TODOS os marks
        String semMarks = nfd.replaceAll("\\p{M}+", ""); // \p{M} = Mn/Mc/Me
        // Trata NBSP e espaços estranhos
        semMarks = semMarks.replace('\u00A0', ' ');

        // NFC novamente e enxuga espaços
        String sem = java.text.Normalizer.normalize(semMarks, java.text.Normalizer.Form.NFC)
                .replaceAll("\\s+", " ")
                .trim();

        return sem;
    }
}