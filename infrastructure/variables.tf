variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "deployment_namespace" {}

variable "common_tags" {
  type = map(any)
}


#Databases
variable "databases" {
  type        = list(string)
  description = "List of Databases Names to create"
  default     = ["subscriptions"]
}