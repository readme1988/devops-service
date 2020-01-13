package io.choerodon.devops.app.service;

import java.util.List;

import com.github.pagehelper.PageInfo;

import org.springframework.data.domain.Pageable;
import io.choerodon.devops.api.vo.iam.AppServiceAndVersionVO;
import io.choerodon.devops.app.eventhandler.payload.*;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  10:27 2019/8/8
 * Description:
 */
public interface OrgAppMarketService {
    /**
     * 根据appId 查询应用服务
     *
     * @param appId
     * @param pageable
     * @param params
     * @return
     */
    PageInfo<AppServiceUploadPayload> pageByAppId(Long appId,
                                                  Pageable pageable,
                                                  String params);

    /**
     * 根据appServiceId 查询所有服务版本
     *
     * @param appServiceId
     * @return
     */
    List<AppServiceVersionUploadPayload> listServiceVersionsByAppServiceId(Long appServiceId);


    /**
     * 应用上传
     *
     * @param marketUploadVO
     */
    void uploadAPP(AppMarketUploadPayload marketUploadVO);

    /**
     * 应用上传 新增修复版本
     *
     * @param appMarketFixVersionPayload
     */
    void uploadAPPFixVersion(AppMarketFixVersionPayload appMarketFixVersionPayload);

    /**
     * 应用下载
     *
     * @param appServicePayload
     */
    void downLoadApp(AppMarketDownloadPayload appServicePayload);

    /**
     * 应用市场下载失败删除gitlab相关项目
     * @param marketDelGitlabProPayload
     */
    void deleteGitlabProject(MarketDelGitlabProPayload marketDelGitlabProPayload) ;

        /**
         * 根据versionId查询应用服务版本
         * 保留原排序
         * @param versionVOList
         */
    List<AppServiceAndVersionVO> listVersions(List<AppServiceAndVersionVO> versionVOList);
}
