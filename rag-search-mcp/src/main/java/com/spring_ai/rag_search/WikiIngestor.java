package com.spring_ai.rag_search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Component
public class WikiIngestor implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WikiIngestor.class);

    private final VectorStore vectorStore;
    private final ResourcePatternResolver resourceResolver;
    private final ChunkExistenceCheck existenceCheck;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public WikiIngestor(VectorStore vectorStore,
                        ResourcePatternResolver resourceResolver,
                        ChunkExistenceCheck existenceCheck) {
        this.vectorStore = vectorStore;
        this.resourceResolver = resourceResolver;
        this.existenceCheck = existenceCheck;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ingest();
    }

    void ingest() throws IOException {
        Resource[] resources = resourceResolver.getResources("classpath:wiki/**/*");
        for (Resource resource : resources) {
            String fileHash = hashBytes(resource.getContentAsByteArray());
            if (existenceCheck.exists(fileHash + "-0")) {
                log.info("Skipping already-ingested wiki file: {}", resource.getFilename());
                continue;
            }
            List<Document> raw = new TikaDocumentReader(resource).get();
            List<Document> chunks = splitter.apply(raw);
            List<Document> stamped = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                Document stampedDoc = new Document(
                    fileHash + "-" + i,
                    chunk.getText(),
                    chunk.getMetadata()
                );
                stamped.add(stampedDoc);
            }
            log.info("Ingesting {} chunks for {}", stamped.size(), resource.getFilename());
            vectorStore.add(stamped);
        }
    }

    private String hashBytes(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
