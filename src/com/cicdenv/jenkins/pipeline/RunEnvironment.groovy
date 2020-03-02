package com.cicdenv.jenkins.pipeline

/**
 * Currently executing job run, Github, and infra environment.
 */
class RunEnvironment {
    static Boolean configured // Whether or not the *Environment values were set

    static String organization   // Github Organization
    static String repository     // Github Repo name
    static String childName      // <branch-name> / PR-<N>
    static String safeChildName  // <branch-name> with char subs (safe docker tag value)
    static String ref            // Github reference (branch, PR{head|merge}, sha1, tag)

    static String serverUrl     // Jenkins server external URL
    static String cacheS3bucket // AWS S3 upstream cache bucket name
    static String cacheS3SubDir // AWS S3 upstream cache bucket sub-folder
}
