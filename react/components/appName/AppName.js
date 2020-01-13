import React, { Fragment } from 'react';
import { FormattedMessage } from 'react-intl';
import PropTypes from 'prop-types';
import { Tooltip, Icon } from 'choerodon-ui';
import MouseOverWrapper from '../MouseOverWrapper';
import './AppName.less';

/**
 * 带icon的应用名称
 * @param { 应用名称，显示应用前icon，本组织or应用市场 } props
 */
export default function AppName(props) {
  const { name, showIcon, self, width, isInstance, hoverName } = props;
  let icon;
  let type;
  if (isInstance) {
    icon = self;
    if (self === 'share') {
      type = 'share';
    } else if (self === 'application_market') {
      type = 'market';
    } else {
      type = 'project';
    }
  } else {
    icon = self ? 'widgets' : 'apps';
    type = self ? 'project' : 'market';
  }

  return (
    <Fragment>
      {showIcon ? (
        <Tooltip title={<FormattedMessage id={type} />}>
          <Icon type={icon} className="c7ncd-app-icon" />
        </Tooltip>
      ) : null}
      {hoverName ? (
        <MouseOverWrapper className="c7ncd-app-text" width={width}>
          {name}
        </MouseOverWrapper>
      ) : <MouseOverWrapper className="c7ncd-app-text" text={name} width={width}>
        {name}
      </MouseOverWrapper>}
    </Fragment>
  );
}

AppName.propTypes = {
  name: PropTypes.string.isRequired,
  showIcon: PropTypes.bool.isRequired,
  self: PropTypes.bool.isRequired,
  hoverName: PropTypes.bool,
  width: PropTypes.oneOfType([
    PropTypes.string.isRequired,
    PropTypes.number.isRequired,
  ]),
};
