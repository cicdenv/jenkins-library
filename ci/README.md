## Purpose
Continous Integration (ci) setup for this repo.

## Job Config
This is intended for the special `test` Jenkins instance.

```
New Item (top-level)
* vogtech (org) folder
  New Item
  * Multibranch Pipeline
  `jenkins-global-library`
  - Branch Sources
      Github branch source
      https://github.com/vogtech/jenkins-global-library.git
  - Build Configuration:
      Discover branches: "Exclude branches that are also files as PRs"
      Discover pull requests from origin: "The current pull request revision"
      [x] Remove Discover pull reqeusts from forks    
      [+] Filter by name including PRs destined for this branch (with wildcards)
          master
    Script Path: ci/Jenkinsfile
```

## Webhook
GitHub / Override Hook URL:
```
https://jenkins-<instance>.<account>.cicdenv.com/github-webhook/
```
