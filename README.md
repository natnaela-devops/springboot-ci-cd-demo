# Spring Boot CI/CD GitOps Pipeline with Harbor, ArgoCD, and K3d

A robust local **GitOps CI/CD pipeline** for deploying a Spring Boot application using **GitHub Actions**, **Harbor**, **ArgoCD**, and **K3d**.

This project demonstrates how to securely connect a **local Kubernetes cluster** and a **private container registry** with cloud-hosted automation using **ngrok HTTPS tunneling**.

---

# Architecture Overview

```text
┌────────────────────┐
│ Local Source Code  │
└─────────┬──────────┘
          │ git push
          ▼
┌────────────────────┐
│   GitHub Actions   │
│  Build + Test +    │
│  Docker Packaging  │
└─────────┬──────────┘
          │ docker push
          ▼
┌────────────────────┐
│   Harbor Registry  │
│ (via ngrok tunnel) │
└─────────┬──────────┘
          │ image pull
          ▼
┌────────────────────┐
│      K3d/K3s       │
│ Kubernetes Cluster │
└─────────┬──────────┘
          │ monitored by
          ▼
┌────────────────────┐
│      ArgoCD        │
│ GitOps Deployment  │
└────────────────────┘
```

---

# Workflow Explanation

## 1. Continuous Integration (CI)

When code is pushed to GitHub:

* GitHub Actions:

  * Builds the Spring Boot application using Maven
  * Runs tests
  * Creates a Docker image
  * Pushes the image to Harbor securely through ngrok

---

## 2. Continuous Deployment (CD)

ArgoCD continuously monitors the Kubernetes manifests inside the repository.

Once a new image tag is committed:

* ArgoCD detects drift
* Marks the application as `OutOfSync`
* Automatically synchronizes the cluster
* Performs a rolling update deployment

---

## 3. Secure Private Registry Access

Kubernetes uses `imagePullSecrets` to authenticate against the private Harbor registry exposed through ngrok.

---

# Technology Stack

| Component        | Technology      |
| ---------------- | --------------- |
| Backend          | Spring Boot 3.x |
| Language         | Java 17         |
| Build Tool       | Maven           |
| Containerization | Docker          |
| Registry         | Harbor v2.10    |
| Kubernetes       | K3d / K3s       |
| GitOps           | ArgoCD          |
| CI/CD            | GitHub Actions  |
| Tunnel           | ngrok           |

---

# Project Structure

```text
.
├── .github/
│   └── workflows/
│       └── ci-cd.yml
├── k8s/
│   ├── deployment.yaml
│   └── service.yaml
├── src/
├── Dockerfile
├── pom.xml
└── README.md
```

---

# Prerequisites

Before starting, install:

* Docker
* kubectl
* k3d
* ngrok
* Java 17
* Maven
* ArgoCD CLI (optional)

---

# Step 1 — Create the K3d Cluster

```bash
k3d cluster create local-cluster
```

Verify:

```bash
kubectl get nodes
```

---

# Step 2 — Install Harbor

Run Harbor using Docker Compose.

## Download Harbor

```bash
wget https://github.com/goharbor/harbor/releases/download/v2.10.0/harbor-online-installer-v2.10.0.tgz

tar -xvf harbor-online-installer-v2.10.0.tgz

cd harbor
```

---

## Configure Harbor

Edit `harbor.yml`:

```yaml
hostname: reg.harbor.com

https:
  port: 443
```

---

## Start Harbor

```bash
./prepare
docker compose up -d
```

Verify:

```bash
docker ps
```

---

# Step 3 — Expose Harbor with ngrok

Since GitHub Actions cannot access localhost directly, expose Harbor using ngrok.

Run:

```bash
ngrok http https://localhost:443
```

Example output:

```text
https://abc123.ngrok-free.dev
```

Copy this domain.

---

# Step 4 — Update Harbor Configuration

Update `harbor.yml`:

```yaml
hostname: abc123.ngrok-free.dev
```

Restart Harbor:

```bash
docker compose down
rm -rf common/config/
./prepare
docker compose up -d
```

---

# Step 5 — Configure GitHub Secrets

Go to:

```text
GitHub Repository
→ Settings
→ Secrets and variables
→ Actions
```

Add the following secrets:

| Secret Name     | Description       |
| --------------- | ----------------- |
| HARBOR_REGISTRY | Your ngrok domain |
| DOCKER_USERNAME | Harbor username   |
| DOCKER_PASSWORD | Harbor password   |

Example:

```text
HARBOR_REGISTRY=abc123.ngrok-free.dev
DOCKER_USERNAME=admin
DOCKER_PASSWORD=your_password
```

---

# Step 6 — Create Dockerfile

Create a `Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
```

---

# Step 7 — GitHub Actions Workflow

Create:

```text
.github/workflows/ci-cd.yml
```

```yaml
name: Spring Boot CI/CD

on:
  push:
    branches:
      - main

env:
  IMAGE_TAG: 0.0.2

jobs:
  build-and-push:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Source
        uses: actions/checkout@v4

      - name: Set Up Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Build Application
        run: mvn clean package -DskipTests

      - name: Login to Harbor
        run: |
          docker login ${{ secrets.HARBOR_REGISTRY }} \
            -u ${{ secrets.DOCKER_USERNAME }} \
            -p ${{ secrets.DOCKER_PASSWORD }}

      - name: Build Docker Image
        run: |
          docker build -t \
          ${{ secrets.HARBOR_REGISTRY }}/demo/springboot-app:${IMAGE_TAG} .

      - name: Push Docker Image
        run: |
          docker push \
          ${{ secrets.HARBOR_REGISTRY }}/demo/springboot-app:${IMAGE_TAG}
```

---

# Step 8 — Kubernetes Deployment

Create:

```text
k8s/deployment.yaml
```

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: springboot-app
spec:
  replicas: 1

  selector:
    matchLabels:
      app: springboot-app

  template:
    metadata:
      labels:
        app: springboot-app

    spec:
      imagePullSecrets:
        - name: harbor-registry-secret

      containers:
        - name: springboot-app
          image: abc123.ngrok-free.dev/demo/springboot-app:0.0.2
          imagePullPolicy: Always

          ports:
            - containerPort: 8080
```

---

# Kubernetes Service

Create:

```text
k8s/service.yaml
```

```yaml
apiVersion: v1
kind: Service

metadata:
  name: springboot-service

spec:
  selector:
    app: springboot-app

  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080

  type: LoadBalancer
```

---

# 🔑 Step 9 — Create Harbor Pull Secret

```bash
kubectl create secret docker-registry harbor-registry-secret \
  --docker-server=abc123.ngrok-free.dev \
  --docker-username=admin \
  --docker-password=your_password
```

---

# Step 10 — Install ArgoCD

Create namespace:

```bash
kubectl create namespace argocd
```

Install ArgoCD:

```bash
kubectl apply -n argocd \
-f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

Verify:

```bash
kubectl get pods -n argocd
```

---

# Access ArgoCD UI

Port-forward:

```bash
kubectl port-forward svc/argocd-server -n argocd 8081:443
```

Open:

```text
https://localhost:8081
```

Get admin password:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
-o jsonpath="{.data.password}" | base64 -d
```

Username:

```text
admin
```

---

# Step 11 — Create ArgoCD Application

```bash
argocd app create springboot-app \
  --repo https://github.com/YOUR_USERNAME/YOUR_REPO.git \
  --path k8s \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace default
```

Enable auto-sync:

```bash
argocd app set springboot-app --sync-policy automated
```

---

# Deployment Flow

## Make a Code Change

Update your Spring Boot application.

---

## Bump the Image Version

Update:

* `.github/workflows/ci-cd.yml`
* `k8s/deployment.yaml`

Example:

```yaml
IMAGE_TAG: 0.0.3
```

```yaml
image: abc123.ngrok-free.dev/demo/springboot-app:0.0.3
```

---

## Commit and Push

```bash
git add .

git commit -m "feat: update welcome endpoint"

git push origin main
```

---

# Monitoring

## GitHub Actions

Navigate to:

```text
GitHub → Actions
```

Monitor:

* Maven build
* Docker image creation
* Harbor push

---

## ArgoCD

Monitor:

* Sync status
* Deployment rollout
* Application health

---

# Features

✅ Fully automated CI/CD pipeline

✅ GitOps-driven deployments

✅ Private container registry support

✅ Secure HTTPS tunneling using ngrok

✅ Kubernetes rolling updates

✅ Local-first production-like environment

✅ Lightweight Kubernetes with K3d

---

# Useful Commands

## Check Pods

```bash
kubectl get pods
```

## Check Services

```bash
kubectl get svc
```

## Describe Deployment

```bash
kubectl describe deployment springboot-app
```

## View Logs

```bash
kubectl logs deployment/springboot-app
```

---

# Security Notes

For production environments:

* Use Harbor robot accounts instead of admin credentials
* Replace ngrok with:

  * VPN
  * WireGuard
  * Cloudflare Tunnel
  * Private networking
* Enable TLS certificates
* Use Kubernetes namespaces and RBAC
* Store secrets using:

  * External Secrets
  * Vault
  * Sealed Secrets

---

# Future Improvements

* Helm chart support
* Automatic semantic versioning
* ArgoCD Image Updater
* Prometheus + Grafana monitoring
* Trivy image scanning
* Multi-environment deployments
* Blue/Green deployments

---

# License

MIT License

---

# Acknowledgements

* Kubernetes
* ArgoCD
* Harbor
* K3d
* ngrok
* Spring Boot

---

# Author

Built for learning modern GitOps CI/CD workflows locally with production-inspired tooling.

Happy GitOps hacking 🚀
