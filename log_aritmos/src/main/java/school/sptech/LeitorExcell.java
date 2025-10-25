package school.sptech;

import org.apache.poi.ss.usermodel.*;
import java.text.Normalizer;
import java.util.*;

public class LeitorExcell {

    private static final Set<String> COLS_ESPERADAS = Set.of(
            "estado","mes","ano",
            "qtdaeroportos","numvoosregulares","numvoosirregulares",
            "numembarques","numdesembarques","numvoostotais"
    );

    public void processar(java.nio.file.Path caminhoXlsx,
                          int batchSize,
                          java.util.function.Consumer<List<Voo>> loteHandler,
                          LogService logService) throws Exception {

        try (java.io.InputStream in = java.nio.file.Files.newInputStream(caminhoXlsx);
             Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(in)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new IllegalStateException("Aba 0 inexistente no XLSX.");

            DataFormatter fmt = new DataFormatter(new java.util.Locale("pt","BR"));

            // 👉 Achar a linha do cabeçalho varrendo as primeiras 30 linhas
            int headerRowNum = localizarLinhaCabecalho(sheet, fmt, COLS_ESPERADAS, 30);
            Row header = sheet.getRow(headerRowNum);

            Map<String,Integer> colIndex = mapearCabecalho(header, fmt);
            validarCabecalho(colIndex); // garante todas as esperadas

            List<Voo> buffer = new ArrayList<>(Math.max(1, batchSize));
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
            if (!buffer.isEmpty()) loteHandler.accept(buffer);

        }
    }

    /** Procura a primeira linha que contenha todas as colunas esperadas (normalizadas). */
    private int localizarLinhaCabecalho(Sheet sheet, DataFormatter fmt,
                                        Set<String> esperadas, int maxScan) {
        int maxRow = Math.min(sheet.getLastRowNum(), maxScan);
        for (int r = 0; r <= maxRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Map<String,Integer> idx = mapearCabecalho(row, fmt);
            if (idx.keySet().containsAll(esperadas)) {
                return r;
            }
        }
        throw new IllegalStateException("Não encontrei linha de cabeçalho nas primeiras " +
                (maxScan + 1) + " linhas. Verifique o arquivo.");
    }

    private Map<String,Integer> mapearCabecalho(Row header, DataFormatter fmt) {
        Map<String,Integer> map = new HashMap<>();
        short lastCell = header.getLastCellNum();
        for (int c = 0; c < lastCell; c++) {
            Cell cell = header.getCell(c);
            if (cell == null) continue;
            String nome = normalizar(fmt.formatCellValue(cell));
            if (!nome.isBlank()) map.put(nome, c);
        }
        return map;
    }

    private void validarCabecalho(Map<String,Integer> colIndex) {
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
        return v == null ? null : v.trim();
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
        s = s.replaceAll("[^0-9-]", "");
        return s.isBlank() ? null : Integer.parseInt(s);
    }

    private String normalizar(String s) {
        if (s == null) return "";
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", "");
    }
}