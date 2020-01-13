package io.choerodon.devops.infra.dto;

import javax.persistence.*;
import java.util.Date;

import io.swagger.annotations.ApiModelProperty;

import io.choerodon.mybatis.entity.BaseDTO;


/**
 * Created with IntelliJ IDEA.
 * User: Runge
 * Date: 2018/4/9
 * Time: 14:23
 * Description:
 */

@Table(name = "devops_merge_request")
public class DevopsMergeRequestDTO extends BaseDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long gitlabProjectId;

    private Long gitlabMergeRequestId;

    private Long authorId;

    private Long assigneeId;

    private String sourceBranch;

    private String targetBranch;

    private String state;

    private String title;

    private Date createdAt;

    private Date updatedAt;

    @Transient
    private Long total;

    @Transient
    private Long merged;

    @Transient
    private Long closed;

    @Transient
    private Long opened;

    @Transient
    @ApiModelProperty("待这个用户审核的merge request的数量")
    private Long auditCount;

    public DevopsMergeRequestDTO() {
    }

    /**
     * constructor a new merge request item
     *
     * @param gitlabProjectId    devops application ID
     * @param sourceBranch source branch to merge
     * @param targetBranch target merge branch
     */
    public DevopsMergeRequestDTO(Long gitlabProjectId, String sourceBranch, String targetBranch) {
        this.gitlabProjectId = gitlabProjectId;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGitlabProjectId() {
        return gitlabProjectId;
    }

    public void setGitlabProjectId(Long gitlabProjectId) {
        this.gitlabProjectId = gitlabProjectId;
    }

    public Long getGitlabMergeRequestId() {
        return gitlabMergeRequestId;
    }

    public void setGitlabMergeRequestId(Long gitlabMergeRequestId) {
        this.gitlabMergeRequestId = gitlabMergeRequestId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }


    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getMerged() {
        return merged;
    }

    public void setMerged(Long merged) {
        this.merged = merged;
    }

    public Long getClosed() {
        return closed;
    }

    public void setClosed(Long closed) {
        this.closed = closed;
    }

    public Long getOpened() {
        return opened;
    }

    public void setOpened(Long opened) {
        this.opened = opened;
    }

    public Long getAuditCount() {
        return auditCount;
    }

    public void setAuditCount(Long auditCount) {
        this.auditCount = auditCount;
    }
}
