import com.cicdenv.jenkins.pipeline.RunEnvironment

import java.security.MessageDigest

def hashedId(String id) {
    def numericId = new BigInteger(1, MessageDigest.getInstance("MD5").digest(id.bytes))
    return numericId.toString(16).padLeft(32, '0')[0..7]
}

def stageTags(String ecrRepo, String hashTag, String buildStage, String defaultBranch) {
    def localJobTag  = "${ecrRepo}:${buildStage}-${hashTag}"
    def remoteJobTag = "${env.AWS_ECR}/${localJobTag}"
    def localDefTag  = "${ecrRepo}:${buildStage}-${defaultBranch}"
    def remoteDefTag = "${env.AWS_ECR}/${localDefTag}"
    return [
        localJob:  localJobTag,
        remoteJob: remoteJobTag,
        localDef:  localDefTag,
        remoteDef: remoteDefTag,
    ]
}

def finalTags(String ecrRepo, String tag, String defaultBranch) {
    def localJobTag  = "${ecrRepo}:${tag}"
    def remoteJobTag = "${env.AWS_ECR}/${localJobTag}"
    def localDefTag  = "${ecrRepo}:${defaultBranch}"
    def remoteDefTag = "${env.AWS_ECR}/${localDefTag}"
    return [
        localJob:  localJobTag,
        remoteJob: remoteJobTag,
        localDef:  localDefTag,
        remoteDef: remoteDefTag,
    ]
}

/**
 * Docker build with ECR backed, multistage aware buildkit caching.
 *
 * @param params - map of args [
 *   ecrRepo (Required)       - AWS ECR Repo short name,
 *   dockerFile (Optional)    - defaults to 'Dockerfile',
 *   context (Optional)       - defaults to '.',
 *   buildArgs (Optional)     - extra --build-arg values,
 *   tag (Optional)           - defaults to hashed 'acct/instance/job',
 *   buildStages (Optional)   - list of non final build stages - defaults to all 'named' non-final stages,
 *   defaultBranch (Optional) - 'master',
 * ]
 */
def call(Map params) {
    // Unique /acct/instance/job tag
    def hashTag = hashedId("/${env.AWS_ACCOUNT_NAME}/${env.JENKINS_INSTANCE}/${env.JOB_NAME}")

    // defaults
    def dockerFile = params.dockerFile ?: 'Dockerfile'
    def context = params.context ?: '.'
    def buildArgs = params.buildArgs ? '--build-arg ' + params.buildArgs.join(' --build-arg ') : ''
    def tag = params.tag ?: hashTag
    def buildStages = params.buildStages ?: []
    if (buildStages.empty && params.buildStages == null) {
        int hasStages = sh(script: "grep 'FROM .* as ' '${dockerFile}'", returnStatus: true)
        if (hasStages == 0) {
            buildStages = sh(// If not naming stages use explicit 'buildStages'
                script: $/grep 'FROM .* as ' '${dockerFile}' | sed -E -e 's/FROM .* as (.*)/\1/'/$,
                returnStdout: true
            ).trim().split('\n')
        }
    }
    def defaultBranch = params.defaultBranch ?: 'master'

    // required
    def ecrRepo = params.ecrRepo

    // Intermediate build stages: Pull/Build/Push incrementally
    Set<String> cacheFrom = []
    for (buildStage in buildStages) {
        Map tags = stageTags(ecrRepo, hashTag, buildStage, defaultBranch)

        int pulled = sh(script: "docker pull '${tags.remoteJob}' && docker tag '${tags.remoteJob}' '${tags.localJob}'", returnStatus: true)
        if (pulled != 0) {
            pulled = sh(script: """
                docker pull '${tags.remoteDef}'
             && docker tag '${tags.remoteDef}' '${tags.localDef}'
             && docker tag '${tags.remoteDef}' '${tags.remoteJob}'
             && docker tag '${tags.localDef}'  '${tags.localJob}'
            """, returnStatus: true)
        }
        if (pulled == 0) {
             cacheFrom << "--cache-from '${tags.localJob}'"
        }
        
        sh """
        DOCKER_BUILDKIT=1 \
        docker build \
            --build-arg BUILDKIT_INLINE_CACHE=1 \
            ${buildArgs} ${cacheFrom ? cacheFrom.join(' '): ''} \
            --target "${buildStage}" \
            --tag '${tags.localJob}' \
            --tag '${tags.remoteJob}' \
            --file ${dockerFile} ${context}
        """
        cacheFrom << "--cache-from '${tags.localJob}'"
        sh "docker push '${tags.remoteJob}'"
        if (env.BRANCH_NAME == defaultBranch) {
            sh """
                docker tag '${tags.localJob}'  '${tags.localDef}'
                docker tag '${tags.remoteJob}' '${tags.remoteDef}'
                docker push '${tags.remoteDef}'
            """
        }
    }

    // Final image: Pull/Build/Push
    Map tags = finalTags(ecrRepo, tag, defaultBranch)

    int pulled = sh(script: "docker pull '${tags.remoteJob}' && docker tag '${tags.remoteJob}' '${tags.localJob}'", returnStatus: true)
    if (pulled != 0) {
        pulled = sh(script: """
            docker pull '${tags.remoteDef}'
         && docker tag '${tags.remoteDef}' '${tags.localDef}'
         && docker tag '${tags.remoteDef}' '${tags.remoteJob}'
         && docker tag '${tags.localDef}'  '${tags.localJob}'
        """, returnStatus: true)
    }
    if (pulled == 0) {
         cacheFrom << "--cache-from '${tags.localJob}'"
    }

    sh """
    DOCKER_BUILDKIT=1 \
    docker build \
        --build-arg BUILDKIT_INLINE_CACHE=1 \
        ${buildArgs} ${cacheFrom ? cacheFrom.join(' '): ''} \
        --tag '${tags.localJob}' \
        --tag '${tags.remoteJob}' \
        --file ${dockerFile} ${context}
    """
    sh "docker push '${tags.remoteJob}'"
    if (env.BRANCH_NAME == defaultBranch) {
        sh """
            docker tag '${tags.localJob}'  '${tags.localDef}'
            docker tag '${tags.remoteJob}' '${tags.remoteDef}'
            docker push '${tags.remoteDef}'
        """
    }
}
