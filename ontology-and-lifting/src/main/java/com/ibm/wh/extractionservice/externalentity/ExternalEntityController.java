package com.ibm.wh.extractionservice.externalentity;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import com.ibm.wh.extractionservice.commons.externalentity.type.GroupExternalEntity;
import com.ibm.wh.extractionservice.externalentity.groupAddition.GroupAdditionConfig;
import com.ibm.wh.extractionservice.externalentity.groupAddition.GroupAdditionAll;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity.Type;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingService;

@RestController
public class ExternalEntityController {

    public static final String ENDPOINT__GET_ALL_GROUP_EXTERNAL_ENTITIES = "/external-entities/group";
    public static final String ENDPOINT__POST_GROUP_EXTERNAL_ENTITIES = "/external-entities/process-group";
    public static final String ENDPOINT__POST_GROUP_EXTERNAL_ENTITIES_CSV = "/external-entities/process-group-csv";
    public static final String ENDPOINT__GET_ALL_EXTERNAL_ENTITIES = "/external-entities";
    public static final String ENDPOINT__LIFT_EXTERNAL_ENTITIES = "/external-entities/lift";

    private final ExternalEntityService externalEntityService;
    private final LiftingService liftingService;

    @Autowired
    public ExternalEntityController(ExternalEntityService externalEntityService, LiftingService liftingService) {
        this.externalEntityService = externalEntityService;
        this.liftingService = liftingService;
    }

    @GetMapping(path = ENDPOINT__GET_ALL_GROUP_EXTERNAL_ENTITIES)
    public Set<? extends ExternalEntity> findAllGroupExternalEntities() {
        return externalEntityService.findAllEntitiesOfType(Type.GROUP);
    }

    @ApiOperation(value = "generateGroupJson - Generate JSON for a group and its members.",
            notes="Input a group configuration to return a GroupExternalEntity representing the group and a " +
                    "List<IndividualExternalEntity> representing the members of that group. These can be saved manually " +
                    "to the JSON files for use by the ontology on application startup.",
            response = GroupAdditionAll.class)
    @PostMapping(path = ENDPOINT__POST_GROUP_EXTERNAL_ENTITIES, consumes = MediaType.APPLICATION_JSON_VALUE)
    public GroupAdditionAll generateSingleGroupJson(
            @ApiParam( name="groupAdditionConfig", value = "The group configuration, including the proposed member ranges ", required = true)
            @RequestBody GroupAdditionConfig groupAdditionConfig)  {
        return externalEntityService.processGroupAddition(groupAdditionConfig);
    }

    @ApiOperation(value = "generateGroupsJsonFromCsv - Use CSV input to generate JSON for a custom groups.",
            notes="Input a CSV to return a Set<GroupExternalEntity> representing the groups, including member list of each group. These can be saved manually " +
                    "to the JSON files for use by the ontology on application startup.",
            response = GroupExternalEntity.class, responseContainer = "Set")
    @PostMapping(path = ENDPOINT__POST_GROUP_EXTERNAL_ENTITIES_CSV, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Set<GroupExternalEntity> generateGroupsJsonFromCsv(
            @ApiParam( name="dataAsCsvFile", value = "A CSV file containing group info, including the proposed member ranges ", required = true)
            @RequestPart MultipartFile dataAsCsvFile) throws Exception {
        return externalEntityService.processGroupAdditionCSV(dataAsCsvFile.getInputStream());
    }

    @GetMapping(path = ENDPOINT__GET_ALL_EXTERNAL_ENTITIES)
    public Set<? extends ExternalEntity> findAllExternalEntities(@RequestParam Optional<String> entityId) {
        if (entityId.isPresent())
            return Collections.singleton(externalEntityService.findEntityByEntityId(entityId.get()));
        return externalEntityService.findAllEntities();
    }

    //
    // LIFTING ENDPOINTS
    //

    @PostMapping(path = ENDPOINT__LIFT_EXTERNAL_ENTITIES, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Collection<ExternalEntity> lift(
            @RequestPart MultipartFile dataAsCsvFile,
            @RequestPart String tag,
            @RequestPart String configurationAsJsonString
    ) throws IOException {
        LiftingConfiguration configuration = new ObjectMapper().readValue(configurationAsJsonString, LiftingConfiguration.class);
        return liftingService.liftEntities(dataAsCsvFile.getInputStream(), tag, configuration);
    }

}
