# Jenkins Service Pipeline Global Libary
Implicit Global Library for Jenkins 2 Declarative Pipelines (Jenkinsfile).

Expected by Jenkinsfile(s) / standalone pipeline scripts.

## Online Help
* Any pipeline job config landing page:
  * "pipeline-syntax" link
  * "Global Variables Reference" link
  * Search for cicdenv, these are the `vars/*.txt` html snippets

## Terminology
* "tools container" - container for running build job stages/steps 
  * Github repo specific
* "jenkins environment" - ci/cd environment customizations
  * infrastructure / dedicated EC2 host dependent settings

## Tests
The 'test' jenkins instance is configured differently in 
[configureGlobalSharedPipelineLibraries.groovy](https://github.com/vogtech/cicdenv/jenkins/server-image/init-scripts/configureGlobalSharedPipelineLibraries.groovy) in the following ways:
* this global library is not implicitly loaded
* this global library can be included at a version different than the default (master branch)

PRs against this repo can be vetted using Multi-branch pipelines.
* [Jenkinsfile](Jenkinsfile)

A "pipeline script" job can be run to test "standalone" pipeline script compatibility.
* [TestPipelineScript.groovy](TestPipelineScript.groovy)

## Links
* https://jenkins.io/doc/book/pipeline/shared-libraries/
  * https://jenkins.io/doc/book/pipeline/shared-libraries/#using-third-party-libraries
* [docs](https://github.com/vogtech/jenkins-global-library/wiki)
