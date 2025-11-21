package school.sptech;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static school.sptech.VooService.semAcento;

public class LeitorExcell extends LeitorArquivo {

    private static final int MAX_VAZIAS_SEGUIDAS = 5;

    @Override
    public void processar(Path caminhoXlsx, Conexao conexao, LogService logService) throws Exception {
        try (InputStream in = Files.newInputStream(caminhoXlsx);
             Workbook wb = new XSSFWorkbook(in)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new IllegalStateException("Aba 0 inexistente no XLSX.");

            DataFormatter fmt = new DataFormatter();
            int lastRow = sheet.getLastRowNum();

            int inseridas = 0, puladas = 0, erros = 0, vaziasSeguidas = 0;

            logService.registrar("INFO",
                    "Leitura simplificada: lastRow=" + lastRow + " (dados começam na linha 1)");

            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);

                if (linhaVazia(row, fmt)) {
                    vaziasSeguidas++;
                    if (vaziasSeguidas >= MAX_VAZIAS_SEGUIDAS) {
                        logService.registrar("INFO",
                                "Fim detectado após " + MAX_VAZIAS_SEGUIDAS + " linhas vazias seguidas (linha " + r + ").");
                        break;
                    }
                    continue;
                } else {
                    vaziasSeguidas = 0;
                }

                Voo v = new Voo();
                v.setEstado(getStr(cell(row, 0), fmt));
                v.setMes(getStr(cell(row, 1), fmt));
                v.setAno(getInt(cell(row, 2), fmt));
                v.setQtdAeroportos(getInt(cell(row, 3), fmt));
                v.setNumVoosRegulares(getInt(cell(row, 4), fmt));
                v.setNumVoosIrregulares(getInt(cell(row, 5), fmt));
                v.setNumEmbarques(getInt(cell(row, 6), fmt));
                v.setNumDesembarques(getInt(cell(row, 7), fmt));
                v.setNumVoosTotais(getInt(cell(row, 8), fmt));

                if (v.getNumVoosTotais() != null && v.getNumVoosTotais() == 1
                        && v.getNumVoosRegulares() != null && v.getNumVoosIrregulares() != null) {
                    v.setNumVoosTotais(v.getNumVoosRegulares() + v.getNumVoosIrregulares());
                }

                if (isBlank(v.getEstado()) || isBlank(v.getMes()) || v.getAno() == null) {
                    puladas++;
                    continue;
                }

                try {
                    conexao.inserirVoo(v);
                    inseridas++;
                } catch (Exception e) {
                    erros++;
                    logService.registrar("ERROR",
                            "Falha ao inserir linha " + r + ": " + safe(e.getMessage(), 160));
                }
            }

            logService.registrar("INFO",
                    "Leitura concluída. Inseridas=" + inseridas + " | Puladas=" + puladas + " | Erros=" + erros);
        }
    }

    private Cell cell(Row row, int idx) {
        if (row == null || idx < 0) return null;
        return row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    }

    private boolean linhaVazia(Row row, DataFormatter fmt) {
        if (row == null) return true;
        for (int c = 0; c <= 2; c++) {
            Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null) {
                String v = fmt.formatCellValue(cell);
                if (v != null && !v.trim().isEmpty()) return false;
            }
        }
        return true;
    }

    private Integer getInt(Cell cell, DataFormatter fmt) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) Math.round(cell.getNumericCellValue());
        }
        String s = fmt.formatCellValue(cell);
        if (s == null) return null;
        s = s.replaceAll("[^0-9-]", "");
        return s.isEmpty() ? null : Integer.parseInt(s);
    }

    private String getStr(Cell cell, DataFormatter fmt) {
        if (cell == null) return null;
        String s = fmt.formatCellValue(cell);
        return isBlank(s) ? null : semAcento(s);
    }
}