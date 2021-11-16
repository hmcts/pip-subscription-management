locals {
  product                  = "pip"
  component                = "subscription-mgmt"
  builtFrom                = "hmcts/jenkins/subscription-management"
  resource_group_name      = "pip-sharedinfra-${var.env}-rg"
  storage_account_name     = "pipsharedinfrasa${var.env}"
  dtu_storage_account_name = "pipdtu${var.env}"
  team_name                = "PIP DevOps"
  team_contact             = "#vh-devops"
  env_long_name            = var.env == "sbox" ? "sandbox" : var.env == "stg" ? "staging" : var.env
  postgresql_user          = "pipdbadmin"
  postgresql_prefix        = "postgre"
}

data "azurerm_subnet" "postgres" {
  name                 = "iaas"
  resource_group_name  = "ss-${var.env}-network-rg"
  virtual_network_name = "ss-${var.env}-vnet"
}

module "databases" {
  for_each           = { for database in var.databases : database => database }
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=postgresql_tf"
  product            = local.product
  component          = local.component
  subnet_id          = data.azurerm_subnet.postgres.id
  location           = var.location
  env                = local.env_long_name
  postgresql_user    = local.postgresql_user
  database_name      = each.value
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

resource "azurerm_key_vault_secret" "db-host" {
  for_each        = { for database in module.databases : database.name => database }
  key_vault_id    = data.azurerm_key_vault.ss_kv.id
  name            = "${local.postgresql_prefix}-${each.value.postgresql_database}-host"
  value           = each.value.host_name
  tags            = merge(var.common_tags, { "source" : "PostgreSQL" })
  content_type    = ""
  expiration_date = timeadd(timestamp(), "8760h")
}
resource "azurerm_key_vault_secret" "db-port" {
  for_each        = { for database in module.databases : database.name => database }
  key_vault_id    = data.azurerm_key_vault.ss_kv.id
  name            = "${local.postgresql_prefix}-${each.value.postgresql_database}-port"
  value           = each.value.postgresql_listen_port
  tags            = merge(var.common_tags, { "source" : "PostgreSQL" })
  content_type    = ""
  expiration_date = timeadd(timestamp(), "8760h")
}
resource "azurerm_key_vault_secret" "db-user" {
  for_each        = { for database in module.databases : database.name => database }
  key_vault_id    = data.azurerm_key_vault.ss_kv.id
  name            = "${local.postgresql_prefix}-${each.value.postgresql_database}-user"
  value           = each.value.user_name
  tags            = merge(var.common_tags, { "source" : "PostgreSQL" })
  content_type    = ""
  expiration_date = timeadd(timestamp(), "8760h")
}
resource "azurerm_key_vault_secret" "db-pwd" {
  for_each        = { for database in module.databases : database.name => database }
  key_vault_id    = data.azurerm_key_vault.ss_kv.id
  name            = "${local.postgresql_prefix}-${each.value.postgresql_database}-pwd"
  value           = each.value.postgresql_password
  tags            = merge(var.common_tags, { "source" : "PostgreSQL" })
  content_type    = ""
  expiration_date = timeadd(timestamp(), "8760h")
}
resource "azurerm_key_vault_secret" "db-name" {
  for_each        = { for database in module.databases : database.name => database }
  key_vault_id    = data.azurerm_key_vault.ss_kv.id
  name            = "${local.postgresql_prefix}-${each.value.postgresql_database}-name"
  value           = each.value.postgresql_database
  tags            = merge(var.common_tags, { "source" : "PostgreSQL" })
  content_type    = ""
  expiration_date = timeadd(timestamp(), "8760h")
}