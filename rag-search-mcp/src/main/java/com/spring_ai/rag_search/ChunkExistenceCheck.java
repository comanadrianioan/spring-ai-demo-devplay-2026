package com.spring_ai.rag_search;

@FunctionalInterface
public interface ChunkExistenceCheck {
    boolean exists(String chunkId);
}
