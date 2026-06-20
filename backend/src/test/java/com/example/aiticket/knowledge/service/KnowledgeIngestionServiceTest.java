package com.example.aiticket.knowledge.service;

import com.example.aiticket.ai.embedding.EmbeddingClient;
import com.example.aiticket.ai.embedding.EmbeddingResult;
import com.example.aiticket.config.AiProviderProperties;
import com.example.aiticket.config.KnowledgeProperties;
import com.example.aiticket.knowledge.chunk.ParagraphTextChunker;
import com.example.aiticket.knowledge.domain.KnowledgeChunkDraft;
import com.example.aiticket.knowledge.domain.KnowledgeDocument;
import com.example.aiticket.knowledge.domain.KnowledgeParseStatus;
import com.example.aiticket.knowledge.mapper.KnowledgeChunkMapper;
import com.example.aiticket.knowledge.mapper.KnowledgeDocumentMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeIngestionServiceTest {
    @Test
    void chunksEmbedsAndMarksDocumentSuccess() {
        FakeDocumentMapper documentMapper = new FakeDocumentMapper();
        FakeChunkMapper chunkMapper = new FakeChunkMapper();
        FakeEmbeddingClient embeddingClient = FakeEmbeddingClient.normal();
        KnowledgeIngestionService service = new KnowledgeIngestionService(
                documentMapper,
                chunkMapper,
                new ParagraphTextChunker(20, 5),
                embeddingClient,
                knowledgeProperties(),
                aiProperties()
        );

        service.ingestText(10L, "测试文档", 1L, "012345678901234567890123456789");

        assertThat(documentMapper.statuses).containsExactly("PARSING", "PARSE_SUCCESS");
        assertThat(chunkMapper.deletedDocumentIds).containsExactly(10L);
        assertThat(chunkMapper.inserted).hasSize(2);
        assertThat(chunkMapper.inserted.getFirst().documentId()).isEqualTo(10L);
        assertThat(chunkMapper.inserted.getFirst().sourceTitle()).isEqualTo("测试文档");
        assertThat(chunkMapper.inserted.getFirst().vectorLiteral()).startsWith("[");
        assertThat(chunkMapper.insertBatchCalls).isEqualTo(1);
        assertThat(embeddingClient.batchCalls).isEqualTo(1);
    }

    @Test
    void doesNotCallEmbeddingProviderWhenTextProducesNoChunks() {
        FakeDocumentMapper documentMapper = new FakeDocumentMapper();
        FakeChunkMapper chunkMapper = new FakeChunkMapper();
        FakeEmbeddingClient embeddingClient = FakeEmbeddingClient.normal();
        KnowledgeIngestionService service = new KnowledgeIngestionService(
                documentMapper,
                chunkMapper,
                new ParagraphTextChunker(20, 5),
                embeddingClient,
                knowledgeProperties(),
                aiProperties()
        );

        service.ingestText(10L, "空白文档", 1L, " \n\n\t ");

        assertThat(documentMapper.statuses).containsExactly("PARSING", "PARSE_SUCCESS");
        assertThat(chunkMapper.deletedDocumentIds).containsExactly(10L);
        assertThat(chunkMapper.insertBatchCalls).isEqualTo(1);
        assertThat(chunkMapper.inserted).isEmpty();
        assertThat(embeddingClient.batchCalls).isZero();
    }

    @Test
    void marksTerminalFailureWhenEmbeddingCountDoesNotMatchChunks() {
        FakeDocumentMapper documentMapper = new FakeDocumentMapper();
        KnowledgeIngestionService service = new KnowledgeIngestionService(
                documentMapper,
                new FakeChunkMapper(),
                new ParagraphTextChunker(20, 5),
                FakeEmbeddingClient.countMismatch(),
                knowledgeProperties(),
                aiProperties()
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.ingestText(10L, "测试文档", 1L, "012345678901234567890123456789"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("embedding result count");

        assertThat(documentMapper.statuses).containsExactly("PARSING", "TERMINAL:embedding result count does not match chunk count");
    }

    @Test
    void marksTerminalFailureWhenEmbeddingDimensionsMismatch() {
        FakeDocumentMapper documentMapper = new FakeDocumentMapper();
        KnowledgeIngestionService service = new KnowledgeIngestionService(
                documentMapper,
                new FakeChunkMapper(),
                new ParagraphTextChunker(20, 5),
                FakeEmbeddingClient.dimensionMismatch(),
                knowledgeProperties(),
                aiProperties()
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.ingestText(10L, "测试文档", 1L, "012345678901234567890123456789"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("embedding dimensions");

        assertThat(documentMapper.statuses).containsExactly("PARSING", "TERMINAL:embedding dimensions mismatch");
    }

    private static KnowledgeProperties knowledgeProperties() {
        return new KnowledgeProperties();
    }

    private static AiProviderProperties aiProperties() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.getEmbedding().setDimensions(1024);
        return properties;
    }

    private static final class FakeEmbeddingClient implements EmbeddingClient {
        private enum Mode {
            NORMAL,
            COUNT_MISMATCH,
            DIMENSION_MISMATCH
        }

        private final Mode mode;
        private int batchCalls;

        private FakeEmbeddingClient(Mode mode) {
            this.mode = mode;
        }

        static FakeEmbeddingClient normal() {
            return new FakeEmbeddingClient(Mode.NORMAL);
        }

        static FakeEmbeddingClient countMismatch() {
            return new FakeEmbeddingClient(Mode.COUNT_MISMATCH);
        }

        static FakeEmbeddingClient dimensionMismatch() {
            return new FakeEmbeddingClient(Mode.DIMENSION_MISMATCH);
        }

        @Override
        public EmbeddingResult embed(String text) {
            return embedBatch(List.of(text)).getFirst();
        }

        @Override
        public List<EmbeddingResult> embedBatch(List<String> texts) {
            batchCalls++;
            if (mode == Mode.COUNT_MISMATCH) {
                return texts.stream()
                        .limit(Math.max(0, texts.size() - 1))
                        .map(text -> new EmbeddingResult("fake-model", 1024, vector(1024)))
                        .toList();
            }
            if (mode == Mode.DIMENSION_MISMATCH) {
                return texts.stream()
                        .map(text -> new EmbeddingResult("fake-model", 2, vector(2)))
                        .toList();
            }
            return texts.stream()
                    .map(text -> new EmbeddingResult("fake-model", 1024, vector(1024)))
                    .toList();
        }

        private static List<Float> vector(int dimensions) {
            List<Float> values = new ArrayList<>();
            for (int i = 0; i < dimensions; i++) {
                values.add(i == 0 ? 1.0f : 0.0f);
            }
            return values;
        }
    }

    private static final class FakeDocumentMapper implements KnowledgeDocumentMapper {
        private final List<String> statuses = new ArrayList<>();

        @Override
        public Long nextDocumentId() {
            return 10L;
        }

        @Override
        public int insertTextDocument(Long id, String title, Long categoryId, long fileSize, Long uploadedBy) {
            return 1;
        }

        @Override
        public int insertDocument(Long id,
                                  String title,
                                  Long categoryId,
                                  String fileName,
                                  String fileType,
                                  long fileSize,
                                  Long uploadedBy) {
            return 1;
        }

        @Override
        public KnowledgeDocument findById(Long id) {
            return new KnowledgeDocument(id, "测试文档", 1L, "测试文档", null, "TEXT", 30L, true,
                    KnowledgeParseStatus.PENDING_PARSE, null, 0, 1L, LocalDateTime.now(), LocalDateTime.now(), false);
        }

        @Override
        public List<KnowledgeDocument> findRecent(int limit) {
            return List.of();
        }

        @Override
        public int updateEnabled(Long id, int enabled) {
            return 1;
        }

        @Override
        public int updateParseStatus(Long id, KnowledgeParseStatus parseStatus, String parseError) {
            statuses.add(parseStatus.name());
            return 1;
        }

        @Override
        public int markParseFailed(Long id, String parseError, int maxRetryCount) {
            statuses.add("FAILED:" + parseError);
            return 1;
        }

        @Override
        public int markParseFailedTerminal(Long id, String parseError) {
            statuses.add("TERMINAL:" + parseError);
            return 1;
        }

        @Override
        public int resetForRetry(Long id) {
            return 1;
        }
    }

    private static final class FakeChunkMapper implements KnowledgeChunkMapper {
        private final List<Long> deletedDocumentIds = new ArrayList<>();
        private final List<KnowledgeChunkDraft> inserted = new ArrayList<>();
        private int insertBatchCalls;

        @Override
        public int deleteByDocumentId(Long documentId) {
            deletedDocumentIds.add(documentId);
            return 1;
        }

        @Override
        public int insertBatch(List<KnowledgeChunkDraft> chunks) {
            insertBatchCalls++;
            return KnowledgeChunkMapper.super.insertBatch(chunks);
        }

        @Override
        public int insertBatchNonEmpty(List<KnowledgeChunkDraft> chunks) {
            inserted.addAll(chunks);
            return chunks.size();
        }

        @Override
        public List<com.example.aiticket.knowledge.domain.KnowledgeChunk> findByDocumentId(Long documentId) {
            return List.of();
        }

        @Override
        public List<com.example.aiticket.knowledge.domain.KnowledgeSearchResult> search(String queryVectorLiteral, Long categoryId, double minSimilarity, int limit) {
            return List.of();
        }
    }
}
