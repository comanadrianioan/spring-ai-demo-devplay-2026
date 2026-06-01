package com.spring_ai.rag_search;

import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaApi.GetEmbeddingResponse;
import org.springframework.ai.chroma.vectorstore.ChromaApi.GetEmbeddingsRequest;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChromaChunkExistenceCheck implements ChunkExistenceCheck {

    private final ChromaApi chromaApi;
    private final ChromaVectorStoreProperties props;
    private volatile String collectionId;

    public ChromaChunkExistenceCheck(ChromaApi chromaApi, ChromaVectorStoreProperties props) {
        this.chromaApi = chromaApi;
        this.props = props;
    }

    @Override
    public boolean exists(String chunkId) {
        GetEmbeddingResponse response = chromaApi.getEmbeddings(
            props.getTenantName(),
            props.getDatabaseName(),
            resolveCollectionId(),
            new GetEmbeddingsRequest(List.of(chunkId))
        );
        return response.ids() != null && !response.ids().isEmpty();
    }

    private String resolveCollectionId() {
        String cached = collectionId;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (collectionId == null) {
                collectionId = chromaApi.getCollection(
                    props.getTenantName(),
                    props.getDatabaseName(),
                    props.getCollectionName()
                ).id();
            }
            return collectionId;
        }
    }
}
