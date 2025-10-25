package school.sptech;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Consumer;

public class LeitorExcell {

    // Nomes canônicos esperados pelo domínio
    private static final Set<String> COLS_ESPERADAS = Set.of(
            "estado", "mes", "ano",
            "qtdaeroportos", "numvoosregulares", "numvoosirregulares",
            "numembarques", "numdesembarques", "numvoostotais"
    );

    // Sinônimos -> canônico (tudo já normalizado)
    private static final Map<String, String> ALIASES;
    static {
        Map<String, String> m = new HashMap<>();
        // estado
        m.put("estado", "estado");
        // mês
        m.put("mes", "mes");
        // ano
        m.put("ano", "ano");

        // qtd aeroportos
        m.put("qtdaeroportos", "qtdaeroportos");
        m.put("qtddeaeroportos", "qtdaeroportos");
        m.put("qtd_aeroportos", "qtdaeroportos");
        m.put("qtdaeroporto", "qtdaeroportos");
        m.put("numerodeaeroportos", "qtdaeroportos");

        // voos regulares
        m.put("numvoosregulares", "numvoosregulares");
        m.put("voosregulares", "numvoosregulares");
        m.put("nvoosregulares", "numvoosregulares");
        m.put("qtdevoosregulares", "numvoosregulares");
        m.put("voos_regulares", "numvoosregulares");

        // voos irregulares
        m.put("numvoosirregulares", "numvoosirregulares");
        m.put("voosirregulares", "numvoosirregulares");
        m.put("nvoosirregulares", "numvoosirregulares");
        m.put("qtdevoosirregulares", "numvoosirregulares");
        m.put("voos_irregulares", "numvoosirregulares");

        // embarques
        m.put("numembarques", "numembarques");
        m.put("embarques", "numembarques");
        m.put("qtdembarques", "numembarques");

        // desembarques
        m.put("numdesembarques", "numdesembarques");
        m.put("desembarques", "numdesembarques");
        m.put("qtddesembarques", "numdesembarques");

        // total
        m.put("numvoostotais", "numvoostotais");
        m.put("voostotais", "numvoostotais");
        m.put("totaldevoos", "numvoostotais");
        m.put("total", "numvoostotais");
        m.put("voos_total", "numvoostotais");

        ALIASES = Collections.unmodifiableMap(m);
    }

    private static final int DEFAULT_BATCH = 1000;
    private static final int MAX_SCAN_HEADER_ROWS = 30;

    public void processar(java.nio.file.Path caminhoXlsx,
                          int batchSize,
                          Consumer<List<Voo>> loteHandler,
                          LogService logService) throws Exception {

        if (batchSize <= 0) batchSize = DEFAULT_BATCH;
        Objects.requireNonNull(caminhoXlsx, "caminhoXlsx nulo");
        Objects.requireNonNull(loteHandler, "loteHandler nulo");
        Objects.requireNonNull(logService, "logService nulo");

        try (java.io.InputStream in = java.nio.file.Files.newInputStream(caminhoXlsx);
             Workbook wb = new XSSFWorkbook(in)) {

            // 1) Descobrir a aba que contém o cabeçalho válido
            Sheet sheet = localizarAbaComCabecalho(wb);
            if (sheet == null) {
                throw new IllegalStateException("Não encontrei nenhuma aba com o cabeçalho esperado.");
            }

            DataFormatter fmt = new DataFormatter(new Locale("pt", "BR"));

            // 2) Descobrir a linha do cabeçalho naquela aba
            int headerRowNum = localizarLinhaCabecalho(sheet, fmt, COLS_ESPERADAS, MAX_SCAN_HEADER_ROWS);
            Row header = sheet.getRow(headerRowNum);

            // 3) Mapear colunas -> índices (já resolvendo sinônimos)
            Map<String, Integer> colIndex = mapearCabecalho(header, fmt);

            // 4) Garantir que todas as esperadas estão presentes
            validarCabecalho(colIndex);

            // 5) Log curto do mapeamento (cabendo em VARCHAR(200))
            String mapeamentoCurto = mapeamentoCurto(colIndex);
            logService.registrar("INFO",
                    String.format("Usando aba '%s', header na linha %d. Mapeamento: %s",
                            sheet.getSheetName(), headerRowNum, mapeamentoCurto));

            // 6) Ler linhas e emitir em lotes
            List<Voo> buffer = new ArrayList<>(batchSize);
            int last = sheet.getLastRowNum();

            for (int r = headerRowNum + 1; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                if (linhaSemDados(row, fmt, 3)) continue;

                Voo v = new Voo();
                v.setEstado(getString(row, colIndex.get("estado"), fmt));
                v.setMes(getString(row, colIndex.get("mes"), fmt));
                v.setAno(getInt(row, colIndex.get("ano"), fmt));

                v.setQtdAeroportos(getInt(row, colIndex.get("qtdaeroportos"), fmt));
                v.setNumVoosRegulares(getInt(row, colIndex.get("numvoosregulares"), fmt));
                v.setNumVoosIrregulares(getInt(row, colIndex.get("numvoosirregulares"), fmt));
                v.setNumEmbarques(getInt(row, colIndex.get("numembarques"), fmt));
                v.setNumDesembarques(getInt(row, colIndex.get("numdesembarques"), fmt));
                v.setNumVoosTotais(getInt(row, colIndex.get("numvoostotais"), fmt));

                buffer.add(v);
                if (buffer.size() == batchSize) {
                    loteHandler.accept(buffer);
                    buffer.clear();
                }
            }
            if (!buffer.isEmpty()) {
                loteHandler.accept(buffer);
            }
        }
    }

    /** Encontra a primeira aba que contenha uma linha com todas as colunas esperadas. */
    private Sheet localizarAbaComCabecalho(Workbook wb) {
        DataFormatter fmt = new DataFormatter(new Locale("pt", "BR"));
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet s = wb.getSheetAt(i);
            try {
                localizarLinhaCabecalho(s, fmt, COLS_ESPERADAS, MAX_SCAN_HEADER_ROWS);
                return s; // achou uma aba válida
            } catch (Exception ignore) {
                // tenta próxima
            }
        }
        return null;
    }

    /** Procura a primeira linha que contenha todas as colunas esperadas (normalizadas/sinônimos resolvidos). */
    private int localizarLinhaCabecalho(Sheet sheet, DataFormatter fmt,
                                        Set<String> esperadas, int maxScan) {
        int maxRow = Math.min(sheet.getLastRowNum(), maxScan);
        for (int r = 0; r <= maxRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Map<String, Integer> idx = mapearCabecalho(row, fmt);
            if (idx.keySet().containsAll(esperadas)) {
                return r;
            }
        }
        throw new IllegalStateException("Não encontrei linha de cabeçalho nas primeiras " + (maxScan + 1) + " linhas.");
    }

    /** Mapeia as colunas: resolve sinônimos e devolve chaves canônicas -> índice. */
    private Map<String, Integer> mapearCabecalho(Row header, DataFormatter fmt) {
        Map<String, Integer> map = new HashMap<>();
        short lastCell = header.getLastCellNum();
        for (int c = 0; c < lastCell; c++) {
            Cell cell = header.getCell(c);
            if (cell == null) continue;
            String bruto = fmt.formatCellValue(cell);
            if (bruto == null || bruto.trim().isEmpty()) continue;

            String norm = normalizar(bruto);
            String canonico = ALIASES.getOrDefault(norm, norm); // resolve sinônimo se houver
            if (COLS_ESPERADAS.contains(canonico)) {
                // só mapeia o que interessa ao domínio; primeira ocorrência prevalece
                map.putIfAbsent(canonico, c);
            }
        }
        return map;
    }

    private void validarCabecalho(Map<String, Integer> colIndex) {
        List<String> faltantes = new ArrayList<>();
        for (String col : COLS_ESPERADAS) {
            if (!colIndex.containsKey(col)) faltantes.add(col);
        }
        if (!faltantes.isEmpty()) {
            throw new IllegalArgumentException("Cabeçalho inválido. Faltando colunas: " + faltantes);
        }
    }

    private boolean linhaSemDados(Row row, DataFormatter fmt, int primeirasColunas) {
        for (int i = 0; i < primeirasColunas; i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                String v = fmt.formatCellValue(cell);
                if (v != null && !v.trim().isEmpty()) return false;
            }
        }
        return true;
    }

    private String getString(Row row, Integer col, DataFormatter fmt) {
        if (col == null) return null;
        String v = fmt.formatCellValue(row.getCell(col));
        return (v == null) ? null : v.trim();
    }

    private Integer getInt(Row row, Integer col, DataFormatter fmt) {
        if (col == null) return null;
        Cell cell = row.getCell(col);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC) {
            // Garante inteiro mesmo quando vier como double
            return (int) Math.round(cell.getNumericCellValue());
        }
        // Se vier como texto: remove tudo que não for dígito/sinal
        String s = fmt.formatCellValue(cell);
        if (s == null || s.isBlank()) return null;
        s = s.replaceAll("[^0-9\\-]", "");
        return s.isBlank() ? null : Integer.parseInt(s);
    }

    /** Normaliza rótulos: sem acentos, minúsculas, sem espaços/pontuação. */
    private String normalizar(String s) {
        if (s == null) return "";
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** Gera um texto curto com o mapeamento (para caber no VARCHAR(200) do registroLogs). */
    private String mapeamentoCurto(Map<String, Integer> colIndex) {
        // Ex.: estado:0, mes:1, ano:2, ...
        List<String> pares = new ArrayList<>();
        for (String k : COLS_ESPERADAS) {
            Integer idx = colIndex.get(k);
            if (idx != null) pares.add(k + ":" + idx);
        }
        String out = String.join(", ", pares);
        // garante <= 180 chars para sobrar margem para prefixos do LogService
        return out.length() > 180 ? out.substring(0, 180) + "..." : out;
    }
}