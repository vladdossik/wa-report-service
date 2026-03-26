package org.wa.report.service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.wa.report.service.dto.CombinedDashboardDto;
import org.wa.report.service.exception.StorageServiceException;
import reactor.core.publisher.Mono;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StorageServiceClient {

    private final WebClient storageServiceWebClient;

    public Mono<CombinedDashboardDto> getData(UUID userId, OffsetDateTime from, OffsetDateTime to, String bucket) {
        return storageServiceWebClient.get()
                .uri(uriBuilder -> uriBuilder
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
