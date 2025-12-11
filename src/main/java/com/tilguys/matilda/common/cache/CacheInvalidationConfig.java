package com.tilguys.matilda.common.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 캐시 무효화 Pub/Sub 구독 설정.
 */
@Configuration
@RequiredArgsConstructor
public class CacheInvalidationConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final CacheInvalidationSubscriber cacheInvalidationSubscriber;

    @Value("${matilda.cache.invalidation.topic:cache:invalidate}")
    private String topic;

    @Bean
    public RedisMessageListenerContainer cacheInvalidationListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(cacheInvalidationSubscriber, new ChannelTopic(topic));
        return container;
    }
}
