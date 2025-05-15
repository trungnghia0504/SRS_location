package com.example.hcm25_cpl_ks_java_01_lms.location;

import com.example.hcm25_cpl_ks_java_01_lms.department.Department;
import com.example.hcm25_cpl_ks_java_01_lms.role.RoleRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LocationService {
    private final LocationRepository locationRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public LocationService(LocationRepository locationRepository, RoleRepository roleRepository) {
        this.locationRepository = locationRepository;
        this.roleRepository = roleRepository;
    }

    public Page<Location> getAllLocations(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (searchTerm != null && !searchTerm.isEmpty()) {
            return locationRepository.findByNameContainingIgnoreCase(searchTerm, pageable);
        }
        return locationRepository.findAll(pageable);
    }

    public Location createLocation(Location location) {
        if (locationRepository.findByName(location.getName()).isPresent()) {
            throw new IllegalArgumentException("Location with name '" + location.getName() + "' already exists");
        }
        return locationRepository.save(location);
    }

    public Location getLocationById(Long id) {
        return locationRepository.findById(id).orElse(null);
    }

    public Location updateLocation(Location locationDetails) {
        Optional<Location> byName = locationRepository.findByName(locationDetails.getName());
        if (byName.isPresent() && !byName.get().getId().equals(locationDetails.getId())) {
            throw new IllegalArgumentException("Location with name '" + locationDetails.getName() + "' already exists");
        }
        return locationRepository.save(locationDetails);
    }

    public void deleteLocation(Long id) {
        locationRepository.deleteById(id);
    }

    public ByteArrayInputStream exportToExcel(List<Location> locations) throws IOException {
        return generateExcel(locations);
    }

    private ByteArrayInputStream generateExcel(List<Location> locations) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Locations");

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerCellStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerCellStyle.setFont(font);

            String[] headers = {"ID", "Name", "Address"};
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerCellStyle);
            }

            // Create data rows
            int rowIdx = 1;
            for (Location location : locations) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(location.getId());
                row.createCell(1).setCellValue(location.getName());
                row.createCell(2).setCellValue(location.getAddress());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public void saveAllFromExcel(List<Location> locations) {
        locationRepository.saveAll(locations);
    }

    public List<Map<String, Object>> getLocationOfDepartment() {
        return locationRepository.findAll()
                .stream()
                .map(name -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", name.getId());
                    userMap.put("name", name.getName());
                    return userMap;
                })
                .collect(Collectors.toList());
    }

    public Page<Location> getLocations(String searchTerm, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Invalid page or size parameters");
        }
        Pageable pageable = PageRequest.of(page, size);
        return searchTerm != null && !searchTerm.trim().isEmpty() ?
                locationRepository.findByNameContainingIgnoreCase(searchTerm.trim(), pageable) :
                locationRepository.findAll(pageable);
    }
}
