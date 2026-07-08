resource "azurerm_container_app" "auth" {
  name                         = "puckzone-auth"
  resource_group_name          = data.terraform_remote_state.base.outputs.resource_group_name
  container_app_environment_id = data.terraform_remote_state.base.outputs.container_app_environment_id
  revision_mode                = "Single"

  # Credenciales sensibles como secrets de la Container App (no aparecen en
  # texto plano en el portal ni en `az containerapp show`).
  secret {
    name  = "db-password"
    value = data.terraform_remote_state.base.outputs.postgres_admin_password
  }
  secret {
    name  = "jwt-secret"
    value = data.terraform_remote_state.base.outputs.jwt_secret
  }

  template {
    # Auth es stateless (todo vive en Postgres): podria escalar sin problema,
    # pero con 1 replica alcanza para la carga del proyecto.
    min_replicas = 1
    max_replicas = 1

    container {
      # 0.5/1Gi como game: con 0.25 vCPU el arranque de Spring supera los ~30s
      # y la liveness probe mataba el contenedor antes de abrir el puerto.
      name   = "auth"
      image  = var.image
      cpu    = 0.5
      memory = "1Gi"

      # Postgres compartido de infra/base, database auth_db.
      # Azure Flexible Server exige TLS, por eso sslmode=require.
      env {
        name  = "DB_URL"
        value = "jdbc:postgresql://${data.terraform_remote_state.base.outputs.postgres_fqdn}:5432/auth_db?sslmode=require"
      }
      env {
        name  = "DB_USERNAME"
        value = data.terraform_remote_state.base.outputs.postgres_admin_login
      }
      env {
        name        = "DB_PASSWORD"
        secret_name = "db-password"
      }
      # auth FIRMA los JWT con el secreto de produccion compartido (infra/base);
      # gateway y los demas servicios validan con el mismo valor.
      env {
        name        = "PUCKZONE_JWT_SECRET"
        secret_name = "jwt-secret"
      }
      env {
        name  = "APPLICATIONINSIGHTS_CONNECTION_STRING"
        value = data.terraform_remote_state.base.outputs.application_insights_connection_string
      }

      liveness_probe {
        transport = "HTTP"
        port      = 8081
        path      = "/actuator/health/liveness"
        # Margen para el arranque de Spring; sin esto la probe empieza a fallar
        # de inmediato y ACA reinicia el contenedor en bucle.
        initial_delay = 20
      }
      readiness_probe {
        transport = "HTTP"
        port      = 8081
        path      = "/actuator/health/readiness"
        initial_delay = 10
      }
    }
  }

  # Interno: solo el gateway (en el mismo environment) le habla; nada de internet.
  ingress {
    external_enabled = false
    target_port      = 8081
    transport        = "auto"

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  lifecycle {
    # El pipeline actualiza la imagen con az containerapp update; sin esto,
    # cada terraform apply intentaria devolver la app a la imagen inicial.
    ignore_changes = [template[0].container[0].image]
  }
}
