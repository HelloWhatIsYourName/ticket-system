package com.example.aiticket.knowledge.service;

import com.example.aiticket.knowledge.domain.KnowledgeDocument;
import com.example.aiticket.knowledge.mapper.KnowledgeDocumentMapper;
import com.example.aiticket.knowledge.queue.KnowledgeParseQueue;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class KnowledgeDocumentService {
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeParseQueue parseQueue;

    public KnowledgeDocumentService(KnowledgeDocumentMapper documentMapper, KnowledgeParseQueue parseQueue) {
        this.documentMapper = documentMapper;
        this.parseQueue = parseQueue;
    }

    public Long createTextDocument(String title, Long categoryId, String content, Long uploadedBy) {
        int fileSize = content == null ? 0 : content.getBytes(StandardCharsets.UTF_8).length;
        return createDocument(title, categoryId, title, "TEXT", fileSize, uploadedBy);
    }

    public Long createDocument(String title,
                               Long categoryId,
                               String fileName,
                               String fileType,
                               long fileSize,
                               Long uploadedBy) {
        Long documentId = documentMapper.nextDocumentId();
        documentMapper.insertDocument(documentId, title, categoryId, fileName, fileType, fileSize, uploadedBy);
        parseQueue.enqueueParseAndEmbed(documentId, 0);
        return documentId;
    }

    public KnowledgeDocument getDocument(Long id) {
        KnowledgeDocument document = documentMapper.findById(id);
        if (document == null) {
            throw new KnowledgeDocumentNotFoundException(id);
        }
        return document;
    }

    public List<KnowledgeDocument> listRecent(int limit) {
        return documentMapper.findRecent(limit);
    }

    public void setEnabled(Long id, boolean enabled) {
        if (documentMapper.updateEnabled(id, enabled ? 1 : 0) == 0) {
            throw new KnowledgeDocumentNotFoundException(id);
        }
    }

    public void resetForRetry(Long id) {
        if (documentMapper.resetForRetry(id) == 0) {
            throw new KnowledgeDocumentNotFoundException(id);
        }
        parseQueue.enqueueParseAndEmbed(id, 0);
    }
}
