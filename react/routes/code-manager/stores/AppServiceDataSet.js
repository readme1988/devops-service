export default ({ projectId }) => ({
  paging: false,
  transport: {
    read: () => ({
      url: `/devops/v1/projects/${projectId}/app_service/list_by_active`,
      method: 'get',
    }),
  },
});
