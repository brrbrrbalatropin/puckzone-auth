output "auth_internal_fqdn" {
  description = "FQDN interno de auth; el gateway lo usa como AUTH_SERVICE_URL"
  value       = "https://${azurerm_container_app.auth.name}.internal.${data.terraform_remote_state.base.outputs.container_app_environment_default_domain}"
}
