package com.spring_ai.rag_search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikiIngestorTest {

    @Mock
    VectorStore vectorStore;

    private final ChunkExistenceCheck none = id -> false;
    private final ChunkExistenceCheck all = id -> true;

    @Test
    void producesChunksWithDeterministicIds() throws Exception {
        WikiIngestor ingestor = new WikiIngestor(vectorStore, new PathMatchingResourcePatternResolver(), none);
        ingestor.ingest();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, atLeastOnce()).add(captor.capture());

        List<Document> allDocs = captor.getAllValues().stream().flatMap(List::stream).toList();
        assertThat(allDocs).isNotEmpty();

        List<String> ids = allDocs.stream().map(Document::getId).toList();
        assertThat(ids).doesNotHaveDuplicates();
        assertThat(ids.get(0)).matches("[a-f0-9]+-\\d+");
    }

    @Test
    void sameInputProducesSameIds() throws Exception {
        WikiIngestor first = new WikiIngestor(vectorStore, new PathMatchingResourcePatternResolver(), none);
        first.ingest();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> cap1 = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, atLeastOnce()).add(cap1.capture());
        List<String> firstIds = cap1.getAllValues().stream().flatMap(List::stream).map(Document::getId).toList();

        reset(vectorStore);
        WikiIngestor second = new WikiIngestor(vectorStore, new PathMatchingResourcePatternResolver(), none);
        second.ingest();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> cap2 = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, atLeastOnce()).add(cap2.capture());
        List<String> secondIds = cap2.getAllValues().stream().flatMap(List::stream).map(Document::getId).toList();

        assertThat(firstIds).isEqualTo(secondIds);
    }

    @Test
    void skipsFilesWhoseFirstChunkAlreadyExists() throws Exception {
        WikiIngestor ingestor = new WikiIngestor(vectorStore, new PathMatchingResourcePatternResolver(), all);
        ingestor.ingest();

        verify(vectorStore, never()).add(any());
    }
}
