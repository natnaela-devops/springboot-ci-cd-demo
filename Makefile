.PHONY: test package image manifests validate

test:
	./mvnw --batch-mode --no-transfer-progress test

package:
	./mvnw --batch-mode --no-transfer-progress verify

image:
	docker build --tag springboot-gitops-demo:local .

manifests:
	kubectl kustomize k8s

validate: package image manifests
