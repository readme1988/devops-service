export default ((intlPrefix, formatMessage, projectId) => ({
  autoQuery: true,
  paging: false,
  transport: {
    read: ({ data }) => ({
      url: `/devops/v1/projects/${projectId}/pipeline/page_by_options`,
      method: 'post',
      data: { triggerType: 'manual', executor: true, enabled: true, name: typeof data.params === 'string' ? [data.params] : data.params },
    }),

  },
  fields: [
    { name: 'name', type: 'string', label: formatMessage({ id: `${intlPrefix}.pipeline.name` }) },
    { name: 'createUserName', type: 'string', label: formatMessage({ id: 'creator' }) },
    { name: 'createUserRealName', type: 'string' },
    { name: 'createUserUrl', type: 'string' },
    { name: 'id', type: 'number' },
  ],
  queryFields: [],
}));
