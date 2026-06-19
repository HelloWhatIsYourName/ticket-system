package com.example.aiticket.ai.embedding;

import com.example.aiticket.config.AiProviderProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SiliconFlowEmbeddingClientTest {
    @Test
    void sendsSiliconFlowEmbeddingRequestAndSortsResultsByIndex() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SiliconFlowEmbeddingClient client = new SiliconFlowEmbeddingClient(properties(), builder);

        server.expect(once(), requestTo("https://api.siliconflow.cn/v1/embeddings"))
                .andExpect(method(POST))
                .andExpect(header(AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("Qwen/Qwen3-Embedding-8B"))
                .andExpect(jsonPath("$.input[0]").value("first text"))
                .andExpect(jsonPath("$.input[1]").value("second text"))
                .andExpect(jsonPath("$.dimensions").value(3))
                .andExpect(jsonPath("$.encoding_format").value("float"))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {"index": 1, "embedding": [0.4, 0.5, 0.6]},
                            {"index": 0, "embedding": [0.1, 0.2, 0.3]}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<EmbeddingResult> results = client.embedBatch(List.of("first text", "second text"));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).model()).isEqualTo("Qwen/Qwen3-Embedding-8B");
        assertThat(results.get(0).dimensions()).isEqualTo(3);
        assertThat(results.get(0).vector()).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(results.get(1).vector()).containsExactly(0.4f, 0.5f, 0.6f);
        server.verify();
    }

    @Test
    void rejectsBlankTextsBeforeCallingProvider() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SiliconFlowEmbeddingClient client = new SiliconFlowEmbeddingClient(properties(), builder);

        assertThatThrownBy(() -> client.embedBatch(List.of("valid", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("texts must not contain blank values");

        server.verify();
    }

    @Test
    void rejectsMismatchedResponseSize() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SiliconFlowEmbeddingClient client = new SiliconFlowEmbeddingClient(properties(), builder);

        server.expect(once(), requestTo("https://api.siliconflow.cn/v1/embeddings"))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {"index": 0, "embedding": [0.1, 0.2, 0.3]}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embedBatch(List.of("first", "second")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("embedding response size does not match request size");

        server.verify();
    }

    private AiProviderProperties properties() {
        AiProviderProperties properties = new AiProviderProperties();
        AiProviderProperties.EmbeddingProvider embedding = new AiProviderProperties.EmbeddingProvider();
        embedding.setProvider("siliconflow");
        embedding.setBaseUrl("https://api.siliconflow.cn/v1");
        embedding.setApiKey("test-key");
        embedding.setModel("Qwen/Qwen3-Embedding-8B");
        embedding.setDimensions(3);
        properties.setEmbedding(embedding);
        return properties;
    }
}
