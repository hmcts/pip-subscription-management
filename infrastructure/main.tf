locals {
  product                  = "pip"
  component                = "subecription-mgmt"
  builtFrom                = "hmcts/jenkins/subscription-management"
  common_tags              = module.ctags.common_tags
  resource_group_name      = "pip-sharedinfra-${var.env}-rg"
  storage_account_name     = "pipsharedinfrasa${var.env}"
  dtu_storage_account_name = "pipdtu${var.env}"
  team_name                = "PIP DevOps"
  team_contact             = "#vh-devops"
  env_long_name            = var.env == "sbox" ? "sandbox" : var.env == "stg" ? "staging" : var.env
  postgresql_user          = "pipdbadmin"
  postgresql_prefix        = "postgre"
}

module "ctags" {
  source      = "git::https://github.com/hmcts/terraform-module-common-tags.git?ref=master"
  environment = var.env
  product     = local.product
  builtFrom   = local.builtFrom
}

data "azurerm_subnet" "postgres" {
  name                 = "iaas"
  resource_group_name  = "ss-${var.env}-network-rg"
  virtual_network_name = "ss-${var.env}-vnet"
}

module "databases" {
  for_each           = { for database in var.databases : database => database }
  source             = "git::https://github.com/hmcts/cnp-module-postgres.git?ref=postgresql_tf"
  product            = local.product
  component          = local.component
  subnet_id          = data.azurerm_subnet.postgres.id
  location           = var.location
  env                = local.env_long_name
  postgresql_user    = local.postgresql_user
  database_name      = each.value
  common_tags        = local.common_tags
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
module "keyvault_postgre_secrets" {
  for_each = { for database in module.databases : database.name => database }
  source   = "./modules/key-vault/secret"

  key_vault_id = data.azurerm_key_vault.ss_kv.id
  tags         = local.common_tags
  secrets = [
    {
      name  = "${local.postgresql_prefix}-${each.value.postgresql_database}-host"
      value = each.value.host_name
      tags = {
        "source" : "PostgreSQL"
      }
      content_type = ""
    },
    {
      name  = "${local.postgresql_prefix}-${each.value.postgresql_database}-port"
      value = each.value.postgresql_listen_port
      tags = {
        "source" : "PostgreSQL"
      }
      content_type = ""
    },
    {
      name  = "${local.postgresql_prefix}-${each.value.postgresql_database}-user"
      value = each.value.user_name
      tags = {
        "source" : "PostgreSQL"
      }
      content_type = ""
    },
    {
      name  = "${local.postgresql_prefix}-${each.value.postgresql_database}-pwd"
      value = each.value.postgresql_password
      tags = {
        "source" : "PostgreSQL"
      }
      content_type = ""
    },
    {
      name  = "${local.postgresql_prefix}-${each.value.postgresql_database}-name"
      value = each.value.postgresql_database
      tags = {
        "source" : "PostgreSQL"
      }
      content_type = ""
    }
  ]
}