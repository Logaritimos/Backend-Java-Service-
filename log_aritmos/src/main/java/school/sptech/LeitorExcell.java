package school.sptech;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import java.util.function.Consumer;

public class LeitorExcell {

    private static final Set<String> COLS_ESPERADAS = Set.of(
            "estado", "mes", "ano",
            "qtdaeroportos", "numvoosregulares", "numvoosirregulares",
            "numembarques", "numdesembarques", "numvoostotais"
    );
    private static final int DEFAULT_BATCH = 1000;

    public void processar(Path caminhoXlsx,
                          int batchSize,
                          Consumer<List<Voo>> loteHandler,
                          LogService logService) throws Exception {

        if (batchSize <= 0) batchSize = DEFAULT_BATCH;

        logService.registrar("INFO",
                "Abrindo planilha de voos para processamento em lote: " + caminhoXlsx.getFileName());

        try (InputStream in = Files.newInputStream(caminhoXlsx);
             Workbook wb = new XSSFWorkbook(in)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new IllegalStateException("Não encontrei a primeira aba no XLSX.");

            Map<String, Integer> colIndex = mapearCabecalho(sheet.getRow(0));
            validarCabecalho(colIndex);

            DataFormatter fmt = new DataFormatter(new Locale("pt", "BR"));
            List<Voo> buffer = new ArrayList<>(batchSize);

            int linhas = sheet.getPhysicalNumberOfRows();
            int lidas = 0;
            for (int r = 1; r < linhas; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                if (linhaVazia(row, 3)) continue;

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
                lidas++;

                if (buffer.size() == batchSize) {
                    loteHandler.accept(buffer);
                    buffer.clear();
                    if (lidas % (batchSize * 5) == 0) {
                        logService.registrar("INFO",
                                String.format("Progresso: %d linhas processadas...", lidas));
                    }
                }
            }
            if (!buffer.isEmpty()) {
                loteHandler.accept(buffer);
            }

            logService.registrar("SUCCESS",
                    String.format("Leitura concluída. Total de linhas processadas: %d.", lidas));
        }
    }

    private Map<String, Integer> mapearCabecalho(Row header) {
        if (header == null) throw new IllegalStateException("Cabeçalho ausente na linha 0.");
        Map<String, Integer> map = new HashMap<>();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            Cell cell = header.getCell(c);
            if (cell == null) continue;
            String nome = normalizar(cell.getStringCellValue());
            if (!nome.isBlank()) map.put(nome, c);
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

    private boolean linhaVazia(Row row, int primeirasColunas) {
        DataFormatter fmt = new DataFormatter();
        for (int i = 0; i < primeirasColunas; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String v = fmt.formatCellValue(cell);
                if (v != null && !v.trim().isEmpty()) return false;
            }
        }
        return true;
    }

    private String getString(Row row, Integer col, DataFormatter fmt) {
        if (col == null) return null;
        Cell cell = row.getCell(col);
        String v = (cell == null) ? null : fmt.formatCellValue(cell);
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
        s = s.replaceAll("[^0-9-]", "");
        return s.isBlank() ? null : Integer.parseInt(s);
    }

    private String normalizar(String s) {
        if (s == null) return "";
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}