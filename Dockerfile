ARG APP_INSIGHTS_AGENT_VERSION=2.5.1
ARG APP

# Application image

FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/$APP /opt/app/

EXPOSE 4550
CMD [ "pip-subscription-management.jar" ]
