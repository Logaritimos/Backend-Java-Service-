package school.sptech;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;

public class LeitorExcell {

    public void processar(Path caminhoXlsx, Conexao conexao, LogService logService) throws Exception {
        try (InputStream in = Files.newInputStream(caminhoXlsx);
             Workbook wb = new XSSFWorkbook(in)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new IllegalStateException("Aba 0 inexistente no XLSX.");

            DataFormatter fmt = new DataFormatter();
            int linhas = sheet.getPhysicalNumberOfRows();

            logService.registrar("INFO", "Iniciando leitura. Total de linhas: " + linhas);

            for (int r = 1; r < linhas; r++) { // pula cabeçalho (linha 0)
                Row row = sheet.getRow(r);
                if (row == null || linhaVazia(row, fmt)) {
                    logService.registrar("INFO", "Linha " + r + " vazia. Encerrando leitura.");
                    break; // encerra se linha vazia
                }

                Voo v = new Voo();
                v.setEstado(removerAcentos(fmt.formatCellValue(row.getCell(0))));
                v.setMes(removerAcentos(fmt.formatCellValue(row.getCell(1))));
                v.setAno(getInt(row.getCell(2), fmt));
                v.setQtdAeroportos(getInt(row.getCell(3), fmt));
                v.setNumVoosRegulares(getInt(row.getCell(4), fmt));
                v.setNumVoosIrregulares(getInt(row.getCell(5), fmt));
                v.setNumEmbarques(getInt(row.getCell(6), fmt));
                v.setNumDesembarques(getInt(row.getCell(7), fmt));
                v.setNumVoosTotais(getInt(row.getCell(8), fmt));

                // Validação simples: se campos obrigatórios faltarem, pula
                if (v.getEstado() == null || v.getMes() == null || v.getAno() == null) {
                    logService.registrar("WARN", "Linha " + r + " inválida. Pulando.");
                    continue;
                }

                // Regra extra: se total == 1, corrige para reg + irr
                if (v.getNumVoosTotais() != null && v.getNumVoosTotais() == 1 &&
                        v.getNumVoosRegulares() != null && v.getNumVoosIrregulares() != null) {
                    v.setNumVoosTotais(v.getNumVoosRegulares() + v.getNumVoosIrregulares());
                }

                // Salva imediatamente no banco
                try {
                    conexao.getJdbcTemplate().update("""
                        INSERT INTO voo (estado, mes, ano, qtdAeroportos, numVoosRegulares, numVoosIrregulares,
                        numEmbarques, numDesembarques, numVoosTotais)
                        VALUES (?,?,?,?,?,?,?,?,?)
                        """,
                            v.getEstado(), v.getMes(), v.getAno(), v.getQtdAeroportos(),
                            v.getNumVoosRegulares(), v.getNumVoosIrregulares(),
                            v.getNumEmbarques(), v.getNumDesembarques(), v.getNumVoosTotais()
                    );
                } catch (Exception e) {
                    logService.registrar("ERROR", "Falha ao inserir linha " + r + ": " + e.getMessage());
                }
            }

            logService.registrar("INFO", "Leitura concluída.");
        }
    }

    private boolean linhaVazia(Row row, DataFormatter fmt) {
        for (int i = 0; i < 3; i++) { // verifica primeiras colunas
            String v = fmt.formatCellValue(row.getCell(i));
            if (v != null && !v.trim().isEmpty()) return false;
        }
        return true;
    }

    private Integer getInt(Cell cell, DataFormatter fmt) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) Math.round(cell.getNumericCellValue());
        }
        String s = fmt.formatCellValue(cell).replaceAll("[^0-9-]", "");
        return s.isEmpty() ? null : Integer.parseInt(s);
    }

    private String removerAcentos(String s) {
        if (s == null) return null;
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .trim();
    }
}
