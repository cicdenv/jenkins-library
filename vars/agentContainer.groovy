import com.cicdenv.jenkins.pipeline.RunEnvironment
import com.cicdenv.jenkins.pipeline.HostEnvironment
import com.cicdenv.jenkins.pipeline.ContainerEnvironment

import java.security.MessageDigest

def shortChecksum(String s) {
    return new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(s.bytes))
        .toString(/*Number Base*/16).padLeft(32, '0')[0..7]
}

def ensureImage(String dockerFile) {
    // Pull over installs
    def installers = ""
    for (packager in ['apt', 'dnf', 'yum', 'apk', 'pacman', 'static']) {
        def installs = libraryResource("files/${packager}-installs.sh")
        writeFile(file: "${packager}-installs.sh", text: installs)
        installers += installs
    }
    sh 'chmod +x *.sh'
    
    // Checksum the complete Dockerfile + installs
    def dockerFileContent = readFile(dockerFile)
    def shortChecksum = shortChecksum(dockerFileContent + installers)

    // Ensures the tooling container is available locally.
    // If the agent container isn't available in the local image cache
    //   Try pulling from AWS ECR, build only if needed (pushing to ECR afterwards)
    def imageTag = "${env.AWS_MAIN_ACCOUNT_ID}.dkr.ecr.${env.AWS_DEFAULT_REGION}.amazonaws.com/ci-builds:${shortChecksum}"
    sh """
set -eux -o pipefail
if [[ ! `docker images --filter=reference="${imageTag}" -q | wc -l` == "1" ]]; then
    if ! docker pull "${imageTag}"; then
        docker build -t "${imageTag}" --file ${dockerFile} .
        docker push "${imageTag}"
    fi
fi
"""
    return imageTag
}

def agentSettings(String imageTag, Map bindings = [:]) {
    settings = [
        image: imageTag,
        nodeLabel: ContainerEnvironment.label,
        dockerRunArgs: [
            '--init',
            '--network jenkins',
            "--volume ${HostEnvironment.homeDir}/.aws/config:${HostEnvironment.homeDir}/.aws/config",
            "--volume ${HostEnvironment.homeDir}/.docker/config.json:${HostEnvironment.homeDir}/.docker/config.json",
            "--volume ${HostEnvironment.homeDir}/.ssh/config:${HostEnvironment.homeDir}/.ssh/config",
            '--group-add docker',
        ].join(' '),
        customWorkspace: HostEnvironment.workspaceDir,
        bindings: bindings,
    ]
    settings << bindings
    return settings
}

def fromRepo(Map args) {
    runEnv this

    // Required args
    String dockerFile = args.dockerFile

    // Optional args
    String ref = args.ref ?: RunEnvironment.ref
    String repoSpec = args.repoSpec ?: "${RunEnvironment.organization}/${RunEnvironment.repository}"
    List repoParts = repoSpec.split('/')
    String org = repoParts[0]
    String repo = repoParts[1]
    
    String imageTag
    node(ContainerEnvironment.label) {
        stage("agent container ${dockerFile}") {
            // Pre-fetch agent container Dockerfile
            def baseDockerFile = githubFile(file: dockerFile, org: org, repo: repo, ref: ref)

            // "Extend" a generic CI tools Dockefile with Jenkins service user requirements
            writeFile(file: 'Dockerfile-jenkins.ci', text: readFile(baseDockerFile) \
                + new groovy.text.SimpleTemplateEngine()
                    .createTemplate(libraryResource('templates/Dockerfile-extensions.tmpl'))
                    .make(HostEnvironment.serviceSettings)
                    .toString())

            // Build/Update the "tools" container image as needed
            imageTag = ensureImage('Dockerfile-jenkins.ci')
        }
    }

    // Agent directive defaults
    return agentSettings(imageTag)
}

def fromImage(Map args) {
    runEnv this

    // Required args
    String image = args.image

    node(ContainerEnvironment.label) {
        stage("agent container ${image}") {
            // "Extend" the base image with Jenkins service user requirements
            writeFile(file: 'Dockerfile-jenkins.ci', text: """
FROM ${image}

""" + new groovy.text.SimpleTemplateEngine()
        .createTemplate(libraryResource('templates/Dockerfile-extensions.tmpl'))
        .make(HostEnvironment.serviceSettings)
        .toString())

            // Build/Update the "tools" container image as needed
            imageTag = ensureImage('Dockerfile-jenkins.ci')
        }
    }

    // Agent directive defaults
    return agentSettings(imageTag)
}

def fromTemplate(Map args) {
    runEnv this

    // Required args
    String template = args.template
    Map repoBindings = args.repoBindings

    // Optional args
    String ref = args.ref ?: RunEnvironment.ref
    String repoSpec = args.repoSpec ?: "${RunEnvironment.organization}/${RunEnvironment.repository}"
    List repoParts = repoSpec.split('/')
    String org = repoParts[0]
    String repo = repoParts[1]

    String renderedContent
    String checksum
    Map bindings = [:]
    String imageTag
    node(ContainerEnvironment.label) {
        stage("agent container pre-fetches") {
            repoBindings.each { var, file ->
                githubFile(file: file, org: org, repo: repo, ref: ref)
                bindings[var] = readFile(file).replaceAll("\\s", "")
            }
            renderedContent = new groovy.text.SimpleTemplateEngine()
                                  .createTemplate(template)
                                  .make(bindings)
                                  .toString()
            checksum = shortChecksum(renderedContent)
        }
        stage("agent container ${checksum}") {
            // "Extend" the Dockefile content with Jenkins service user requirements
            writeFile(file: 'Dockerfile-jenkins.ci', text: renderedContent \
                + new groovy.text.SimpleTemplateEngine()
                    .createTemplate(libraryResource('templates/Dockerfile-extensions.tmpl'))
                    .make(HostEnvironment.serviceSettings)
                    .toString())

            // Build/Update the "tools" container image as needed
            imageTag = ensureImage('Dockerfile-jenkins.ci')
        }
    }

    // Agent directive defaults
    return agentSettings(imageTag, bindings)
}

def from(Map args) {
    runEnv this

    // Required args
    String content = args.content
    String checksum = shortChecksum(content)

    String imageTag
    node(ContainerEnvironment.label) {
        stage("agent container ${checksum}") {
            // "Extend" the Dockefile content with Jenkins service user requirements
            writeFile(file: 'Dockerfile-jenkins.ci', text: content \
                + new groovy.text.SimpleTemplateEngine()
                    .createTemplate(libraryResource('templates/Dockerfile-extensions.tmpl'))
                    .make(HostEnvironment.serviceSettings)
                    .toString())

            // Build/Update the "tools" container image as needed
            imageTag = ensureImage('Dockerfile-jenkins.ci')
        }
    }

    // Agent directive defaults
    return agentSettings(imageTag)
}

/**
 * Retrieves the contents of a "generic" CI Dockerfile from a git repo,
 * "extending" it with Jenkins service user / local host+docker specific items.
 *
 * This is "assumed" to be the job "current" repo if this is a
 * multibranch pipeline job.
 *
 * NOTE: for multibranch pipeline job this ALWAYS fetches the PR+merge for PRs.
 *
 * @param dockerFile - relative path within the above repo to a CI Dockerfile
 * @return Map of declarative agent settings [image, label, args, workspace]
 */
def fromRepo(String dockerFile) {
    fromRepo(dockerFile: dockerFile)
}

/**
 * Retrieves the contents of a "generic" CI Dockerfile from a git repo,
 * "extending" it with Jenkins service user / local host+docker specific items.
 *
 * This is "assumed" to be the job "current" repo if this is a
 * multibranch pipeline job.
 *
 * NOTE: for multibranch pipeline job this ALWAYS fetches the PR+merge for PRs.
 *
 * @param repoSpec - must be in the form '{organization}:{repo}'
 * @param dockerFile - relative path within the above repo to a CI Dockerfile
 * @return Map of declarative agent settings [image, label, args, workspace]
 */
def fromRepo(String repoSpec, String dockerFile) {
    fromRepo(repoSpec: repoSpec, dockerFile: dockerFile)
}

/**
 * "Extends" an existing docker image with Jenkins service user / local docker specific items.
 *
 * @param image - Docker image name[:tag]
 * @return Map of declarative agent settings [image, label, args, workspace]
 */
def fromImage(String image) {
    fromImage(image: image)
}

/**
 * "Extends" an inline Dockerfile with Jenkins service user / local docker specific items.
 *
 * @param content - Dockerfile content in the form of a triple quoted string
 * @return Map of declarative agent settings [image, label, args, workspace]
 */
def from(String content) {
    from(content: content)
}

/**
 * "Extends" an inline Dockerfile with Jenkins service user / local docker specific items.
 *
 * @param template - Dockerfile content in the form of a triple quoted string
 * @param repoBindings - map of variable name to repo files
 * @return Map of declarative agent settings [image, label, args, workspace]
 */
def from(String template, Map repoBindings) {
    fromTemplate(template: template, repoBindings: repoBindings)
}
