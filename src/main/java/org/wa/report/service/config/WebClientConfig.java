package org.wa.report.service.config;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import org.wa.report.service.exception.ConnectException;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    @Value("${storage-service.base-url}")
    private String storageBaseUrl;

    @Value("${storage-service.timeout}")
    private int timeout;

    @Bean
    public WebClient webClient() {
        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeout))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);

        return WebClient.builder()
                .baseUrl(storageBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .filter(ExchangeFilterFunctions.statusError(
                        HttpStatusCode::isError,
                        response -> new ConnectException(
                                "Ошибка соединения с сервисом: " + response.statusCode())
                ))
                .build();
    }
}
