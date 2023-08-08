ARG APP_INSIGHTS_AGENT_VERSION=3.4.14
FROM hmctspublic.azurecr.io/base/java:17-distroless

ENV APP pip-subscription-management.jar

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/$APP /opt/app/

EXPOSE 4550
CMD [ "pip-subscription-management.jar" ]
