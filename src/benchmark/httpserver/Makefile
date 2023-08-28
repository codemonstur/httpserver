
.PHONY: help build benchmark

NAME=`xmllint --xpath "project/artifactId/text()" pom.xml`

help:
	@echo "[INFO] The following make targets are available:"
	@echo "[INFO]   - help\t\t:\tYou are reading it now"
	@echo "[INFO]   - build\t\t:\tBuilds the code and packages into a jar"
	@echo "[INFO]   - benchmark\t\t:\tRuns the wrk benchmark tool against localhost:8080"

build:
	@mvn clean package

benchmark:
	@wrk --version
	@wrk http://localhost:8080/