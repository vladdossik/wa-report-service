package org.wa.report.service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.wa.report.service.dto.CombinedDashboardDto;
import org.wa.report.service.enumeration.Bucket;
import org.wa.report.service.exception.StorageServiceException;
import reactor.core.publisher.Mono;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StorageServiceClient {

    private final WebClient storageServiceWebClient;

    @Value("${integration.storage-service.path}")
    private String storagePath;

    public Mono<CombinedDashboardDto> getData(UUID userId, OffsetDateTime from, OffsetDateTime to, Bucket bucket) {
        return storageServiceWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(storagePath)
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .queryParam("bucket", bucket)
                        .build(userId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class).flatMap(error ->
                                Mono.error(new StorageServiceException(
                                        "Не удалось получить данные. Статус: " +
                                        clientResponse.statusCode())
                                )
                        ))
                .bodyToMono(CombinedDashboardDto.class);
    }
}
