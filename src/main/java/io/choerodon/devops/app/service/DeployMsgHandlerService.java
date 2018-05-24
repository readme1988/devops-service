package io.choerodon.devops.app.service;

import io.choerodon.websocket.Msg;

/**
 * Created by Zenger on 2018/4/17.
 */
public interface DeployMsgHandlerService {

    void handlerReleaseInstall(String msg);

    void handlerPreInstall(String msg);

    void resourceUpdate(String key, Long envId, String msg);

    void resourceDelete(Long envId,String msg);

    void helmReleaseHookLogs(String key, String msg);

    void updateInstanceStatus(String key, String instanceStatus, String commandStatus, String commandMsg);

    void handlerDomainCreateMessage(String key, String msg);

    void helmReleasePreUpgrade(String msg);

    void handlerReleaseUpgrade(String msg);

    void helmReleaseDeleteFail(String key, String msg);

    void helmReleaseStartFail(String key, String msg);

    void helmReleaseRollBackFail(String key, String msg);

    void helmReleaseInstallFail(String key, String msg);

    void helmReleaseUpgradeFail(String key, String msg);

    void helmReleaeStopFail(String key, String msg);

    void netWorkUpdate(String key, String msg);

    void helmReleaseGetContent(String key, Long envId, String msg);
}
