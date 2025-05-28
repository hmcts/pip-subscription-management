terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      version = "4.24.0"
    }
    postgresql = {
      source  = "cyrilgdn/postgresql"
      version = ">=1.17.1"
    }
  }
}

provider "azurerm" {
  features {}
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "postgres_network"
  subscription_id            = var.aks_subscription_id
}
