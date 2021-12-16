# App infrastructure

This is the application specific infrastructure for PIP Subscription Management.

## Local validating
To validate against Azure, create a `override.tf` file in this folder with the following content

```terraform
terraform {
  backend "azurerm" {
    resource_group_name  = "jenkins-state-stg"
    storage_account_name = "sdsstatestg"
    container_name       = "tfstate-stg"
    key                  = "pip-subscription-management/stg/terraform.tfstate"
  }
}
```
