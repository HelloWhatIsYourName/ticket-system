package com.example.aiticket.knowledge.web;

import com.example.aiticket.common.api.ApiResponse;
import com.example.aiticket.knowledge.domain.KnowledgeDocument;
import com.example.aiticket.knowledge.domain.KnowledgeParseStatus;
import com.example.aiticket.knowledge.mapper.KnowledgeChunkMapper;
import com.example.aiticket.knowledge.service.KnowledgeDocumentService;
import com.example.aiticket.knowledge.service.KnowledgeIngestionService;
import com.example.aiticket.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.List;

@RestController
@RequestMapping("/api/kb/documents")
public class KnowledgeDocumentController {
    private static final long MAX_UPLOAD_BYTES = 200_000;

    private final KnowledgeDocumentService documentService;
    private final KnowledgeIngestionService ingestionService;
    private final KnowledgeChunkMapper chunkMapper;

    public KnowledgeDocumentController(KnowledgeDocumentService documentService,
                                       KnowledgeIngestionService ingestionService,
                                       KnowledgeChunkMapper chunkMapper) {
        this.documentService = documentService;
        this.ingestionService = ingestionService;
        this.chunkMapper = chunkMapper;
    }

    @PostMapping("/text")
    @PreAuthorize("hasAuthority('knowledge:document:upload')")
    public ApiResponse<DocumentResponse> createTextDocument(@Valid @RequestBody CreateTextDocumentRequest request,
                                                            @AuthenticationPrincipal AuthenticatedUser user) {
        Long categoryId = request.categoryId() == null ? 1L : request.categoryId();
        Long documentId = documentService.createTextDocument(request.title(), categoryId, request.content(), user.id());
        try {
            ingestionService.ingestText(documentId, request.title(), categoryId, request.content());
        } catch (RuntimeException ex) {
            KnowledgeDocument document = documentService.getDocument(documentId);
            if (document.parseStatus() == KnowledgeParseStatus.PENDING_PARSE) {
                throw ex;
            }
            return ApiResponse.ok(DocumentResponse.from(document));
        }
        return ApiResponse.ok(DocumentResponse.from(documentService.getDocument(documentId)));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('knowledge:document:upload')")
    public ApiResponse<DocumentResponse> uploadDocument(@RequestParam("file") MultipartFile file,
                                                        @RequestParam(required = false) String title,
                                                        @RequestParam(required = false) Long categoryId,
                                                        @AuthenticationPrincipal AuthenticatedUser user) {
        validateUpload(file);
        String fileName = safeFileName(file.getOriginalFilename());
        String fileType = fileType(fileName);
        String normalizedTitle = normalizeUploadTitle(title, fileName);
        String content = readUtf8(file);
        if (content.isBlank()) {
            throw new IllegalArgumentException("uploaded file content must not be blank");
        }

        Long resolvedCategoryId = categoryId == null ? 1L : categoryId;
        Long documentId = documentService.createDocument(
                normalizedTitle,
                resolvedCategoryId,
                fileName,
                fileType,
                file.getSize(),
                user.id()
        );
        try {
            ingestionService.ingestText(documentId, normalizedTitle, resolvedCategoryId, content);
        } catch (RuntimeException ex) {
            KnowledgeDocument document = documentService.getDocument(documentId);
            if (document.parseStatus() == KnowledgeParseStatus.PENDING_PARSE) {
                throw ex;
            }
            return ApiResponse.ok(DocumentResponse.from(document));
        }
        return ApiResponse.ok(DocumentResponse.from(documentService.getDocument(documentId)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('knowledge:document:view')")
    public ApiResponse<List<DocumentResponse>> listDocuments() {
        return ApiResponse.ok(documentService.listRecent(100).stream().map(DocumentResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('knowledge:document:view')")
    public ApiResponse<DocumentResponse> getDocument(@PathVariable Long id) {
        return ApiResponse.ok(DocumentResponse.from(documentService.getDocument(id)));
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('knowledge:document:manage')")
    public ApiResponse<DocumentResponse> enable(@PathVariable Long id) {
        documentService.setEnabled(id, true);
        return ApiResponse.ok(DocumentResponse.from(documentService.getDocument(id)));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('knowledge:document:manage')")
    public ApiResponse<DocumentResponse> disable(@PathVariable Long id) {
        documentService.setEnabled(id, false);
        return ApiResponse.ok(DocumentResponse.from(documentService.getDocument(id)));
    }

    @PostMapping("/{id}/retry-parse")
    @PreAuthorize("hasAuthority('knowledge:document:manage')")
    public ApiResponse<DocumentResponse> retryParse(@PathVariable Long id) {
        documentService.resetForRetry(id);
        return ApiResponse.ok(DocumentResponse.from(documentService.getDocument(id)));
    }

    @GetMapping("/{id}/chunks")
    @PreAuthorize("hasAuthority('knowledge:document:view')")
    public ApiResponse<List<ChunkResponse>> chunks(@PathVariable Long id) {
        documentService.getDocument(id);
        return ApiResponse.ok(chunkMapper.findByDocumentId(id).stream().map(ChunkResponse::from).toList());
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("uploaded file must not be empty");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("uploaded file is too large");
        }
        String fileName = safeFileName(file.getOriginalFilename());
        if (!isAllowedFileName(fileName)) {
            throw new IllegalArgumentException("unsupported knowledge file type");
        }
    }

    private String readUtf8(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalArgumentException("uploaded file cannot be read", ex);
        }
    }

    private String normalizeUploadTitle(String title, String fileName) {
        String normalized = title == null || title.isBlank() ? fileStem(fileName) : title.trim();
        if (normalized.length() > 200) {
            throw new IllegalArgumentException("title must not exceed 200 characters");
        }
        return normalized;
    }

    private String safeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("uploaded file name must not be blank");
        }
        String normalized = originalFilename.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private boolean isAllowedFileName(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown");
    }

    private String fileType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".txt") ? "TEXT" : "MARKDOWN";
    }

    private String fileStem(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return fileName;
        }
        return fileName.substring(0, dot);
    }
}
