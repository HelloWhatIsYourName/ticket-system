package com.example.aiticket.knowledge.mapper;

import com.example.aiticket.knowledge.domain.KnowledgeDocument;
import com.example.aiticket.knowledge.domain.KnowledgeParseStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {
    Long nextDocumentId();

    int insertTextDocument(@Param("id") Long id,
                           @Param("title") String title,
                           @Param("categoryId") Long categoryId,
                           @Param("fileSize") long fileSize,
                           @Param("uploadedBy") Long uploadedBy);

    int insertDocument(@Param("id") Long id,
                       @Param("title") String title,
                       @Param("categoryId") Long categoryId,
                       @Param("fileName") String fileName,
                       @Param("fileType") String fileType,
                       @Param("fileSize") long fileSize,
                       @Param("uploadedBy") Long uploadedBy);

    KnowledgeDocument findById(@Param("id") Long id);

    List<KnowledgeDocument> findRecent(@Param("limit") int limit);

    int updateEnabled(@Param("id") Long id, @Param("enabled") int enabled);

    int updateParseStatus(@Param("id") Long id,
                          @Param("parseStatus") KnowledgeParseStatus parseStatus,
                          @Param("parseError") String parseError);

    int markParseFailed(@Param("id") Long id,
                        @Param("parseError") String parseError,
                        @Param("maxRetryCount") int maxRetryCount);

    int markParseFailedTerminal(@Param("id") Long id, @Param("parseError") String parseError);

    int resetForRetry(@Param("id") Long id);
}
