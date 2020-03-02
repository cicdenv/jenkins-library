import com.cicdenv.jenkins.pipeline.RunEnvironment
import com.cicdenv.jenkins.pipeline.HostEnvironment
import com.cicdenv.jenkins.pipeline.ContainerEnvironment

/**
 * Provides git repo Jenkinsfile and standalone pipeline jobs 
 * needed reflective and build environment specific paths and settings.
 *
 * Initializes exactly once before use during a given job run.
 */

/**
 * This is the primary shared library initializer.
 *
 * Usage: `runEnv this`
 * 
 * @param script the running pipeline
 */
def call(script) {
    if (RunEnvironment.configured) {
        return
    }
    RunEnvironment.configured = true

    // Print primodial PipelineScript run environment
    echo env.getEnvironment().toString().replace(',', '\n')

    // Determine If its a multibranch pipeline job
    if (env.CHANGE_ID) { // PR multibranch pipeline job
        RunEnvironment.ref = "pull/${CHANGE_ID}/merge"
    } else if (env.BRANCH_NAME) { // Branch multibranch pipeline job
        RunEnvironment.ref = BRANCH_NAME
    } else { // pipeline job
        RunEnvironment.ref = 'master'
    }

    // Parse / Decode job name to 'guess' github org, repo, child (branch|PR)
    def parts = JOB_NAME.split('/')
    parts = parts.collect { part -> URLDecoder.decode(part)}
    def organization = parts[0]
    def repository = parts.size() > 1 ? parts[-2] : 'job'
    def childName = parts[-1]

    RunEnvironment.organization = organization
    RunEnvironment.repository = repository
    RunEnvironment.childName = childName

    // Normalize the job childname to something safe for use as a docker image name
    RunEnvironment.safeChildName = childName.replaceAll(/[^\w.-]/, '_')

    // Unique per build workspace folder
    HostEnvironment.workspacesRoot = "${HostEnvironment.homeDir}/workspace"
    HostEnvironment.workspaceDir = "${HostEnvironment.workspacesRoot}/${repository}-${RunEnvironment.safeChildName}-${BUILD_NUMBER}"

    // Jenkins agent node label
    ContainerEnvironment.label = ''

    RunEnvironment.serverUrl = new URI(JENKINS_URL).host

    RunEnvironment.cacheS3bucket = "jenkins-builds-${env.AWS_ACCOUNT_NAME}-cicdenv-com"
    RunEnvironment.cacheS3SubDir = "shared/cache"
}
