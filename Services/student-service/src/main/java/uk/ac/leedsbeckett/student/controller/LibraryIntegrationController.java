package uk.ac.leedsbeckett.student.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.leedsbeckett.student.service.LibraryIntegrationService;

@RestController
@RequestMapping("/library")
public class LibraryIntegrationController {

    private LibraryIntegrationService libraryIntegrationService;

    public LibraryIntegrationController(LibraryIntegrationService LibraryIntegrationService) {
        this.libraryIntegrationService = LibraryIntegrationService;
    }

    @PostMapping("/api/register/{studentId}")
    @ResponseBody
    public ResponseEntity<?> registerAccount(@PathVariable String studentId) {
        return libraryIntegrationService.createLibraryAccountModel(studentId);
    }
}
