FROM registry.cn-shanghai.aliyuncs.com/c7n/devops-base:0.7.4 as pyinstaller
FROM registry.cn-shanghai.aliyuncs.com/choerodon-tools/javabase:0.8.2
# Install kaniko
COPY --from=pyinstaller /home/yaml/dist/values_yaml /usr/lib/yaml
COPY --from=pyinstaller /usr/bin/kaniko /usr/bin/kaniko
COPY app.jar /devops-service.jar
CMD java $JAVA_OPTS $SKYWALKING_OPTS -jar /devops-service.jar