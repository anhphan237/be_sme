package com.sme.be_sme.modules.content.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Downloads document bytes from a URL for text extraction.
 */
public final class DocumentDownloader {

    private static final Logger log = LoggerFactory.getLogger(DocumentDownloader.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private DocumentDownloader() {
    }

    /**
     * Download resource from fileUrl. Returns null on any error or timeout.
     */
    public static byte[] download(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return null;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fileUrl.trim()))
                    .timeout(READ_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            log.warn("Document download failed: status {}", response.statusCode());
            return null;
        } catch (IOException | InterruptedException e) {
            log.warn("Document download failed: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        } catch (Exception e) {
            log.warn("Document download failed: {}", e.getMessage());
            return null;
        }
    }
}
