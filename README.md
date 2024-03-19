# debug-aws-msk-iam-auth

1. In `application.properties`, you need to replace value `app.kafka.bootstrapServers` with your own public IAM authenticated MSK cluster endpoints.
2. In `Makefile`, update:
   * `AWS_ACCESS_KEY_ID` with your own AWS profile
   * `AWS_SECRET_ACCESS_KEY` with your own AWS profile
   * `AWS_REGION` with your own AWS region
3. Do a `make build/native start` or `make build/jvm start`
