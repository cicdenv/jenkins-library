package com.cicdenv.jenkins.pipeline

/**
 * Currently executing job run Jenkins service host environment.
 */
class HostEnvironment {
    static final String homeDir = '/var/lib/jenkins'
    static final String cacheDir = "${homeDir}/cache"
    static final String sshDir = "${homeDir}/.ssh"

    static final String  jenkinsUser    = 'jenkins'
    static final Integer jenkinsUserId  = 8008
    static final Integer jenkinsGroupId = 8008
    static final Integer dockerGroupId  = 8088

    static String workspacesRoot
    static String workspaceDir

    static serviceSettings = [
        jenkinsUser:    jenkinsUser,
        jenkinsUserId:  jenkinsUserId,
        jenkinsGroupId: jenkinsGroupId,
        homeDir:        homeDir,
        dockerGroupId:  dockerGroupId,
    ]
}
