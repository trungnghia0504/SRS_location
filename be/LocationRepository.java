package com.example.hcm25_cpl_ks_java_01_lms.location;

import com.example.hcm25_cpl_ks_java_01_lms.course.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Page<Location> findByNameContainingIgnoreCase(String searchTerm, Pageable pageable);
    Optional<Location> findByName(String name);
}
