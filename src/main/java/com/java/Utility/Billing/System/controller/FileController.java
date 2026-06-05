package com.java.Utility.Billing.System.controller;

import com.java.Utility.Billing.System.dto.response.ApiResponse;
import com.java.Utility.Billing.System.dto.response.DocumentResponse;
import com.java.Utility.Billing.System.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload and download APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload a file (profile picture or document)")
    public ApiResponse<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long customerId) throws IOException {
        return ApiResponse.success("File uploaded", fileService.upload(file, userId, customerId));
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download a file by document ID")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        DocumentResponse doc = fileService.getById(id);
        Resource resource = fileService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getOriginalFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get files by user")
    public ApiResponse<List<DocumentResponse>> getByUser(@PathVariable Long userId) {
        return ApiResponse.success(fileService.getByUser(userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Delete a file")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        fileService.delete(id);
        return ApiResponse.success("File deleted");
    }
}
