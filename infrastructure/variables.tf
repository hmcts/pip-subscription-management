variable "product" {
  default = "pip"
}

variable "component" {
  default = "subscription-mangement"
}

variable "location" {
  default = "UK South"
}

variable "env" {
  default = "sbox"
}

variable "subscription" {
  default = "sbox"
}

variable "deployment_namespace" {
  default = ""
}

variable "common_tags" {
  type = map(any)
}


#Databases
variable "databases" {
  type        = list(string)
  description = "List of Databases Names to create"
  default     = ["subscriptions"]
}