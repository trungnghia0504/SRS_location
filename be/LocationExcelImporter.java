package com.example.hcm25_cpl_ks_java_01_lms.location;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LocationExcelImporter {
    public static List<Location> importLocations(InputStream inputStream) throws IOException, IOException {
        List<Location> locations = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);

        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();

        int rowNumber = 0;
        while (rows.hasNext()) {
            Row currentRow = rows.next();
            // Skip header
            if (rowNumber == 0) {
                rowNumber++;
                continue;
            }

            Iterator<Cell> cellsInRow = currentRow.iterator();

            Location location = new Location();
            int cellIdx = 0;
            while (cellsInRow.hasNext()) {
                Cell currentCell = cellsInRow.next();
                switch (cellIdx) {
                    case 0:
                        location.setName(currentCell.getStringCellValue());
                        break;
                    case 1:
                        location.setAddress(currentCell.getStringCellValue());
                        break;
                    default:
                        break;
                }
                cellIdx++;
            }

            locations.add(location);
        }

        workbook.close();
        return locations;
    }
}
