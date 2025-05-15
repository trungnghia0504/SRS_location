package com.example.hcm25_cpl_ks_java_01_lms.location;

import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
import com.example.hcm25_cpl_ks_java_01_lms.department.DepartmentController;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/locations")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Location')")
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public String listLocations(Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String searchTerm) {
        Page<Location> locations = locationService.getAllLocations(searchTerm, page, size);
        model.addAttribute("locations", locations);
        model.addAttribute("content", "locations/list");
        return Constants.LAYOUT;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("location", new Location());
        model.addAttribute("content", "locations/create");
        return Constants.LAYOUT;
    }

    @PostMapping
    public String createLocation(@ModelAttribute Location location, Model model) {
        try {
            locationService.createLocation(location);
            return "redirect:/locations";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("location", location);
            model.addAttribute("content", "locations/create");
            return Constants.LAYOUT;
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Location location = locationService.getLocationById(id);
        if (location != null) {
            model.addAttribute("location", location);
            model.addAttribute("content", "locations/update");
            return Constants.LAYOUT;
        }
        return "redirect:/locations";
    }

    @PostMapping("/edit/{id}")
    public String updateLocation(@PathVariable Long id, @ModelAttribute Location locationDetails, Model model) {
        try {
            locationDetails.setId(id);
            locationService.updateLocation(locationDetails);
            return "redirect:/locations";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("location", locationDetails);
            model.addAttribute("content", "locations/update");
            return Constants.LAYOUT;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return "redirect:/locations";
    }

    @GetMapping("/export")
    public ResponseEntity<Resource> exportToExcel(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<Location> locations = locationService.getAllLocations("", page, size).getContent();
            ByteArrayInputStream in = locationService.exportToExcel(locations);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=locations.xlsx");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                    .body(new InputStreamResource(in));
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('Admin')")
    public String importExcel(@RequestParam("file") MultipartFile file, Model model) {
        model.addAttribute("content", "locations/list");
        if (file.isEmpty()) {
            model.addAttribute("error", "Please select a file to upload");
            return Constants.LAYOUT;
        }

        try {
            List<Location> locations = LocationExcelImporter.importLocations(file.getInputStream());
            locationService.saveAllFromExcel(locations);
            model.addAttribute("success", "Successfully uploaded and imported data");
            return "redirect:/locations";
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to import data from file");
            return Constants.LAYOUT;
        }
    }

    @GetMapping("/print")
    public String printLocation(Model model) {
        model.addAttribute("locations", locationService.getLocations("", 0, Integer.MAX_VALUE));
        return "locations/print";
    }

    @GetMapping("/download-template")
    public ResponseEntity<Resource> downloadExcelTemplate() {
        try {
            // Đường dẫn tương đối từ thư mục gốc của project
            Path filePath = Paths.get("data-excel/location_template.xlsx");
            Resource resource = new ByteArrayResource(Files.readAllBytes(filePath));

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=location_template.xlsx");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/delete-all")
    @Transactional
    public ResponseEntity<String> deleteSelectedLocations(@RequestBody LocationController.DeleteRequest deleteRequest, Model model) {
        try {
            List<Long> ids = deleteRequest.getIds();
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body("No locations selected for deletion");
            }
            for (Long id : ids) {
                locationService.deleteLocation(id);
            }
            return ResponseEntity.ok("Locations deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete locations: " + e.getMessage());
        }
    }

    // Class để nhận dữ liệu từ request body
    public static class DeleteRequest {
        private List<Long> ids;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }
}
