import java.io.File

import jenkins.model.Jenkins
import org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy

/**
 * Multibranch Pipeline job config "Origin PR Discovery Trait" checkout strategy.
 *
 * @returns - one of {MERGE,HEAD,UNKNOWN}
 */
def call () {
    def projectItem = new File(env.JOB_NAME).parent
    def projectConfig = Jenkins.instance.getItemByFullName(projectItem)

    List branchSourceTraits = projectConfig.sources.source.traits[0]

    String strategyValue
    branchSourceTraits.each { t -> 
        if (t instanceof OriginPullRequestDiscoveryTrait) {
            String strategy
            switch (t.strategyId) {
                case 1: // Default - algorithmic merge into target branch
                    strategyValue = ChangeRequestCheckoutStrategy.MERGE.toString()
                    break
                case 2:  // Branch HEAD
                    strategyValue = ChangeRequestCheckoutStrategy.HEAD.toString()
                    break
                case 3:  // With strategy the child job name will have a {-merge/-head} suffix
                    strategyValue = env.JOB_NAME.split('-').last().toUpperCase()
                    break
                default: // Should never happen
                    strategyValue = "UNKNOWN"
            }
        }
    }
    return strategyValue
}
