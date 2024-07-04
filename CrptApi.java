package by.it.selsuptesttask;

import org.apache.commons.lang3.concurrent.TimedSemaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.concurrent.*;

public class CrptApi {

    private final TimedSemaphore timedSemaphore;
    private final RestClient restClient = RestClient.create();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    @Value("${crpt-service.base-url:https://ismp.crpt.ru/api/v3/lk/documents/create}")
    private String baseUrl;

    public CrptApi(long timePeriod, TimeUnit timeUnit, int requestLimit) {
        this.timedSemaphore = new TimedSemaphore(timePeriod, timeUnit, requestLimit);
    }

    public Document create(Document doc) {
        try {
            timedSemaphore.acquire();
            Future<Document> result = executor.submit(new Task(doc));
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SelsupTestTaskException("Something went wrong", e);
        }
    }

    public void close() {
        executor.shutdown();
    }

    private class Task implements Callable<Document> {
        private final Document doc;

        Task(Document doc) {
            this.doc = doc;
        }

        @Override
        public Document call() {
            return restClient
                    .post()
                    .uri(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(doc)
                    .retrieve()
                    .body(Document.class);
        }
    }

    private static class SelsupTestTaskException extends RuntimeException {
        public SelsupTestTaskException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
