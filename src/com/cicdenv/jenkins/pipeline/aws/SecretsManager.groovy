package com.cicdenv.jenkins.pipeline.aws

import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import com.amazonaws.services.secretsmanager.AWSSecretsManager
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult

import groovy.json.JsonSlurper

/**
 * AWS Secrets Store access logic.
 *
 * NOTE: secrets created from the AWS CLI differ from the 
 *       JSON abominations created from the AWS Console web UI.
 */
class SecretsManager {

    /**
     * Accesses the secret key(s)/value(s) given the secret name|arn.
     * 
     * @param secretId can be a short name or an secret ARN
     *
     * @return secretValue as is or parsed JSON if created in the UI
     */
    @NonCPS
    static def getSecretValue(String secretId) {
        AWSSecretsManager client = AWSSecretsManagerClientBuilder.defaultClient()

        GetSecretValueRequest request = new GetSecretValueRequest().withSecretId(secretId)
        GetSecretValueResult response = client.getSecretValue(request)
        String raw = response.secretString
        try {
            return new JsonSlurper().parseText(raw) // JSON string => Map
        } catch (all) {
            return [value: raw] // Not JSON
        }
    }
}
