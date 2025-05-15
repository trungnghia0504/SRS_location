package com.example.hcm25_cpl_ks_java_01_lms.location;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Location')")
@Tag(name = "Location", description = "Location management API")
public class LocationAPIController {
    
    private final LocationService locationService;

    public LocationAPIController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    @Operation(summary = "Get all locations", description = "Get a paginated list of locations with optional filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved locations",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    public ResponseEntity<Page<Location>> listLocations(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Optional search term") @RequestParam(required = false) String searchTerm) {
        Page<Location> locations = locationService.getAllLocations(searchTerm, page, size);
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get location by ID", description = "Get location details by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved location",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Location.class))),
            @ApiResponse(responseCode = "404", description = "Location not found")
    })
    public ResponseEntity<Location> getLocationById(
            @Parameter(description = "Location ID", required = true) @PathVariable Long id) {
        Location location = locationService.getLocationById(id);
        if (location != null) {
            return ResponseEntity.ok(location);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Create location", description = "Create a new location")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Location created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Location.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - validation error")
    })
    public ResponseEntity<?> createLocation(
            @Parameter(description = "Location data", required = true) @RequestBody Location location) {
        try {
            Location savedLocation = locationService.createLocation(location);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedLocation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update location", description = "Update an existing location by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Location updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Location.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - validation error"),
            @ApiResponse(responseCode = "404", description = "Location not found")
    })
    public ResponseEntity<?> updateLocation(
            @Parameter(description = "Location ID", required = true) @PathVariable Long id,
            @Parameter(description = "Updated location data", required = true) @RequestBody Location locationDetails) {
        try {
            Location existingLocation = locationService.getLocationById(id);
            if (existingLocation == null) {
                return ResponseEntity.notFound().build();
            }
            
            locationDetails.setId(id);
            Location updatedLocation = locationService.updateLocation(locationDetails);
            return ResponseEntity.ok(updatedLocation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete location", description = "Delete a location by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Location deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Location not found")
    })
    public ResponseEntity<?> deleteLocation(
            @Parameter(description = "Location ID", required = true) @PathVariable Long id) {
        try {
            Location location = locationService.getLocationById(id);
            if (location == null) {
                return ResponseEntity.notFound().build();
            }
            
            locationService.deleteLocation(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/export")
    @Operation(summary = "Export locations to Excel", description = "Export locations to Excel file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel file generated successfully",
                    content = @Content(mediaType = "application/vnd.ms-excel")),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Resource> exportToExcel(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
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
    @Operation(summary = "Import locations from Excel", description = "Import locations from an Excel file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Locations imported successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid file format or empty file")
    })
    public ResponseEntity<?> importExcel(
            @Parameter(description = "Excel file to import", required = true) @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload");
        }

        try {
            List<Location> locations = LocationExcelImporter.importLocations(file.getInputStream());
            locationService.saveAllFromExcel(locations);
            return ResponseEntity.ok("Successfully uploaded and imported data");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to import data from file");
        }
    }

    @GetMapping("/download-template")
    @Operation(summary = "Download template Excel file", description = "Download an Excel template for locations import")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template downloaded successfully",
                    content = @Content(mediaType = "application/vnd.ms-excel")),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Resource> downloadExcelTemplate() {
        try {
            // Path to the template file
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
}