# Automated build and deployment

The root `Jenkinsfile` runs frontend lint/build, Maven verification, and Docker image builds on each branch. On `main`, image publication and deployment are opt-in build parameters. Deployment pauses for a Jenkins approval, pushes images tagged with the full commit SHA, and uses Ansible to install/update the Compose stack, configure Nginx, and check API/frontend health.

## Jenkins agent requirements

Use a Linux Jenkins agent with Node.js 22, npm, Java 21, Maven 3.9+, Docker Engine/CLI, Docker Compose v2, Ansible Core, and SSH access to the target. Install Jenkins Credentials Binding and SSH Agent plugins. The agent needs permission to use the Docker daemon. Do not put secrets in job parameters, source files, or the pipeline log.

Create these Jenkins credentials:

| Credential ID | Kind | Contents |
|---|---|---|
| `loopcycle-registry` | Username with password | Push/pull credentials for the image registry and namespace. |
| `loopcycle-deploy-env` | Secret file | Production `.env` values: unique `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, and `APP_JWT_SECRET`; optionally initial `ADMIN_EMAIL`/`ADMIN_PASSWORD`. Set `BACKEND_PORT=8080`. Do not include image tags; Ansible pins those to the build commit. |
| `loopcycle-ansible-inventory` | Secret file | Private inventory containing the target under `[web]`, SSH user/host, and required privilege escalation settings. |
| `loopcycle-deploy-ssh` | SSH Username with private key | Key accepted by every host in the inventory; configure passwordless sudo for that user. |

The Jenkins agent must have the target host key in its `known_hosts`; leave host-key checking enabled. The inventory and production environment file are not archived, printed, or committed. The remote environment file is installed as root with mode `0600`.

## Configure and run the pipeline

Create a Jenkins Multibranch Pipeline job for this repository and use the root `Jenkinsfile`; the publish and deploy gates use the multibranch `main` branch condition. Configure SCM discovery and a Git provider webhook to notify Jenkins on pushes/PRs. The job builds and checks feature branches but only publishes/deploys from the `main` branch.

For an ordinary CI build, leave `PUBLISH_IMAGES` and `DEPLOY_ENABLED` unchecked. To deploy, provide a lowercase registry `IMAGE_NAMESPACE`, select both parameters, and approve the deployment when Jenkins pauses. The deploy stage requires the four credentials above. It pins the Compose image names to that build's commit SHA, pulls the images on the host, waits for `/actuator/health` and the frontend, then configures the host Nginx reverse proxy.

Deploy the first revision to a disposable staging host. Configure DNS, firewall rules, TLS termination, database backups, and restore procedures before exposing a production system. The sample Nginx playbook listens on port 80 and does not configure TLS. The database volume persists across Compose updates; `docker compose down -v` is destructive and is not used by the playbook.

## Manual Ansible use

The Docker playbook expects the repository's `compose.yaml` and a private `.env` in the repository root. The web playbook expects Docker services on localhost ports 8080 and 3000.

```bash
ansible-playbook -i ansible/inventory.ini ansible/playbooks/docker.yml --syntax-check
ansible-playbook -i ansible/inventory.ini ansible/playbooks/webserver.yml --syntax-check
ansible-playbook -i ansible/inventory.ini ansible/playbooks/docker.yml \
  -e 'image_registry=docker.io image_namespace=YOUR_NAMESPACE image_tag=COMMIT_SHA'
ansible-playbook -i ansible/inventory.ini ansible/playbooks/webserver.yml
```

The container registry login is read from the Ansible controller's `REGISTRY_USERNAME` and `REGISTRY_PASSWORD` environment variables and is logged out after the deployment block. Use a private inventory and secret store; never put registry passwords in `--extra-vars`.

## What is still external

Jenkins, the registry, a webhook, SSH inventory/keys, production environment secrets, target hosts, DNS, and TLS are not configured by this repository. The pipeline will not publish or deploy unless explicitly enabled, and the deployment stage requires manual approval.
