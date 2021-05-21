package com.ibm.wh.extractionservice.externalentity;

import java.util.Set;
import org.springframework.data.repository.Repository;

import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity.Type;

public interface  ExternalEntityRepository extends Repository<ExternalEntity, String> {

    Set<ExternalEntity> findAll();

    Set<ExternalEntity> findByType(Type type);

    ExternalEntity findByEntityId(String entityId);
}
