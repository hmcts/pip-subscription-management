locals {
  prefix              = "${var.product}-ss"
  prefix_no_special   = replace(local.prefix, "-", "")
  resource_group_name = "${local.prefix}-${var.env}-rg"
  key_vault_name      = "${local.prefix}-kv-${var.env}"
  env_long_name       = var.env == "sbox" ? "sandbox" : var.env == "stg" ? "staging" : var.env
  sdp_key_vault_name  = "${local.prefix}-sdp-kv-${var.env}"
}
data "azurerm_client_config" "current" {}


data "azurerm_subnet" "iaas" {
  name                 = "iaas"
  resource_group_name  = "ss-${var.env}-network-rg"
  virtual_network_name = "ss-${var.env}-vnet"
}

data "azurerm_key_vault" "kv" {
  name                = local.key_vault_name
  resource_group_name = local.resource_group_name
}

data "azurerm_key_vault" "sdp-kv" {
  name                = local.sdp_key_vault_name
  resource_group_name = local.resource_group_name
}

data "azurerm_key_vault_secret" "sdp-user" {
  name         = "subscription-management-SDP-USER"
  key_vault_id = data.azurerm_key_vault.sdp-kv.id
}

data "azurerm_key_vault_secret" "sdp-pass" {
  name         = "subscription-management-SDP-PASS"
  key_vault_id = data.azurerm_key_vault.sdp-kv.id
}
