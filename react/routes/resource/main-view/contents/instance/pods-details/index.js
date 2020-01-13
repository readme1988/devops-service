import React, { Fragment, memo, useState, useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { Action } from '@choerodon/boot';
import { Table } from 'choerodon-ui/pro';
import { Icon, Popover, Tooltip } from 'choerodon-ui';
import map from 'lodash/map';
import MouserOverWrapper from '../../../../../../components/MouseOverWrapper';
import TimePopover from '../../../../../../components/timePopover/TimePopover';
import StatusTags from '../../../../../../components/status-tag';
import { useResourceStore } from '../../../../stores';
import { useInstanceStore } from '../stores';
import LogSiderbar from '../../../../../../components/log-siderbar';
import TermSiderbar from '../../../../../../components/term-sidebar';

import './index.less';

const { Column } = Table;

const PodDetail = memo(() => {
  const {
    prefixCls,
    intlPrefix,
  } = useResourceStore();
  const {
    intl,
    podsDs,
  } = useInstanceStore();
  shellVisible;
  const [visible, setVisible] = useState(false);
  const [shellVisible, setShellVisible] = useState(false);

  function renderName({ value, record }) {
    const ready = record.get('ready');
    return (
      <Fragment>
        <Tooltip title={<FormattedMessage id={`ist.${ready ? 'y' : 'n'}`} />}>
          <Icon
            type={ready ? 'check_circle' : 'cancel'}
            className={`${prefixCls}-pod-ready-${ready ? 'check' : 'cancel'}`}
          />
        </Tooltip>
        <MouserOverWrapper text={value} width={0.2} style={{ display: 'inline' }}>
          {value}
        </MouserOverWrapper>
      </Fragment>
    );
  }

  function renderStatus({ value }) {
    const wrapStyle = {
      width: 54,
    };

    const statusMap = {
      Completed: [true, '#00bf96'],
      Running: [false, '#00bf96'],
      Error: [false, '#f44336'],
      Pending: [false, '#ff9915'],
    };

    const [wrap, color] = statusMap[value] || [true, 'rgba(0, 0, 0, 0.36)'];

    return (
      <StatusTags
        ellipsis={wrap}
        color={color}
        name={value}
        style={wrapStyle}
      />
    );
  }

  function renderContainers({ value }) {
    const node = [];
    let item;
    if (value && value.length) {
      item = value[0];
      map(value, ({ ready, name }, index) => {
        node.push(
          <div className="column-container-mt" key={index}>
            <Tooltip title={<FormattedMessage id={`ist.${ready ? 'y' : 'n'}`} />}>
              <Icon
                type={ready ? 'check_circle' : 'cancel'}
                className={`${prefixCls}-pod-ready-${ready ? 'check' : 'cancel'}`}
              />
            </Tooltip>
            <span>{name}</span>
          </div>,
        );
      });
    }
    return (
      <Fragment>
        {item && (
          <Fragment>
            <Tooltip title={<FormattedMessage id={`ist.${item.ready ? 'y' : 'n'}`} />}>
              <Icon
                type={item.ready ? 'check_circle' : 'cancel'}
                className={`${prefixCls}-pod-ready-${item.ready ? 'check' : 'cancel'}`}
              />
            </Tooltip>
            <MouserOverWrapper text={item.name} width={0.1} style={{ display: 'inline' }}>
              {item.name}
            </MouserOverWrapper>
          </Fragment>)}
        {node.length > 1 && (
          <Popover
            arrowPointAtCenter
            placement="bottomRight"
            content={<Fragment>{node}</Fragment>}
            overlayClassName={`${prefixCls}-pods-popover`}
          >
            <Icon type="expand_more" className="container-expend-icon" />
          </Popover>
        )}
      </Fragment>
    );
  }

  const renderDate = useCallback(({ value }) => (
    <TimePopover content={value} />
  ), []);

  function renderAction() {
    const buttons = [
      {
        service: [],
        text: intl.formatMessage({ id: `${intlPrefix}.instance.log` }),
        action: () => openLog(),
      },
      {
        service: [],
        text: intl.formatMessage({ id: `${intlPrefix}.instance.term` }),
        action: () => openShell(),
      },
      {
        service: [],
        text: intl.formatMessage({ id: 'delete' }),
        action: () => deletePod(),
      },
    ];
    return <Action data={buttons} />;
  }
  /**
   * 控制Log侧边窗的可见性
   */
  function openLog() {
    setVisible(true);
  }
  function closeLog() {
    setVisible(false);
  }
  /**
   * 控制Shell侧边窗的可见性
   */
  function openShell() {
    setShellVisible(true);
  }
  function closeShell() {
    setShellVisible(false);
  }
  /**
   * 删除Pod
   */
  function deletePod() {
    const modalProps = {
      title: intl.formatMessage({ id: `${intlPrefix}.instance.pod.delete.title` }),
      children: intl.formatMessage({ id: `${intlPrefix}.instance.pod.delete.des` }),
      okText: intl.formatMessage({ id: 'delete' }),
      okProps: { color: 'red' },
      cancelProps: { color: 'dark' },
    };
    podsDs.delete(podsDs.current, modalProps);
  }

  return (
    <Fragment>
      <div className="c7ncd-tab-table">
        <Table
          dataSet={podsDs}
          border={false}
          queryBar="bar"
          className={`${prefixCls}-instance-pods`}
        >
          <Column name="name" renderer={renderName} />
          <Column renderer={renderAction} width="0.7rem" />
          <Column name="containers" renderer={renderContainers} />
          <Column name="ip" width="1.2rem" />
          <Column name="creationDate" sortable renderer={renderDate} width="1rem" />
          <Column name="status" renderer={renderStatus} width="1rem" />
        </Table>
      </div>
      {visible && <LogSiderbar visible={visible} onClose={closeLog} record={podsDs.current.toData()} />}
      {shellVisible && <TermSiderbar visible={shellVisible} onClose={closeShell} record={podsDs.current.toData()} />}
    </Fragment>
  );
});

export default PodDetail;
