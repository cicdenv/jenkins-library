import com.cicdenv.jenkins.pipeline.RunEnvironment

import java.security.MessageDigest

def call(Map params) {
    // Unique /acct/instance/job tag
    def hashString = "/${env.AWS_ACCOUNT_NAME}/${env.JENKINS_INSTANCE}/${env.JOB_NAME}"
    def hashTag = new BigInteger(1, MessageDigest.getInstance("MD5").digest(hashString.bytes)).toString(16).padLeft(32, '0')[0..7]

    // defaults
    def dockerFile = params.dockerFile ?: 'Dockerfile'
    def context = params.context ?: '.'
    def buildArgs = params.buildArgs ? '--build-arg ' + params.buildArgs.join(' --build-arg ') : ''
    def tag = params.tag ?: hashTag

    // required
    def ecrRepo = params.ecrRepo
    def buildStages = params.buildStages ?: sh(
       script: $/grep 'FROM .* as ' '${dockerFile}' | sed -E -e 's/FROM .* as (.*)/\1/'/$,
       returnStdout: true
    ).trim().split('\n') // If not naming stages use explicit 'buildStages'

    // Intermediate build stages: Pull/Build/Push incrementally
    Set<String> cacheFrom = []
    for (buildStage in buildStages) {
        def localTag = "${ecrRepo}:${buildStage}-${hashTag}"
        def remoteTag = "${env.AWS_ECR}/${localTag}"

        int pulled = sh(script: "docker pull '${remoteTag}' && docker tag '${remoteTag}' '${localTag}'", returnStatus: true)
        if (pulled == 0) {
             cacheFrom << "--cache-from '${localTag}'"
        }
        
        sh """
        DOCKER_BUILDKIT=1 \
        docker build \
            --build-arg BUILDKIT_INLINE_CACHE=1 \
            ${buildArgs} ${cacheFrom ? cacheFrom.join(' '): ''} \
            --target "${buildStage}" \
            --tag '${localTag}' \
            --tag '${remoteTag}' \
            --file ${dockerFile} ${context}
        """
        cacheFrom << "--cache-from '${localTag}'"
        sh "docker push '${remoteTag}'"
    }

    // Final image: Pull/Build/Push
    def localTag = "${ecrRepo}:${tag}"
    def remoteTag = "${env.AWS_ECR}/${localTag}"

    int pulled = sh(script: "docker pull '${remoteTag}' && docker tag '${remoteTag}' '${localTag}'", returnStatus: true)
    if (pulled == 0) {
         cacheFrom << "--cache-from '${localTag}'"
    }
    
    sh """
    DOCKER_BUILDKIT=1 \
    docker build \
        --build-arg BUILDKIT_INLINE_CACHE=1 \
        ${buildArgs} ${cacheFrom ? cacheFrom.join(' '): ''} \
        --tag '${localTag}' \
        --tag '${remoteTag}' \
        --file ${dockerFile} ${context}
    """
    sh "docker push '${remoteTag}'"
}
