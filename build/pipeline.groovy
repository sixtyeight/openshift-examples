def namespace, appReleaseTag, webReleaseTag, prodCluster, prodProject, prodToken
def label = "worker-${UUID.randomUUID().toString()}"
podTemplate(
    label: label,

    containers: [
        containerTemplate(
            name: 'worker',
            image: openshift.selector("istag", "jenkins-slave-base-rhel7:latest").object().image.dockerImageReference
            resourceRequestMemory: "512Mi",
            resourceLimitMemory: "1Gi"
        )
    ],
    volumes: [ 
        secretVolume(secretName: 'rbo-demo-demo-auth', mountPath: '/quay/')
    ]
)

pipeline {
    agent {
        label: label
    }
    stages {
        stage('Playground') {
            sh """

            set +x

            find -ls /quay
            ls -la /quay/*
            """
        }

        stage('Start build') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject() {
                            echo "Using project ${openshift.project()} in cluster with url ${openshift.cluster()}"
                            // echo openshift.selector("istag", "jenkins-slave-base-rhel7:v3.9").object().image.dockerImageReference
                            def buildSelector = openshift.selector("bc", "simple-http-server").startBuild()
                            buildSelector.logs('-f')
                        }
                    }
                }
            }
        }
    
        stage('Copy to Cluster A') {
            steps {
            }
        }
    }
}