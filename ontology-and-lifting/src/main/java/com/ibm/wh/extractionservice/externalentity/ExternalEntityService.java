package com.ibm.wh.extractionservice.externalentity;

import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.value.CandidateValue;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity.Type;
import com.ibm.wh.extractionservice.commons.externalentity.type.GroupExternalEntity;
import com.ibm.wh.extractionservice.commons.externalentity.type.IndividualExternalEntity;
import com.ibm.wh.extractionservice.externalentity.groupAddition.GroupAdditionConfig;
import com.ibm.wh.extractionservice.externalentity.groupAddition.GroupAdditionAll;
import com.ibm.wh.extractionservice.externalentity.groupAddition.GroupAdditionItem;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExternalEntityService {

    public static final String[] GROUP_CSV_HEADERS = {"description", "displayName", "entityId", "members", "namespace", "tag", "typesId"};
    private final ExternalEntityRepository externalEntityRepository;

    @Autowired
    public ExternalEntityService(ExternalEntityRepository externalEntityRepository) {
        this.externalEntityRepository = externalEntityRepository;
    }

    public Set<? extends ExternalEntity> findAllEntities() {
        return externalEntityRepository.findAll();
    }

    public Set<? extends ExternalEntity> findAllEntitiesOfType(Type type) {
        return externalEntityRepository.findByType(type);
    }

    public Set<GroupExternalEntity> findAllGroupEntities() {
        return findAllEntitiesOfType(Type.GROUP).stream()
                .map(this::toGroupEntity)
                .collect(Collectors.toSet());
    }

    private GroupExternalEntity toGroupEntity(ExternalEntity externalEntity) {
        if (!externalEntity.getType().equals(ExternalEntity.Type.GROUP))
            throw new IllegalArgumentException(String.format("Entity of type [%s] cannot be cast to GroupEntity!", externalEntity.getType()));
        return (GroupExternalEntity) externalEntity;
    }

    public ExternalEntity findEntityByEntityId(String entityId) {
        return externalEntityRepository.findByEntityId(entityId);
    }

    private IndividualExternalEntity getEntity(String namespace, String beginCode) {
        String resourceId = namespace.toLowerCase() + "procedure_code_" + beginCode.trim().toLowerCase();
        return (IndividualExternalEntity) this.externalEntityRepository.findByEntityId(resourceId);
    }

    public GroupAdditionAll processGroupAddition(GroupAdditionConfig groupJson) {
        if (groupJson.getNamespace() != null) {
            List<IndividualExternalEntity> individualExternalEntities = new ArrayList<>();
            groupJson.getProposedMembers().forEach(item -> {
                if (item.getGroupAdditionItemType() == GroupAdditionItem.GroupAdditionItemType.SINGLE_CODE) {
                    IndividualExternalEntity entity = getEntity(groupJson.getNamespace(), item.getGroupAdditionBeginCode());
                    if (entity != null) {
                        individualExternalEntities.add(entity);
                    } else {
                        throw new IllegalArgumentException(String.format("No IndividualExternalEntity with code %s can be found", item.getGroupAdditionBeginCode()));
                    }

                } else if (item.getGroupAdditionItemType() == GroupAdditionItem.GroupAdditionItemType.RANGE) {
                    Set<IndividualExternalEntity> rangeEntities = rangeEntities(item.getGroupAdditionBeginCode(), item.getGroupAdditionEndCode());
                    individualExternalEntities.addAll(rangeEntities);
                }

            });

            Set<CandidateValue> members = new HashSet<>();
            Set<String> surfaceForms = new HashSet<>();
            individualExternalEntities.forEach(individualExternalEntity -> {
                members.add(cvFromGroupMember(individualExternalEntity.getEntityId()));
                surfaceForms.addAll(individualExternalEntity.getSurfaceForms());
            });

            String cleanedGroupName = groupJson.getEntityId();
            if (cleanedGroupName.isEmpty()) {
                cleanedGroupName = groupJson.getNamespace() + (groupJson.getDisplayName().trim()
                        .toLowerCase()
                        .replaceAll(" ", "_")
                        .replaceAll("[^a-zA-Z\\d\\s:]", "_"));
            }

            GroupExternalEntity groupExternalEntity = new GroupExternalEntity(cleanedGroupName, groupJson.getTypesId(),
                    groupJson.getDisplayName(), surfaceForms, groupJson.getDescription(), groupJson.getTag(),
                    null, members);

            return new GroupAdditionAll(groupExternalEntity, individualExternalEntities);
        } else {
            throw new IllegalArgumentException("Namespace is required");
        }
    }

    private Set<ExternalEntity> getRange(String startCode, String endCode) {
        // valid range:
        // 1. code starts OR ends with a letter - ie Dental procedure codes
        // 2. startCode smaller than endCode

        String codeLetterPrefix = "";
        String codeLetterPostfix = "";

        int numericStartCode;
        int numericEndCode;
        if (Character.isAlphabetic(startCode.charAt(0))) {
            if (startCode.substring(0, 1).equals(endCode.substring(0, 1))) {
                codeLetterPrefix = startCode.substring(0, 1);
                numericStartCode = Integer.parseInt(startCode.substring(1));
                numericEndCode = Integer.parseInt(endCode.substring(1));
            } else {
                throw new IllegalArgumentException(String.format("Invalid range.  Entity range start and end codes do not begin with same letter [%s], [%s]", startCode, endCode));
            }
        } else if (Character.isAlphabetic(startCode.charAt(startCode.length() - 1))) {
            if (startCode.substring(0, startCode.length() - 1).equals(endCode.substring(0, endCode.length() - 1))) {
                codeLetterPostfix = startCode.substring(startCode.length() - 1);
                numericStartCode = Integer.parseInt(startCode.substring(0, startCode.length() - 1));
                numericEndCode = Integer.parseInt(endCode.substring(0, endCode.length() - 1));
            } else {
                throw new IllegalArgumentException(String.format("Invalid range.  Entity range start and end codes do not end with same letter [%s], [%s]", startCode, endCode));
            }

        } else {
            numericStartCode = Integer.parseInt(startCode);
            numericEndCode = Integer.parseInt(endCode);
        }

        if (numericStartCode >= numericEndCode) {
            throw new IllegalArgumentException(String.format("Invalid range. Entity range start number is not larger than end number [%s], [%s]", startCode, endCode));
        }

        String finalCodeLetterPrefix = codeLetterPrefix;
        String finalCodeLetterPostfix = codeLetterPostfix;
        int finalStartCode = numericStartCode;
        int finalEndCode = numericEndCode;

        return this.externalEntityRepository.findByType(Type.INDIVIDUAL)
                .stream()
                .filter(entity -> isInRange(entity, finalCodeLetterPrefix, finalCodeLetterPostfix, finalStartCode, finalEndCode))
                .collect(Collectors.toSet());

    }

    private boolean isInRange(ExternalEntity entity, String alphaIdentifierPrefix, String alphaIdentifierPostfix, int startCode, int endCode) {
        boolean inRange = false;
        String claimValue = entity.getClaimValue();
        if (claimValue != null) {
            String numericClaimValue = "";
            if (!alphaIdentifierPrefix.isEmpty()) {
                numericClaimValue = claimValue.substring(1);
            } else if (!alphaIdentifierPostfix.isEmpty()) {
                numericClaimValue = claimValue.substring(0, claimValue.length() - 1);
            }
            if (StringUtils.isNumeric(numericClaimValue)) {
                int numericalCode = Integer.parseInt(numericClaimValue);
                if ((claimValue.substring(0, 1).equals(alphaIdentifierPrefix) || claimValue.substring(claimValue.length() - 1).equals(alphaIdentifierPostfix))
                        && numericalCode >= startCode
                        && numericalCode <= endCode) {
                    inRange = true;
                }
            }
        }
        return inRange;
    }

    private CandidateValue cvFromGroupMember(String entityId) {
        return new CandidateValue(entityId, null, null, CandidateValue.Type.SINGLE);
    }

    private Set<IndividualExternalEntity> rangeEntities(String beginCode, String endCode) {
        return getRange(beginCode.trim().toUpperCase(),
                endCode.trim().toUpperCase()).stream()
                .filter(entity -> entity.getType().equals(Type.INDIVIDUAL))
                .map(entity -> (IndividualExternalEntity) entity)
                .collect(Collectors.toSet());
    }

    private GroupExternalEntity groupFromCSVLine(String[] line) {
        //{"description","displayName","entityId","members","namespace","tag","typesId"};
        String description = line[0];
        String displayName = line[1];
        String entityId = line[2];
        String[] entries = line[3].replace("[","").replace("]", ",").split(",");
        String namespace = line[4];
        String tag = line[5];
        String typesId = line[6];

        List<IndividualExternalEntity> individualExternalEntities = new ArrayList<>();

        Arrays.stream(entries).forEach(item -> {
            if (!item.contains("-")) {
                IndividualExternalEntity entity = getEntity(namespace, item);
                if (entity != null) {
                    individualExternalEntities.add(entity);
                } else {
                    throw new IllegalArgumentException(String.format("No IndividualExternalEntity with code %s can be found", item));
                }

            } else {
                String[] range = item.split("-");
                individualExternalEntities.addAll(rangeEntities(range[0], range[1]));
            }
        });


        Set<CandidateValue> members = new HashSet<>();
        Set<String> surfaceForms = new HashSet<>();
        individualExternalEntities.forEach(individualExternalEntity -> {
            members.add(cvFromGroupMember(individualExternalEntity.getEntityId()));
            surfaceForms.addAll(individualExternalEntity.getSurfaceForms());
        });

        return new GroupExternalEntity(entityId, new HashSet<>(Collections.singletonList(typesId)),
                displayName, surfaceForms, description, tag,
                null, members);
    }


    public Set<GroupExternalEntity> processGroupAdditionCSV(InputStream inputStream) throws Exception {
        Set<GroupExternalEntity> groups = new HashSet<>();
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] line;
            if ((line = csvReader.readNext()) == null || !Arrays.equals(line, GROUP_CSV_HEADERS)) {
                throw new IllegalArgumentException("CSV file does not have expected headers");
            }
            while ((line = csvReader.readNext()) != null) {
                groups.add(groupFromCSVLine(line));
            }
            return groups;
        } catch (Exception e) {
            throw new Exception("Error parsing CSV: " + e.getLocalizedMessage());
        }
    }

}
