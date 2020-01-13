package io.choerodon.devops.api.vo.kubernetes;

public class Resource {

    private String kind;
    private String resourceVersion;
    private String name;
    private String version;
    private String group;
    private String object;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(String resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "kind='" + kind + '\'' +
                ", resourceVersion='" + resourceVersion + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", group='" + group + '\'' +
                ", object='" + object + '\'' +
                '}';
    }
}
