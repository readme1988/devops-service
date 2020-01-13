import React, { Fragment, useMemo } from 'react';
import { observer } from 'mobx-react-lite';
import { Action, Choerodon } from '@choerodon/boot';
import { Modal, Table, Spin } from 'choerodon-ui/pro';
import StatusTag from '../../../../../components/status-tag';
import eventStopProp from '../../../../../utils/eventStopProp';
import { getEnvStatus, statusMappings } from '../../../../../components/status-dot';
import ClickText from '../../../../../components/click-text';
import { handlePromptError } from '../../../../../utils';
import EnvModifyForm from '../../modals/env-modify';
import Modals from './modals';
import { useEnvironmentStore } from '../../../stores';
import { useMainStore } from '../../stores';
import { useEnvGroupStore } from './stores';

const { Column } = Table;
const envKey = Modal.key;
const modalKey = Modal.key;
const deleteKey = Modal.key;
const effectKey = Modal.key;
const formKey = Modal.key;

const Group = observer(() => {
  const modalStyle = useMemo(() => ({
    width: 380,
  }), []);
  const {
    intlPrefix,
    envStore,
    treeDs,
    AppState: { currentMenuType: { id: projectId } },
  } = useEnvironmentStore();
  const { mainStore } = useMainStore();
  const {
    groupDs,
    intl: { formatMessage },
  } = useEnvGroupStore();
  const { getSelectedMenu: { name } } = envStore;

  function refresh() {
    groupDs.query();
    treeDs.query();
  }

  async function openDelete(record) {
    const envId = record.get('id');
    const envName = record.get('name');

    const deleteModal = Modal.open({
      key: deleteKey,
      title: formatMessage({ id: `${intlPrefix}.delete.title` }, { name: envName }),
      children: <Spin />,
      footer: null,
      movable: false,
    });

    try {
      const res = await checkStatus(record);

      if (res) {
        const result = await mainStore.checkDelete(projectId, envId);
        if (result && result.failed) {
          deleteModal.close();
        } else if (result) {
          deleteModal.update({
            children: formatMessage({ id: `${intlPrefix}.delete.des.resource.confirm` }),
            okText: formatMessage({ id: 'delete' }),
            okProps: { color: 'red' },
            cancelProps: { color: 'dark' },
            onOk: handleDelete,
            footer: ((okBtn, cancelBtn) => (
              <Fragment>
                {cancelBtn}{okBtn}
              </Fragment>
            )),
          });
        } else {
          deleteModal.update({
            children: formatMessage({ id: `${intlPrefix}.delete.des.pipeline.confirm` }),
            okText: formatMessage({ id: 'iknow' }),
            footer: ((okBtn) => (
              <Fragment>
                {okBtn}
              </Fragment>
            )),
          });
        }
      } else {
        deleteModal.update({
          children: formatMessage({ id: `${intlPrefix}.status.change` }),
          onOk: refresh,
          footer: ((okBtn, cancelBtn) => (
            <Fragment>
              {okBtn}
            </Fragment>
          )),
        });
      }
    } catch (e) {
      Choerodon.handlePromptError(e);
      deleteModal.close();
    }
  }

  async function handleDelete() {
    const envId = groupDs.current ? groupDs.current.get('id') : null;
    try {
      const res = await mainStore.deleteEnv(projectId, envId);
      handlePromptError(res);
    } catch (e) {
      Choerodon.handleResponseError(e);
    } finally {
      refresh();
    }
  }

  async function handleEffect(envId, target) {
    try {
      const res = await mainStore.effectEnv(projectId, envId, target);
      handlePromptError(res);
    } catch (e) {
      Choerodon.handleResponseError(e);
    } finally {
      refresh();
    }
  }

  function checkStatus(record) {
    const envId = record.get('id');
    const oldStatus = getStatusInRecord(record);
    return new Promise((resolve) => {
      mainStore.checkStatus(projectId, envId).then((res) => {
        if (res && res.id) {
          const newStatus = getEnvStatus(res);
          resolve(newStatus === oldStatus);
        }
      });
    });
  }

  async function openEffectModal(record) {
    const envId = record.get('id');
    const envName = record.get('name');
    const effectModal = Modal.open({
      key: effectKey,
      title: formatMessage({ id: `${intlPrefix}.stop.title` }, { name: envName }),
      children: <Spin />,
      footer: null,
      movable: false,
    });
    const res = await checkStatus(record);
    if (res) {
      try {
        const result = await mainStore.checkStop(projectId, envId);
        if (handlePromptError(result)) {
          effectModal.update({
            children: formatMessage({ id: `${intlPrefix}.stop.des` }),
            okText: formatMessage({ id: 'ok' }),
            okCancel: true,
            onOk: () => handleEffect(envId, false),
            footer: ((okBtn, cancelBtn) => (
              <Fragment>
                {okBtn}{cancelBtn}
              </Fragment>
            )),
          });
        } else if (!result.failed) {
          effectModal.update({
            children: formatMessage({ id: `${intlPrefix}.no.stop.des` }),
            okText: formatMessage({ id: 'iknow' }),
            footer: ((okBtn, cancelBtn) => (
              <Fragment>
                {okBtn}
              </Fragment>
            )),
          });
        } else {
          effectModal.close();
        }
      } catch (error) {
        Choerodon.handleResponseError(error);
        effectModal.close();
      }
    } else {
      effectModal.update({
        children: formatMessage({ id: `${intlPrefix}.status.change` }),
        onOk: refresh,
        footer: (okBtn, cancelBtn) => (
          <Fragment>
            {okBtn}
          </Fragment>
        ),
      });
    }
  }

  async function openModifyModal(record) {
    const modifyModal = Modal.open({
      key: formKey,
      title: formatMessage({ id: `${intlPrefix}.modify` }),
      style: modalStyle,
      children: <Spin />,
      drawer: true,
      okCancel: false,
      okText: formatMessage({ id: 'close' }),
    });
    try {
      const res = await checkStatus(record);
      if (res) {
        modifyModal.update({
          okCancel: true,
          children: <EnvModifyForm
            intlPrefix={intlPrefix}
            refresh={refresh}
            record={record}
            store={envStore}
          />,
          okText: formatMessage({ id: 'save' }),
        });
      } else {
        modifyModal.update({
          children: formatMessage({ id: `${intlPrefix}.status.change` }),
          okText: formatMessage({ id: 'iknow' }),
          onOk: refresh,
        });
      }
    } catch (error) {
      Choerodon.handlePromptError(error);
      modifyModal.close();
    }
  }

  function getStatusInRecord(record) {
    const active = record.get('active');
    const connect = record.get('connect');
    const failed = record.get('failed');
    const synchronize = record.get('synchro');
    return getEnvStatus({
      active,
      connect,
      failed,
      synchronize,
    });
  }

  function renderName({ value, record }) {
    const { RUNNING, DISCONNECTED } = statusMappings;
    const status = getStatusInRecord(record);
    return (
      <Fragment>
        <StatusTag
          colorCode={status}
          name={formatMessage({ id: status })}
        />
        <ClickText
          value={value}
          clickAble={status === RUNNING || status === DISCONNECTED}
          onClick={openModifyModal.bind(this, record)}
          record={record}
        />
      </Fragment>
    );
  }

  function renderActions({ record }) {
    const { RUNNING, DISCONNECTED, FAILED, OPERATING, STOPPED } = statusMappings;

    const status = getStatusInRecord(record);
    const envId = record.get('id');
    if (status === OPERATING) return null;

    let actionData = [];

    switch (status) {
      case RUNNING:
        actionData = [{
          service: [],
          text: formatMessage({ id: `${intlPrefix}.modal.detail.stop` }),
          action: () => openEffectModal(record),
        }, {
          service: [],
          text: formatMessage({ id: `${intlPrefix}.modal.detail.modify` }),
          action: openModifyModal.bind(this, record),
        }];
        break;
      case DISCONNECTED:
        actionData = [{
          service: [],
          text: formatMessage({ id: `${intlPrefix}.modal.detail.modify` }),
          action: () => openModifyModal(record),
        }, {
          service: [],
          text: formatMessage({ id: `${intlPrefix}.modal.detail.delete` }),
          action: () => openDelete(record),
        }];
        break;
      case STOPPED:
        actionData = [{
          service: [],
          text: formatMessage({ id: `${intlPrefix}.modal.detail.start` }),
          action: () => handleEffect(envId, true),
        }, {
          service: [],
          text: formatMessage({ id: `${intlPrefix}.modal.detail.delete` }),
          action: () => openDelete(record),
        }];
        break;
      case FAILED:
        actionData = [{
          service: [],
          text: formatMessage({ id: `${intlPrefix}.modal.detail.delete` }),
          action: () => openDelete(record),
        }];
        break;
      default:
    }

    return <Action
      placement="bottomRight"
      data={actionData}
      onClick={eventStopProp}
    />;
  }

  return (
    <Fragment>
      <h2>{name}</h2>
      <Table
        dataSet={groupDs}
        border={false}
        queryBar="none"
      >
        <Column name="name" renderer={renderName} />
        <Column renderer={renderActions} width={100} />
        <Column name="description" />
        <Column name="clusterName" />
      </Table>
      <Modals />
    </Fragment>
  );
});

export default Group;
