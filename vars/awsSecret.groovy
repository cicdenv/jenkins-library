import com.cicdenv.jenkins.pipeline.aws.SecretsManager

/**
 * AWS Secrets Manager get secret values global.
 *
 * @param secretName AWS secrets manager (specific secret name [id])
 *
 * @return Groovy map with secret key/value pairs
 */
def call(String secretName) {
    // ARN to secret in main account
    String secretId = "arn:aws:secretsmanager:${env.AWS_DEFAULT_REGION}:${env.AWS_MAIN_ACCOUNT_ID}:secret:${secretName}"
    return SecretsManager.getSecretValue(secretId)
}
