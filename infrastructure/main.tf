locals {
  product                  = "pip"
  component                = "subscription-mgmt"
  builtFrom                = "hmcts/jenkins/subscription.management"
  resource_group_name      = "pip-sharedinfra-${var.env}-rg"
  storage_account_name     = "pipsharedinfrasa${var.env}"
  dtu_storage_account_name = "pipdtu${var.env}"
  team_name                = "PIP DevOps"
  team_contact             = "#vh-devops"
  env_long_name            = var.env == "sbox" ? "sandbox" : var.env == "stg" ? "staging" : var.env
  postgresql_user          = "pipdbadmin"
  secret_prefix            = "pip-subscription.management-POSTGRES"

}

data "azurerm_subnet" "postgres" {
  name                 = "iaas"
  resource_group_name  = "ss-${var.env}-network-rg"
  virtual_network_name = "ss-${var.env}-vnet"
}

module "database" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=postgresql_tf"
  product            = local.product
  component          = local.component
  subnet_id          = data.azurerm_subnet.postgres.id
  location           = var.location
  env                = local.env_long_name
  postgresql_user    = local.postgresql_user
  database_name      = "subscriptions"
  common_tags        = var.common_tags
  subscription       = local.env_long_name
  business_area      = "SDS"
  postgresql_version = 10

  key_vault_rg   = "genesis-rg"
  key_vault_name = "dtssharedservices${var.env}kv"
}

data "azurerm_key_vault" "ss_kv" {
  name                = "${local.product}-shared-kv-${var.env}"
  resource_group_name = "${local.product}-sharedservices-${var.env}-rg"
}

locals {
  secrets = [
    {
      name  = "DATABASE"
      value = module.database.name
    },
    {
      name  = "PORT"
      value = module.database.postgresql_listen_port
    },
    {
      name  = "USER"
      value = module.database.user_name
    },
    {
      name  = "PASS"
      value = module.database.postgresql_password
    }
  ]
}

resource "azurerm_key_vault_secret" "db-name" {
  for_each        = { for secret in local.secrets : secret.name => secret }
  key_vault_id    = data.azurerm_key_vault.ss_kv.id
  name            = "${local.secret_prefix}-${each.value.name}"
  value           = each.value.value
  tags            = merge(var.common_tags, { "source" : "PostgreSQL" })
  content_type    = ""
  expiration_date = timeadd(timestamp(), "8760h")
}
