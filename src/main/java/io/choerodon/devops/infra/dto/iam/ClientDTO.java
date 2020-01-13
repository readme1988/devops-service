package io.choerodon.devops.infra.dto.iam;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModelProperty;

import io.choerodon.mybatis.entity.BaseDTO;

/**
 * @author superlee
 * @since 2019-04-22
 */
public class ClientDTO extends BaseDTO {

    private static final String REGEX = "^[a-z0-9A-Z]+$";

    @ApiModelProperty(value = "客户端ID/非必填")
    private Long id;

    @ApiModelProperty(value = "客户端名称/必填")
    @Size(min = 1, max = 12, message = "error.client.name.size")
    @NotNull(message = "error.clientName.null")
    @Pattern(regexp = REGEX, message = "error.client.name.regex")
    private String name;

    @ApiModelProperty(value = "组织ID/必填")
    private Long organizationId;

    @ApiModelProperty(value = "客户端资源/非必填/默认：default")
    private String resourceIds;

    @ApiModelProperty(value = "客户端秘钥/必填")
    @Size(min = 6, max = 16, message = "error.client.secret.size")
    @NotNull(message = "error.secret.null")
    @Pattern(regexp = REGEX, message = "error.client.secret.regex")
    private String secret;

    @ApiModelProperty(value = "作用域/非必填")
    private String scope;

    @ApiModelProperty(value = "授权类型/必填")
    @NotNull(message = "error.authorizedGrantTypes.null")
    private String authorizedGrantTypes;

    @ApiModelProperty(value = "重定向地址/非必填")
    private String webServerRedirectUri;

    @ApiModelProperty(value = "访问授权超时时间/必填")
    private Long accessTokenValidity;

    @ApiModelProperty(value = "授权超时时间/必填")
    private Long refreshTokenValidity;

    @ApiModelProperty(value = "附加信息/非必填")
    private String additionalInformation;

    @ApiModelProperty(value = "自动授权域/非必填")
    private String autoApprove;

    @ApiModelProperty(value = "集群id")
    private Long sourceId;

    @ApiModelProperty(value = "client类型")
    private String sourceType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(String resourceIds) {
        this.resourceIds = resourceIds;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getAuthorizedGrantTypes() {
        return authorizedGrantTypes;
    }

    public void setAuthorizedGrantTypes(String authorizedGrantTypes) {
        this.authorizedGrantTypes = authorizedGrantTypes;
    }

    public String getWebServerRedirectUri() {
        return webServerRedirectUri;
    }

    public void setWebServerRedirectUri(String webServerRedirectUri) {
        this.webServerRedirectUri = webServerRedirectUri;
    }

    public Long getAccessTokenValidity() {
        return accessTokenValidity;
    }

    public void setAccessTokenValidity(Long accessTokenValidity) {
        this.accessTokenValidity = accessTokenValidity;
    }

    public Long getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    public void setRefreshTokenValidity(Long refreshTokenValidity) {
        this.refreshTokenValidity = refreshTokenValidity;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public String getAutoApprove() {
        return autoApprove;
    }

    public void setAutoApprove(String autoApprove) {
        this.autoApprove = autoApprove;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    @Override
    public String toString() {
        return "ClientDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", organizationId=" + organizationId +
                ", resourceIds='" + resourceIds + '\'' +
                ", secret='" + secret + '\'' +
                ", scope='" + scope + '\'' +
                ", authorizedGrantTypes='" + authorizedGrantTypes + '\'' +
                ", webServerRedirectUri='" + webServerRedirectUri + '\'' +
                ", accessTokenValidity=" + accessTokenValidity +
                ", refreshTokenValidity=" + refreshTokenValidity +
                ", additionalInformation='" + additionalInformation + '\'' +
                ", autoApprove='" + autoApprove + '\'' +
                ", sourceId=" + sourceId +
                ", sourceType='" + sourceType + '\'' +
                '}';
    }
}
