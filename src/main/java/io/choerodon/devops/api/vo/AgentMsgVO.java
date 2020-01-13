package io.choerodon.devops.api.vo;

/**
 * Created by Sheep on 2019/7/25.
 */
public class AgentMsgVO {

    private String key;
    private String type;
    private String payload;
    private Integer msgType;
    private Long commandId;
    private String clusterId;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Integer getMsgType() {
        return msgType;
    }

    public void setMsgType(Integer msgType) {
        this.msgType = msgType;
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(Long commandId) {
        this.commandId = commandId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    @Override
    public String toString() {
        return "AgentMsgVO{" +
                "key='" + key + '\'' +
                ", type='" + type + '\'' +
                ", payload='" + payload + '\'' +
                ", msgType=" + msgType +
                ", commandId=" + commandId +
                ", clusterId='" + clusterId + '\'' +
                '}';
    }
}
