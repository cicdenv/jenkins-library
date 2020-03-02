import com.cicdenv.jenkins.pipeline.RunEnvironment
import com.cicdenv.jenkins.pipeline.HostEnvironment

import java.security.MessageDigest

/**
 * Jenkins build agent caching system.
 *
 * @param params keys (list of 'input' files), values, folders or files (list of 'output dirs or files')
 * @param body block of steps to run
 */
def call(Map params, Closure body) {
    List sortedKeys = params.keys.clone().sort()
    def hashedKeys = []
    for (int i = 0; i < sortedKeys.size(); i++) {
        def fileContent = readFile(sortedKeys[i])

        hashedKeys[i] = new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(fileContent.bytes)).toString(16).padLeft(32, '0')[0..7]
    }
    def inputKeys = hashedKeys.join('-')

    List outputs = [ // Accept folders, files, values
        params.folders ?: [],
        params.files   ?: [],
        params.values  ?: [],
    ].flatten()
    List sortedItems = outputs.clone().sort()
    def hashedItems = []
    for (int i = 0; i < sortedItems.size(); i++) {
        def itemName = sortedItems[i]

        hashedItems[i] = new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(itemName.bytes)).toString(16).padLeft(32, '0')[0..7]
    }
    def outputKeys = hashedItems.join('-')

    def cacheKey = "${inputKeys}_${outputKeys}"
    def archiveFile = "${cacheKey}.tar"

    def cacheRoot = "${HostEnvironment.cacheDir}/${RunEnvironment.repository}"
    def cachedOutputs = "${cacheRoot}/${archiveFile}"

    def s3Bucket = RunEnvironment.cacheS3bucket
    def s3Object = "${RunEnvironment.cacheS3SubDir}/${RunEnvironment.repository}/${archiveFile}"
    def s3Url = "s3://${s3Bucket}/${s3Object}"

    def cacheHit = fileExists(cachedOutputs)
    if (cacheHit) {
        // Expand the cached archive to the workspace
        echo "Cache hit: '${cachedOutputs}'"
        sh "tar -xf '${cachedOutputs}'"
    } else {
        echo "Cache miss: '${cachedOutputs}'"

        // Pull from s3 for exact match
        def available = sh(script: "aws s3api head-object --bucket '${s3Bucket}' --key '${s3Object}'", returnStatus:true)
        echo "Upstream cache has it? -> ${available}"
        if (available == 0) { // Pull archive from s3
            sh "aws s3 cp '${s3Url}' '${cachedOutputs}'"

            // Expand the cached archive to the workspace
            echo "Cache fill: '${cachedOutputs}'"
            sh "tar -xf '${cachedOutputs}'"
        }
    }
    
    if (cacheHit && params.fullyCacheable) { // Don't run body steps if "fully cacheable" specified
        return
    }

    body()

    if (!fileExists(cachedOutputs)) {
        lock(cachedOutputs) {
            if (!fileExists(cachedOutputs)) {
                // Archive the new files in the workspace to the cache
                echo "Caching: keys=${params.keys}, outputs=${outputs}, location=${cachedOutputs} ..."
                sh "mkdir -p ${cacheRoot}; tar -cf '${cachedOutputs}' ${outputs.join(' ')}"

                def available = sh(script: "aws s3api head-object --bucket '${s3Bucket}' --key '${s3Object}'", returnStatus:true)
                echo "Upstream cache has it? -> ${available}"
                if (available != 0) { // Push archive to s3
                    sh "aws s3 cp '${cachedOutputs}' '${s3Url}'"
                }
            }
        }
    }
}
