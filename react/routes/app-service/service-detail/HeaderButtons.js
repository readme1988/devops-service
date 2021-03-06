import React, { memo, useMemo, Fragment } from 'react';
import { FormattedMessage } from 'react-intl';
import { Header, Permission, Choerodon } from '@choerodon/boot';
import { Button, Tooltip } from 'choerodon-ui';
import { Modal, Spin } from 'choerodon-ui/pro';
import { observer } from 'mobx-react-lite';
import { useAppTopStore } from '../stores';
import { useServiceDetailStore } from './stores';
import Detail from './modals/detail';
import CreateForm from '../modals/creat-form';
import EditForm from '../modals/edit-form';
import { handlePromptError } from '../../../utils';

const modalKey1 = Modal.key();
const modalKey2 = Modal.key();
const modalKey3 = Modal.key();
const editModalKey = Modal.key();
const modalStyle = {
  width: 380,
};


const HeaderButtons = observer(({ children }) => {
  const {
    intlPrefix,
    prefixCls,
    appServiceStore,
  } = useAppTopStore();
  const {
    intl: { formatMessage },
    AppState: { currentMenuType: { id: projectId } },
    detailDs,
  } = useServiceDetailStore();

  const serviceActive = useMemo(() => detailDs.current && detailDs.current.get('active'), [detailDs.current]);

  function refresh() {
    detailDs.query();
  }

  function openDetail() {
    const detailModal = Modal.open({
      key: modalKey1,
      title: formatMessage({ id: `${intlPrefix}.detail` }),
      children: <Detail record={detailDs.current} intlPrefix={intlPrefix} prefixCls={prefixCls} formatMessage={formatMessage} />,
      drawer: true,
      style: modalStyle,
      footer: (
        <Button funcType="raised" type="primary" onClick={() => detailModal.close()}>
          <FormattedMessage id="close" />
        </Button>
      ),
    });
  }

  function openEdit() {
    const appServiceId = detailDs.current.get('id');

    Modal.open({
      key: editModalKey,
      drawer: true,
      style: modalStyle,
      title: formatMessage({ id: `${intlPrefix}.edit` }),
      children: <EditForm
        refresh={refresh}
        intlPrefix={intlPrefix}
        prefixCls={prefixCls}
        appServiceId={appServiceId}
      />,
      okText: formatMessage({ id: 'save' }),
    });
  }

  function getActiveText() {
    const active = serviceActive ? 'disable' : 'enable';
    return <FormattedMessage id={`${intlPrefix}.${active}`} />;
  }

  function openStop(record) {
    const id = record.get('id');

    const stopModal = Modal.open({
      key: modalKey3,
      title: formatMessage({ id: `${intlPrefix}.check` }),
      children: <Spin />,
      footer: null,
    });

    appServiceStore.checkAppService(projectId, id).then((res) => {
      if (handlePromptError(res)) {
        const { checkResources, checkRule } = res;
        const status = checkResources || checkRule;
        let childrenContent;

        if (!status) {
          childrenContent = <FormattedMessage id={`${intlPrefix}.stop.tips`} />;
        } else if (checkResources && !checkRule) {
          childrenContent = formatMessage({ id: `${intlPrefix}.has.resource` });
        } else if (!checkResources && checkRule) {
          childrenContent = formatMessage({ id: `${intlPrefix}.has.rules` });
        } else {
          childrenContent = formatMessage({ id: `${intlPrefix}.has.both` });
        }

        const statusObj = {
          title: status ? formatMessage({ id: `${intlPrefix}.cannot.stop` }) : formatMessage({ id: `${intlPrefix}.stop` }, { name: detailDs.current.get('name') }),
          // eslint-disable-next-line no-nested-ternary
          children: childrenContent,
          okCancel: !status,
          onOk: () => (status ? stopModal.close() : handleChangeActive(false)),
          okText: status ? formatMessage({ id: 'iknow' }) : formatMessage({ id: 'stop' }),
          footer: ((okBtn, cancelBtn) => (
            <Fragment>
              {okBtn}
              {!status && cancelBtn}
            </Fragment>
          )),
        };
        stopModal.update(statusObj);
      } else {
        stopModal.close();
      }
    }).catch((err) => {
      stopModal.close();
      Choerodon.handleResponseError(err);
    });
  }

  async function changeActive() {
    const { current } = detailDs;
    if (current.get('active')) {
      Modal.open({
        key: modalKey2,
        title: formatMessage({ id: `${intlPrefix}.stop` }, { name: detailDs.current.get('name') }),
        children: <FormattedMessage id={`${intlPrefix}.stop.tips`} />,
        onOk: () => handleChangeActive(false),
        okText: formatMessage({ id: 'stop' }),
      });
    } else {
      handleChangeActive(true);
    }
  }

  async function handleChangeActive(active) {
    const { current } = detailDs;
    try {
      if (await appServiceStore.changeActive(projectId, current.get('id'), active) !== false) {
        detailDs.query();
      } else {
        return false;
      }
    } catch (e) {
      Choerodon.handleResponseError(e);
      return false;
    }
  }


  return (
    <Header>
      <Permission
        service={['devops-service.app-service.update']}
      >
        <Tooltip
          title={!serviceActive ? <FormattedMessage id={`${intlPrefix}.button.disabled`} /> : ''}
          placement="bottom"
        >
          <Button
            icon="mode_edit "
            onClick={openEdit}
            disabled={!serviceActive}
          >
            <FormattedMessage id={`${intlPrefix}.edit`} />
          </Button>
        </Tooltip>
      </Permission>
      <Permission
        service={['devops-service.app-service.updateActive']}
      >
        <Button
          icon={serviceActive ? 'remove_circle_outline' : 'finished'}
          onClick={serviceActive ? openStop.bind(this, detailDs.current) : handleChangeActive.bind(this, true)}
        >
          {getActiveText()}
        </Button>
      </Permission>
      <Permission
        service={['devops-service.app-service.query']}
      >
        <Button
          icon="find_in_page"
          onClick={openDetail}
        >
          <FormattedMessage id={`${intlPrefix}.detail`} />
        </Button>
      </Permission>
      {children}
    </Header>
  );
});

export default HeaderButtons;
