package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.dto.response.DocumentResponse;
import com.java.Utility.Billing.System.entity.Customer;
import com.java.Utility.Billing.System.entity.Document;
import com.java.Utility.Billing.System.entity.User;
import com.java.Utility.Billing.System.exception.BadRequestException;
import com.java.Utility.Billing.System.exception.ResourceNotFoundException;
import com.java.Utility.Billing.System.mapper.EntityMapper;
import com.java.Utility.Billing.System.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final DocumentRepository documentRepository;
    private final UserService userService;
    private final CustomerService customerService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Transactional
    public DocumentResponse upload(MultipartFile file, Long userId, Long customerId) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(storedFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        User user = userId != null ? userService.findUser(userId) : null;
        Customer customer = customerId != null ? customerService.findCustomer(customerId) : null;

        Document document = Document.builder()
                .fileName(storedFileName)
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(filePath.toString())
                .user(user)
                .customer(customer)
                .build();

        document = documentRepository.save(document);
        log.info("File uploaded: {}", storedFileName);
        return EntityMapper.toDocumentResponse(document);
    }

    public Resource download(Long id) throws MalformedURLException {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        Path path = Paths.get(document.getFilePath());
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) {
            throw new ResourceNotFoundException("File not found on disk");
        }
        return resource;
    }

    public DocumentResponse getById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        return EntityMapper.toDocumentResponse(document);
    }

    public List<DocumentResponse> getByUser(Long userId) {
        return documentRepository.findByUserId(userId).stream()
                .map(EntityMapper::toDocumentResponse).toList();
    }

    @Transactional
    public void delete(Long id) throws IOException {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        Files.deleteIfExists(Paths.get(document.getFilePath()));
        documentRepository.delete(document);
    }
}
