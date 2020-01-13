import React, { createContext, useContext, useMemo, useEffect } from 'react';
import { inject } from 'mobx-react';
import { injectIntl } from 'react-intl';
import { DataSet } from 'choerodon-ui/pro';
import VersionDataSet from './VersionDataSet';
import AllocationDataSet from './AllocationDataSet';
import ShareDataSet from './ShareDataSet';
import usePermissionStore from '../modals/stores/useStore';
import OptionsDataSet from './OptionsDataSet';
import { useAppTopStore } from '../../stores';
import DetailDataSet from './DetailDataSet';

const Store = createContext();

export function useServiceDetailStore() {
  return useContext(Store);
}


export const StoreProvider = injectIntl(inject('AppState')(
  (props) => {
    const {
      AppState: { currentMenuType: { projectId, organizationId } },
      intl: { formatMessage },
      match: { params: { id } },
      children,
    } = props;
    const emptyDataSetConfig = { transport: { read: { method: 'get' } }, paging: false };
    const { appServiceStore, intlPrefix } = useAppTopStore();
    const shareVersionsDs = useMemo(() => new DataSet(emptyDataSetConfig), []);
    const shareLevelDs = useMemo(() => new DataSet(emptyDataSetConfig), []);
    const versionDs = useMemo(() => new DataSet(VersionDataSet(formatMessage, projectId, id)), [formatMessage, id, projectId]);
    const permissionDs = useMemo(() => new DataSet(AllocationDataSet(formatMessage, intlPrefix, projectId, id)), [formatMessage, id, projectId]);
    const shareDs = useMemo(() => new DataSet(ShareDataSet(intlPrefix, formatMessage, projectId, id)), [formatMessage, id, projectId]);
    const detailDs = useMemo(() => new DataSet(DetailDataSet(intlPrefix, formatMessage)), []);
    const nonePermissionDs = useMemo(() => new DataSet(OptionsDataSet()), []);
    const permissionStore = usePermissionStore();

    useEffect(() => {
      nonePermissionDs.transport.read.url = `/devops/v1/projects/${projectId}/app_service/${id}/list_non_permission_users`;
      detailDs.transport.read = {
        url: `/devops/v1/projects/${projectId}/app_service/${id}`,
        method: 'get',
      };
      detailDs.query();
    }, [projectId, id]);

    useEffect(() => {
      shareLevelDs.transport.read.url = `/devops/v1/projects/${projectId}/app_service/${organizationId}/list_projects`;
      appServiceStore.judgeRole(organizationId, projectId);
    }, [organizationId, projectId]);

    const value = {
      ...props,
      versionDs,
      permissionDs,
      shareDs,
      detailDs,
      nonePermissionDs,
      permissionStore,
      shareVersionsDs,
      shareLevelDs,
      params: {
        projectId,
        id,
      },
    };
    return (
      <Store.Provider value={value}>
        {children}
      </Store.Provider>
    );
  },
));
