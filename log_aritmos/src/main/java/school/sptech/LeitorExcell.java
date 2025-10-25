package school.sptech;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Consumer;

public class LeitorExcell {

    // Nomes canônicos esperados
    private static final Set<String> COLS_ESPERADAS = Set.of(
            "estado", "mes", "ano",
            "qtdaeroportos", "numvoosregulares", "numvoosirregulares",
            "numembarques", "numdesembarques", "numvoostotais"
    );

    // Aliases -> canônico (todos normalizados)
    private static final Map<String, String> ALIASES;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("estado","estado");
        m.put("mes","mes");
        m.put("ano","ano");

        m.put("qtdaeroportos","qtdaeroportos");
        m.put("qtddeaeroportos","qtdaeroportos");
        m.put("qtd_aeroportos","qtdaeroportos");
        m.put("qtdaeroporto","qtdaeroportos");
        m.put("numerodeaeroportos","qtdaeroportos");

        m.put("numvoosregulares","numvoosregulares");
        m.put("voosregulares","numvoosregulares");
        m.put("nvoosregulares","numvoosregulares");
        m.put("qtdevoosregulares","numvoosregulares");
        m.put("voos_regulares","numvoosregulares");

        m.put("numvoosirregulares","numvoosirregulares");
        m.put("voosirregulares","numvoosirregulares");
        m.put("nvoosirregulares","numvoosirregulares");
        m.put("qtdevoosirregulares","numvoosirregulares");
        m.put("voos_irregulares","numvoosirregulares");

        m.put("numembarques","numembarques");
        m.put("embarques","numembarques");
        m.put("qtdembarques","numembarques");

        m.put("numdesembarques","numdesembarques");
        m.put("desembarques","numdesembarques");
        m.put("qtddesembarques","numdesembarques");

        m.put("numvoostotais","numvoostotais");
        m.put("voostotais","numvoostotais");
        m.put("totaldevoos","numvoostotais");
        m.put("total","numvoostotais");
        m.put("voos_total","numvoostotais");

        ALIASES = Collections.unmodifiableMap(m);
    }

    private static final int DEFAULT_BATCH = 1000;
    private static final int MAX_SCAN_HEADER_ROWS = 30;

    public void processar(java.nio.file.Path caminhoXlsx,
                          int batchSize,
                          Consumer<List<Voo>> loteHandler,
                          LogService logService) throws Exception {

        if (batchSize <= 0) batchSize = DEFAULT_BATCH;

        try (java.io.InputStream in = java.nio.file.Files.newInputStream(caminhoXlsx);
             Workbook wb = new XSSFWorkbook(in)) {

            Sheet sheet = localizarAbaComCabecalho(wb);
            if (sheet == null) {
                throw new IllegalStateException("Não encontrei nenhuma aba com o cabeçalho esperado.");
            }

            DataFormatter fmt = new DataFormatter(new Locale("pt", "BR"));

            int headerRowNum = localizarLinhaCabecalho(sheet, fmt, COLS_ESPERADAS, MAX_SCAN_HEADER_ROWS);
            Row header = sheet.getRow(headerRowNum);

            Map<String, Integer> colIndex = mapearCabecalho(header, fmt);
            validarCabecalho(colIndex);

            // Log do mapeamento (curto para caber no VARCHAR(200))
            logService.registrar("INFO",
                    String.format("Usando aba '%s', header na linha %d. Mapeamento: %s",
                            sheet.getSheetName(), headerRowNum, mapeamentoCurto(colIndex)));

            // 👉 Auto‑correção da coluna "Total"
            corrigirColunaTotalSeNecessario(sheet, headerRowNum, fmt, colIndex, logService);

            // Leitura em lotes
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

                // Fallback suave: se total vier nulo, preenche com reg+irr
                if (v.getNumVoosTotais() == null &&
                        v.getNumVoosRegulares() != null &&
                        v.getNumVoosIrregulares() != null) {
                    v.setNumVoosTotais(v.getNumVoosRegulares() + v.getNumVoosIrregulares());
                }

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

    /** Escolhe a primeira aba que contenha linha com todas as colunas esperadas. */
    private Sheet localizarAbaComCabecalho(Workbook wb) {
        DataFormatter fmt = new DataFormatter(new Locale("pt", "BR"));
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet s = wb.getSheetAt(i);
            try {
                localizarLinhaCabecalho(s, fmt, COLS_ESPERADAS, MAX_SCAN_HEADER_ROWS);
                return s;
            } catch (Exception ignore) { }
        }
        return null;
    }

    /** Procura a linha de cabeçalho nas primeiras N linhas. */
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

    /** Mapeia rótulos -> índices resolvendo sinônimos; retorna apenas canônicos esperados. */
    private Map<String, Integer> mapearCabecalho(Row header, DataFormatter fmt) {
        Map<String, Integer> map = new HashMap<>();
        short lastCell = header.getLastCellNum();
        for (int c = 0; c < lastCell; c++) {
            Cell cell = header.getCell(c);
            if (cell == null) continue;
            String bruto = fmt.formatCellValue(cell);
            if (bruto == null || bruto.trim().isEmpty()) continue;

            String norm = normalizar(bruto);
            String canon = ALIASES.getOrDefault(norm, norm);
            if (COLS_ESPERADAS.contains(canon)) {
                map.putIfAbsent(canon, c); // primeira ocorrência prevalece
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

    /** Heurística: se "Total" divergir maciçamente de (Reg + Irr), tenta corrigir coluna alvo. */
    private void corrigirColunaTotalSeNecessario(Sheet sheet, int headerRowNum, DataFormatter fmt,
                                                 Map<String, Integer> colIndex, LogService logService) {
        // Se já temos as três colunas, avalia divergência numa amostra de linhas
        Integer iReg = colIndex.get("numvoosregulares");
        Integer iIrr = colIndex.get("numvoosirregulares");
        Integer iTot = colIndex.get("numvoostotais");
        if (iReg == null || iIrr == null || iTot == null) return;

        List<Integer> candidatos = new ArrayList<>();
        candidatos.add(iTot); // atual
        // Candidatos comuns de erro de mapeamento: desembarques, embarques, vizinhos
        Integer iDes = colIndex.get("numdesembarques");
        Integer iEmb = colIndex.get("numembarques");
        if (iDes != null) candidatos.add(iDes);
        if (iEmb != null) candidatos.add(iEmb);
        if (iTot - 1 >= 0) candidatos.add(iTot - 1);
        candidatos.add(iTot + 1);

        // Avalia divergência por candidato
        int amostrasMax = Math.min(80, sheet.getLastRowNum() - headerRowNum);
        if (amostrasMax <= 0) return;

        Map<Integer, Integer> divergencias = new HashMap<>(); // idx -> qtd divergente
        for (Integer idxCand : candidatos) {
            int diverg = 0, vistos = 0;
            for (int r = headerRowNum + 1; r <= headerRowNum + amostrasMax; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Integer reg = getInt(row, iReg, fmt);
                Integer irr = getInt(row, iIrr, fmt);
                Integer tot = getInt(row, idxCand, fmt);
                if (reg == null || irr == null || tot == null) continue;

                vistos++;
                if (tot.intValue() != reg + irr) diverg++;
            }
            if (vistos > 0) divergencias.put(idxCand, diverg);
        }

        if (divergencias.isEmpty()) return;

        // Escolhe o candidato com MENOR divergência
        Integer melhorIdx = null;
        double melhorTaxa = Double.MAX_VALUE;
        int totalVistosRef = 0;

        for (Map.Entry<Integer, Integer> e : divergencias.entrySet()) {
            int idx = e.getKey();
            int diverg = e.getValue();
            // recomputa vistos para o idx (para taxa correta)
            int vistos = 0;
            for (int r = headerRowNum + 1; r <= headerRowNum + amostrasMax; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Integer reg = getInt(row, iReg, fmt);
                Integer irr = getInt(row, iIrr, fmt);
                Integer tot = getInt(row, idx, fmt);
                if (reg == null || irr == null || tot == null) continue;
                vistos++;
            }
            if (vistos == 0) continue;
            double taxa = (double) diverg / vistos;
            if (taxa < melhorTaxa) {
                melhorTaxa = taxa;
                melhorIdx = idx;
                totalVistosRef = vistos;
            }
        }

        if (melhorIdx == null) return;

        // Se a taxa do "melhor" for bem menor que a do atual, troca e loga
        double taxaAtual = taxa(divergencias, iTot, headerRowNum, sheet, fmt, iReg, iIrr, amostrasMax);
        if (melhorIdx != iTot && melhorTaxa + 0.05 < taxaAtual) { // 5 p.p. de folga
            colIndex.put("numvoostotais", melhorIdx);
            logService.registrar("WARN",
                    String.format("Auto-correção: coluna 'Total' ajustada de idx %d para %d (divergência %.0f%%→%.0f%% em %d amostras).",
                            iTot, melhorIdx, taxaAtual * 100, melhorTaxa * 100, totalVistosRef));
        }
    }

    private double taxa(Map<Integer, Integer> divergencias, int idx,
                        int headerRowNum, Sheet sheet, DataFormatter fmt,
                        int iReg, int iIrr, int amostrasMax) {
        Integer div = divergencias.get(idx);
        if (div == null) return 1.0;
        int vistos = 0;
        for (int r = headerRowNum + 1; r <= headerRowNum + amostrasMax; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Integer reg = getInt(row, iReg, fmt);
            Integer irr = getInt(row, iIrr, fmt);
            Integer tot = getInt(row, idx, fmt);
            if (reg == null || irr == null || tot == null) continue;
            vistos++;
        }
        if (vistos == 0) return 1.0;
        return (double) div / vistos;
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
            return (int) Math.round(cell.getNumericCellValue());
        }
        String s = fmt.formatCellValue(cell);
        if (s == null || s.isBlank()) return null;
        s = s.replaceAll("[^0-9\\-]", "");
        return s.isBlank() ? null : Integer.parseInt(s);
    }

    private String normalizar(String s) {
        if (s == null) return "";
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String mapeamentoCurto(Map<String, Integer> colIndex) {
        List<String> pares = new ArrayList<>();
        for (String k : COLS_ESPERADAS) {
            Integer idx = colIndex.get(k);
            if (idx != null) pares.add(k + ":" + idx);
        }
        String out = String.join(", ", pares);
        return out.length() > 180 ? out.substring(0, 180) + "..." : out;
    }
}
