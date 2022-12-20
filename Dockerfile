ARG APP_INSIGHTS_AGENT_VERSION=3.2.10
FROM hmctspublic.azurecr.io/base/java:17-distroless

ENV APP pip-subscription-management.jar

COPY build/libs/$APP /opt/app/

EXPOSE 4550
CMD [ "pip-subscription-management.jar" ]