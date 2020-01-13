package io.choerodon.devops.infra.enums;

/**
 * Created by Sheep on 2019/5/14.
 */
public enum TriggerObject {

    HANDLER("handler"),
    PROJECT_OWNER("projectOwner"),
    SPECIFIER("specifier");

    private String object;

    TriggerObject(String object) {
        this.object = object;
    }

    public String getObject() {
        return object;
    }

}
