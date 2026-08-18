# Optional Harbor integration

The default GitHub workflow publishes to GHCR because GitHub-hosted runners can access it without exposing a private network service. Harbor is useful for local and private-network practice, but its connectivity and trust model must be explicit.

## Recommended lab patterns

1. Run Harbor on a VM or host reachable from the Kubernetes nodes.
2. Use TLS with a certificate trusted by Docker, containerd, the runner, and cluster nodes.
3. Create a dedicated robot account with push access only to the lab project.
4. Store credentials in GitHub Actions secrets or the self-hosted runner's approved secret store.
5. Use a self-hosted runner on the same trusted network when Harbor is private.
6. Configure an `imagePullSecret` or an external secret controller for Kubernetes image pulls.

Do not expose an administrative Harbor account through an ad-hoc public tunnel. Do not commit passwords, robot tokens, certificates, or generated pull secrets.

## Optional workflow values

Define these only for a deliberately configured Harbor publishing job:

- `HARBOR_REGISTRY`: trusted registry hostname
- `HARBOR_USERNAME`: robot-account username
- `HARBOR_PASSWORD`: robot-account token

The base workflow intentionally does not assume those secrets exist.
