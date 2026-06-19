package com.example.aiticket.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderPropertiesTest {
    @Test
    void bindsIndependentChatAndEmbeddingProviders() throws Exception {
        String yaml = """
                ai:
                  chat:
                    provider: deepseek
                    base-url: https://api.deepseek.com
                    api-key: chat-key
                    model: deepseek-chat
                  embedding:
                    provider: siliconflow
                    base-url: https://api.siliconflow.cn/v1
                    api-key: embedding-key
                    model: Qwen/Qwen3-Embedding-8B
                    dimensions: 1024
                """;

        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources sources = environment.getPropertySources();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        sources.addFirst(loader.load("test", new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8))).getFirst());

        AiProviderProperties properties = Binder.get(environment)
                .bind("ai", Bindable.of(AiProviderProperties.class))
                .orElseThrow(() -> new IllegalStateException("ai properties did not bind"));

        assertThat(properties.getChat().getProvider()).isEqualTo("deepseek");
        assertThat(properties.getChat().getModel()).isEqualTo("deepseek-chat");
        assertThat(properties.getEmbedding().getProvider()).isEqualTo("siliconflow");
        assertThat(properties.getEmbedding().getBaseUrl()).isEqualTo("https://api.siliconflow.cn/v1");
        assertThat(properties.getEmbedding().getModel()).isEqualTo("Qwen/Qwen3-Embedding-8B");
        assertThat(properties.getEmbedding().getDimensions()).isEqualTo(1024);
    }
}
