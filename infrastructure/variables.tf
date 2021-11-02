
variable "location" {
  type        = string
  default     = "UK South"
  description = "Deployment location"
}
variable "env" {
  type        = string
  description = "Deployment Environment"
}

variable "databases" {
  type        = list(string)
  description = "List of Databases Names to create"
  default     = ["subscriptions"]
}