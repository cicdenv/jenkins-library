import com.cicdenv.jenkins.pipeline.RunEnvironment

/**
 * Retrieves the contents of a file from Github using their REST API.
 *
 * @param params - map of args [
 *   file (Required),
 *   outputFile (Optional) - ${file},
 *   org (Optional) - defaults to job top-level folder,
 *   repo (Optional) - ${parent},
 *   ref (Optional) - 'master',
 * ]
 */
def call(Map params) {
    runEnv this

    def file = params.file // Required
    def outputFile = params.outputFile ? params.outputFile : new File(file).name // Optional

    // Optional, defaults to ${file}
    if (outputFile.contains('/')) {
        def subFolder =  new File(outputFile).absoluteFile.parentFile.name
        sh "mkdir -p ${subFolder}"
    }

    // Optional, defaults to global Jenkins default github org
    def org
    if (params.org) {
        org = params.org
    } else {
        org = RunEnvironment.organization
    }

    // Optional, sniffs the current jobs item-name to determine github repo
    def repo
    if (params.repo) {
        repo = params.repo
    } else {
        repo = RunEnvironment.repository
    }

    // Optional, defaults to current github repo branch head
    def gitRef
    if (params.ref) {
       gitRef = params.ref // Explicitly specified
    } else { // Use current branch|PR ref
       gitRef = env.CHANGE_ID ? env.CHANGE_BRANCH : env.BRANCH_NAME
    }

    withCredentials([usernamePassword(credentialsId: 'github-jenkins-token', usernameVariable: 'username', passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
        sh """
            curl -s \
             -H 'Authorization: token ${GITHUB_ACCESS_TOKEN}' \
             -H 'Accept: application/vnd.github.v4.raw' \
             -o ${outputFile} \
             -L 'https://api.github.com/repos/${org}/${repo}/contents/${file}?ref=${gitRef}'
        """
    }
    
    return outputFile;
}
