package io.choerodon.devops.app.service;

import java.util.List;

import com.github.pagehelper.PageInfo;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Pageable;
import io.choerodon.devops.api.vo.C7nCertificationVO;
import io.choerodon.devops.api.vo.CertificationRespVO;
import io.choerodon.devops.api.vo.CertificationVO;
import io.choerodon.devops.api.vo.ProjectCertificationVO;
import io.choerodon.devops.api.vo.kubernetes.C7nCertification;
import io.choerodon.devops.infra.dto.CertificationDTO;
import io.choerodon.devops.infra.dto.CertificationFileDTO;

/**
 * Created by n!Ck
 * Date: 2018/8/20
 * Time: 17:43
 * Description:
 */
public interface CertificationService {

    /**
     * 创建c7n证书
     *
     * @param projectId        项目id
     * @param certificationDTO 证书
     */
    void createCertification(Long projectId, C7nCertificationVO certificationDTO,
                             MultipartFile key, MultipartFile cert, Boolean isGitOps);

    C7nCertification getC7nCertification(String name, String type, List<String> domains,
                                         String keyContent, String certContent, String envCode);

    void deleteById(Long certId);

    void certDeleteByGitOps(Long certId);

    PageInfo<CertificationVO> pageByOptions(Long projectId, Long envId, Pageable pageable, String params);

    List<CertificationVO> queryActiveCertificationByDomain(Long projectId, Long envId, String domain);

    Boolean checkCertNameUniqueInEnv(Long envId, String certName);


    /**
     * 根据证书名称查询证书
     *
     * @param envId    环境ID
     * @param certName 证书名称
     * @return CertificationVO
     */
    CertificationVO queryByName(Long envId, String certName);

    /**
     * 根据证书ID查询证书
     *
     * @param certId 证书ID
     * @return 证书信息
     */
    CertificationRespVO queryByCertId(Long certId);

    Long createCertCommand(String type, Long certId, Long userId);

    List<ProjectCertificationVO> listProjectCertInProject(Long projectId);

    CertificationDTO baseCreate(CertificationDTO certificationVO);

    CertificationDTO baseQueryById(Long certId);

    CertificationDTO baseQueryByEnvAndName(Long envId, String name);

    PageInfo<CertificationDTO> basePage(Long projectId, Long envId, Pageable pageable, String params);

    List<CertificationDTO> baseQueryActiveByDomain(Long projectId, Long clusterId, String domain);

    void baseUpdateCommandId(CertificationDTO certificationDTO);

    void baseUpdateValidField(CertificationDTO inputCertificationDTO);

    void baseUpdateCertFileId(CertificationDTO inputCertificationDTO);

    void baseClearValidField(Long certId);

    void baseDeleteById(Long certId);

    Boolean baseCheckCertNameUniqueInEnv(Long envId, String certName);

    Long baseStoreCertFile(CertificationFileDTO certificationFileDTO);

    CertificationFileDTO baseQueryCertFile(Long certId);

    List<CertificationDTO> baseListByEnvId(Long envId);

    void baseUpdateSkipProjectPermission(CertificationDTO certificationDTO);

    CertificationDTO baseQueryByProjectAndName(Long projectId, String name);

    List<CertificationDTO> baseListByOrgCertId(Long orgCertId);

    List<CertificationDTO> baseListByProject(Long projectId, Long organizationId);

    void updateStatus(CertificationDTO certificationDTO);
}
