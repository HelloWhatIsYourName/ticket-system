package com.example.aiticket.knowledge.web;

import com.example.aiticket.common.api.ApiResponse;
import com.example.aiticket.knowledge.domain.KnowledgeChunk;
import com.example.aiticket.knowledge.domain.KnowledgeChunkDraft;
import com.example.aiticket.knowledge.domain.KnowledgeDocument;
import com.example.aiticket.knowledge.domain.KnowledgeParseStatus;
import com.example.aiticket.knowledge.domain.KnowledgeSearchResult;
import com.example.aiticket.knowledge.mapper.KnowledgeChunkMapper;
import com.example.aiticket.knowledge.mapper.KnowledgeDocumentMapper;
import com.example.aiticket.knowledge.queue.KnowledgeParseQueue;
import com.example.aiticket.knowledge.service.KnowledgeDocumentNotFoundException;
import com.example.aiticket.knowledge.service.KnowledgeDocumentService;
import com.example.aiticket.knowledge.service.KnowledgeIngestionService;
import com.example.aiticket.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeDocumentControllerTest {
    @Test
    void uploadMarkdownDocumentStoresMetadataAndIngestsUtf8Content() {
        FakeDocumentMapper documentMapper = new FakeDocumentMapper();
        RecordingIngestionService ingestionService = new RecordingIngestionService();
        KnowledgeDocumentController controller = new KnowledgeDocumentController(
                new KnowledgeDocumentService(documentMapper, new NoopParseQueue()),
                ingestionService,
                new FakeChunkMapper()
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "policy.md",
                "text/markdown",
                "密码重置步骤".getBytes(StandardCharsets.UTF_8)
        );

        ApiResponse<DocumentResponse> response = controller.uploadDocument(file, "账号政策", 1L, user());

        assertThat(response.success()).isTrue();
        assertThat(response.data().id()).isEqualTo(10L);
        assertThat(documentMapper.fileName).isEqualTo("policy.md");
        assertThat(documentMapper.fileType).isEqualTo("MARKDOWN");
        assertThat(ingestionService.documentId).isEqualTo(10L);
        assertThat(ingestionService.title).isEqualTo("账号政策");
        assertThat(ingestionService.categoryId).isEqualTo(1L);
        assertThat(ingestionService.text).isEqualTo("密码重置步骤");
    }

    @Test
    void uploadRejectsUnsupportedFileExtension() {
        KnowledgeDocumentController controller = new KnowledgeDocumentController(
                new KnowledgeDocumentService(new FakeDocumentMapper(), new NoopParseQueue()),
                new RecordingIngestionService(),
                new FakeChunkMapper()
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "policy.pdf",
                "application/pdf",
                "pdf".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> controller.uploadDocument(file, "账号政策", 1L, user()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported");
    }

    @Test
    void uploadRejectsOversizedFile() {
        KnowledgeDocumentController controller = new KnowledgeDocumentController(
                new KnowledgeDocumentService(new FakeDocumentMapper(), new NoopParseQueue()),
                new RecordingIngestionService(),
                new FakeChunkMapper()
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                "a".repeat(200_001).getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> controller.uploadDocument(file, null, 1L, user()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too large");
    }

    @Test
    void createTextDocumentReturnsFailedDocumentWhenSynchronousIngestionFails() {
        FakeDocumentMapper documentMapper = new FakeDocumentMapper();
        KnowledgeDocumentController controller = new KnowledgeDocumentController(
                new KnowledgeDocumentService(documentMapper, new NoopParseQueue()),
                new FailingIngestionService(documentMapper),
                new FakeChunkMapper()
        );

        ApiResponse<DocumentResponse> response = controller.createTextDocument(
                new CreateTextDocumentRequest("失败文档", 1L, "内容"),
                user()
        );

        assertThat(response.success()).isTrue();
        assertThat(response.data().id()).isEqualTo(10L);
        assertThat(response.data().parseStatus()).isEqualTo("PARSE_FAILED");
    }

    @Test
    void createTextDocumentPropagatesIngestionFailureWhenDocumentStateDoesNotChange() {
        FakeDocumentMapper documentMapper = new FakeDocumentMapper();
        KnowledgeDocumentController controller = new KnowledgeDocumentController(
                new KnowledgeDocumentService(documentMapper, new NoopParseQueue()),
                new FailingBeforeStatusUpdateIngestionService(),
                new FakeChunkMapper()
        );

        assertThatThrownBy(() -> controller.createTextDocument(
                new CreateTextDocumentRequest("失败文档", 1L, "内容"),
                user()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ingestion failed before status update");
    }

    @Test
    void getDocumentThrowsNotFoundForMissingDocument() {
        KnowledgeDocumentController controller = new KnowledgeDocumentController(
                new KnowledgeDocumentService(new FakeDocumentMapper(), new NoopParseQueue()),
                new FailingIngestionService(null),
                new FakeChunkMapper()
        );

        assertThatThrownBy(() -> controller.getDocument(999L))
                .isInstanceOf(KnowledgeDocumentNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void chunksThrowsNotFoundForMissingDocument() {
        KnowledgeDocumentController controller = new KnowledgeDocumentController(
                new KnowledgeDocumentService(new FakeDocumentMapper(), new NoopParseQueue()),
                new FailingIngestionService(null),
                new FakeChunkMapper()
        );

        assertThatThrownBy(() -> controller.chunks(999L))
                .isInstanceOf(KnowledgeDocumentNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void writeEndpointsKeepExpectedPreAuthorizeAnnotations() throws Exception {
        Method create = KnowledgeDocumentController.class.getMethod(
                "createTextDocument",
                CreateTextDocumentRequest.class,
                AuthenticatedUser.class
        );
        Method upload = KnowledgeDocumentController.class.getMethod(
                "uploadDocument",
                org.springframework.web.multipart.MultipartFile.class,
                String.class,
                Long.class,
                AuthenticatedUser.class
        );
        Method enable = KnowledgeDocumentController.class.getMethod("enable", Long.class);
        Method chunks = KnowledgeDocumentController.class.getMethod("chunks", Long.class);

        assertThat(create.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('knowledge:document:upload')");
        assertThat(upload.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('knowledge:document:upload')");
        assertThat(enable.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('knowledge:document:manage')");
        assertThat(chunks.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('knowledge:document:view')");
    }

    private static AuthenticatedUser user() {
        return new AuthenticatedUser(1L, "admin", "Admin", 0, List.of("ADMIN"), List.of("knowledge:document:upload"));
    }

    private static KnowledgeDocument document(Long id, KnowledgeParseStatus status) {
        return new KnowledgeDocument(
                id,
                "测试文档",
                1L,
                "测试文档",
                null,
                "TEXT",
                6L,
                true,
                status,
                status == KnowledgeParseStatus.PARSE_FAILED ? "failed" : null,
                0,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now(),
                false
        );
    }

    private static final class NoopParseQueue implements KnowledgeParseQueue {
        @Override
        public void enqueueParseAndEmbed(Long documentId, int retryCount) {
        }
    }

    private static final class RecordingIngestionService extends KnowledgeIngestionService {
        private Long documentId;
        private String title;
        private Long categoryId;
        private String text;

        private RecordingIngestionService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public void ingestText(Long documentId, String title, Long categoryId, String text) {
            this.documentId = documentId;
            this.title = title;
            this.categoryId = categoryId;
            this.text = text;
        }
    }

    private static final class FailingIngestionService extends KnowledgeIngestionService {
        private final FakeDocumentMapper documentMapper;

        private FailingIngestionService(FakeDocumentMapper documentMapper) {
            super(null, null, null, null, null, null);
            this.documentMapper = documentMapper;
        }

        @Override
        public void ingestText(Long documentId, String title, Long categoryId, String text) {
            if (documentMapper != null) {
                documentMapper.status = KnowledgeParseStatus.PARSE_FAILED;
            }
            throw new IllegalStateException("ingestion failed");
        }
    }

    private static final class FailingBeforeStatusUpdateIngestionService extends KnowledgeIngestionService {
        private FailingBeforeStatusUpdateIngestionService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public void ingestText(Long documentId, String title, Long categoryId, String text) {
            throw new IllegalStateException("ingestion failed before status update");
        }
    }

    private static final class FakeDocumentMapper implements KnowledgeDocumentMapper {
        private long nextId = 10L;
        private KnowledgeParseStatus status = KnowledgeParseStatus.PENDING_PARSE;
        private final List<Long> insertedIds = new ArrayList<>();
        private String fileName;
        private String fileType;

        @Override
        public Long nextDocumentId() {
            return nextId;
        }

        @Override
        public int insertTextDocument(Long id, String title, Long categoryId, long fileSize, Long uploadedBy) {
            return insertDocument(id, title, categoryId, title, "TEXT", fileSize, uploadedBy);
        }

        @Override
        public int insertDocument(Long id,
                                  String title,
                                  Long categoryId,
                                  String fileName,
                                  String fileType,
                                  long fileSize,
                                  Long uploadedBy) {
            insertedIds.add(id);
            this.fileName = fileName;
            this.fileType = fileType;
            return 1;
        }

        @Override
        public KnowledgeDocument findById(Long id) {
            if (!insertedIds.contains(id)) {
                return null;
            }
            return document(id, status);
        }

        @Override
        public List<KnowledgeDocument> findRecent(int limit) {
            return List.of();
        }

        @Override
        public int updateEnabled(Long id, int enabled) {
            return insertedIds.contains(id) ? 1 : 0;
        }

        @Override
        public int updateParseStatus(Long id, KnowledgeParseStatus parseStatus, String parseError) {
            status = parseStatus;
            return insertedIds.contains(id) ? 1 : 0;
        }

        @Override
        public int markParseFailed(Long id, String parseError, int maxRetryCount) {
            status = KnowledgeParseStatus.PARSE_FAILED;
            return insertedIds.contains(id) ? 1 : 0;
        }

        @Override
        public int markParseFailedTerminal(Long id, String parseError) {
            status = KnowledgeParseStatus.PARSE_FAILED;
            return insertedIds.contains(id) ? 1 : 0;
        }

        @Override
        public int resetForRetry(Long id) {
            return insertedIds.contains(id) ? 1 : 0;
        }
    }

    private static final class FakeChunkMapper implements KnowledgeChunkMapper {
        @Override
        public int deleteByDocumentId(Long documentId) {
            return 0;
        }

        @Override
        public int insertBatchNonEmpty(List<KnowledgeChunkDraft> chunks) {
            return chunks.size();
        }

        @Override
        public List<KnowledgeChunk> findByDocumentId(Long documentId) {
            return List.of();
        }

        @Override
        public List<KnowledgeSearchResult> search(String queryVectorLiteral, Long categoryId, double minSimilarity, int limit) {
            return List.of();
        }
    }
}
