import React, { createContext, useContext, useEffect, useMemo } from 'react';
import { inject } from 'mobx-react';
import { injectIntl } from 'react-intl';
import { DataSet } from 'choerodon-ui/pro';
import useStore from './useStore';
import AppServiceDs from './AppServiceDataSet';
import SelectAppDataSet from './SelectAppDataSet';
import handleMapStore from '../main-view/store/handleMapStore';

const Store = createContext();

export function useCodeManagerStore() {
  return useContext(Store);
}

export const StoreProvider = injectIntl(inject('AppState')(
  (props) => {
    const {
      children,
      AppState: {
        currentMenuType: { id: projectId },
      },
    } = props;

    const checkHasApp = (value, recentApp) => recentApp.some(e => e.id === value);

    const unshiftPop = (value, recentApp, recentAppList) => { // 有数据的话又再一次访问这个appservice则把他放到数组第一位
      for (let i = 0; i < recentApp.length; i++) {
        if (recentApp[i].id === value[0].id) {
          recentApp.splice(i, 1); // 如果数据组存在该元素，则把该元素删除
          break;
        }
      }
      recentApp.unshift(value[0]);
      recentAppList[projectId] = recentApp;
      localStorage.setItem('recent-app', JSON.stringify(recentAppList));
    };

    const setLocalStorage = (value) => {
      const recentAppList = localStorage.getItem('recent-app') && JSON.parse(localStorage.getItem('recent-app'));
      const temp = appServiceDs.toData().filter(e => e.id === value);
      const objTemp = {};
      if (recentAppList !== null && recentAppList[projectId]) {
        const recentApp = recentAppList[projectId];
        if (!checkHasApp(value, recentApp)) { // 先校验localstorage里面有没有这个数据
          recentApp.unshift(temp[0]);
          if (recentApp.length > 5) {
            recentApp.splice(-1, 1);
          }
          recentAppList[projectId] = recentApp;
          localStorage.setItem('recent-app', JSON.stringify(recentAppList));
        } else {
          unshiftPop(temp, recentApp, recentAppList);
        }
      } else if (recentAppList === null) {
        objTemp[projectId] = [temp[0]];
        localStorage.setItem('recent-app', JSON.stringify(objTemp));
      } else {
        recentAppList[projectId] = [temp[0]];
        localStorage.setItem('recent-app', JSON.stringify(recentAppList));
      }
    };

    const handleDataSetChange = ({ dataSet, record, value, oldValue }) => {
      if (!value) return;

      const option = {
        props: {
          children: record.get('name'),
          value: record.get('id'),
        },
      };
      setLocalStorage(value);
      Object.keys(handleMapStore)
        .forEach((key) => {
          if (key.indexOf('Code') !== -1) {
            handleMapStore[key]
              && handleMapStore[key].select
              && handleMapStore[key].select(value, option);
          }
        });
    };
    const appServiceDs = useMemo(() => new DataSet(AppServiceDs({ projectId })), []);
    const selectAppDs = useMemo(() => new DataSet(SelectAppDataSet({ handleDataSetChange })), []);
    const codeManagerStore = useStore();
    const permissions = useMemo(() => ([
      'devops-service.devops-git.pageTagsByOptions',
      'devops-service.devops-git.createTag',
      'devops-service.devops-git.checkTag',
      'devops-service.devops-git.createBranch',
      'devops-service.devops-git.deleteBranch',
      'devops-service.devops-git.updateBranchIssue',
      'devops-service.devops-git.pageBranchByOptions',
      'devops-service.pipeline.pageByOptions',
      'devops-service.application.getSonarQube',
      'devops-service.devops-git.queryUrl',
      'devops-service.devops-git.listMergeRequest',
      'devops-service.app-service.listByActive',
      'choerodon.route.develop.code-management',
    ]), []);

    useEffect(() => {
      codeManagerStore.checkHasApp(projectId);
    }, []);
    useEffect(() => {
      appServiceDs.transport.read = () => ({
        url: `/devops/v1/projects/${projectId}/app_service/list_by_active`,
        method: 'get',
      });
      const recentAppList = localStorage.getItem('recent-app') && JSON.parse(localStorage.getItem('recent-app'));
      appServiceDs.query().then((res) => {
        if (recentAppList !== null && recentAppList[projectId]) {
          selectAppDs.current && selectAppDs.current.set('appServiceId', recentAppList[projectId][0].id);
        } else if (res && res.length && res.length > 0) {
          selectAppDs.current.set('appServiceId', res[0].id);
        }
      });
    }, [projectId]);
    const value = {
      ...props,
      prefixCls: 'c7ncd-code-manager',
      intlPrefix: 'c7ncd.code-manager',
      itemType: {
        CIPHER_ITEM: 'secrets',
        CUSTOM_ITEM: 'customResources',
      },
      codeManagerStore,
      permissions,
      appServiceDs,
      selectAppDs,
      projectId,
    };
    return (
      <Store.Provider value={value}>
        {children}
      </Store.Provider>
    );
  },
));
