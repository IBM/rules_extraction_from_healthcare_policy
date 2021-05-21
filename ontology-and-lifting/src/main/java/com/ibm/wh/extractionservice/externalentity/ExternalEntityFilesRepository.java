package com.ibm.wh.extractionservice.externalentity;

import static java.lang.String.*;
import static java.util.stream.Collectors.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.util.ResourceUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity.Type;

@Primary // fixme: replace with conditional or db initialiser
@Repository
public class ExternalEntityFilesRepository implements ExternalEntityRepository {

    private final Map<String, ExternalEntity> entityIdToEntity;
    private final ObjectMapper objectMapper;

    @Autowired
    public ExternalEntityFilesRepository(
            @Value("${external-data.input.files}") Collection<String> externalEntitiesFilePaths,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.entityIdToEntity = initializeEntitiesMap(externalEntitiesFilePaths);
    }

    private Map<String, ExternalEntity> initializeEntitiesMap(Collection<String> externalEntitiesFilePaths) {
        return externalEntitiesFilePaths.stream()
                .map(path -> getEntitiesFromFile(getFile(path)))
                .flatMap(Collection::stream)
                .collect(groupingBy(ExternalEntity::getEntityId, reducing(this::mergeOrThrow)))
                .entrySet().stream()
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
    }

    private ExternalEntity mergeOrThrow(ExternalEntity one, ExternalEntity other) {
        if (!one.canBeMerged(other)) {
            throw new IllegalStateException(format("Information regarding entity [%s] are incompatible and cannot be loaded!", one.getEntityId()));
        }
        return one.merge(other);
    }

    private Collection<ExternalEntity> getEntitiesFromFile(File file) {
        try {
            return objectMapper.readValue(file, new TypeReference<Set<ExternalEntity>>() {});
        } catch (IllegalArgumentException | IOException e) {
            throw new IllegalStateException(format("Something bad happened while reading groups candidate values from file [%s]", file.toString()), e);
        }
    }

    private File getFile(String filePathInResources) {
        try {
            return ResourceUtils.getFile(Objects.requireNonNull(this.getClass().getClassLoader().getResource(filePathInResources)));
        } catch (FileNotFoundException | IllegalArgumentException e) {
            throw new IllegalArgumentException(format("Something bad happened when reading groups from file: %s", filePathInResources), e);
        }
    }

    @Override
    public Set<ExternalEntity> findAll() {
        return new HashSet<>(entityIdToEntity.values());
    }

    @Override
    public Set<ExternalEntity> findByType(Type type) {
        return entityIdToEntity.values().stream()
                .filter(entity -> entity.getType().equals(type))
                .collect(Collectors.toSet());
    }

    @Override
    public ExternalEntity findByEntityId(String entityId) {
        return Optional.ofNullable(entityIdToEntity.getOrDefault(entityId, null))
                .orElseThrow(() -> new IllegalArgumentException(format("Entity [%s] not found!", entityId)));
    }

}
