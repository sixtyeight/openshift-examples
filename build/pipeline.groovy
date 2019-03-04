
def labelName = "worker-${UUID.randomUUID().toString()}"
def image

openshift.withCluster() {
    openshift.withProject() {
        echo "Using project ${openshift.project()} in cluster with url ${openshift.cluster()}"
        // echo openshift.selector("istag", "jenkins-slave-base-rhel7:v3.9").object().image.dockerImageReference
        image = openshift.selector("istag", "jenkins-slave-image-mgmt:latest").object().image.dockerImageReference
    }
}


podTemplate(
    cloud: "openshift",
    label: labelName,
    inheritFrom: "maven", 
    containers: [
        containerTemplate(
            name: 'worker',
            image: image,
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
        label "maven"
    }
    stages {
        stage('Playground') {    
            agent {
                label labelName
            }
            steps {
                sh """

                set +x

                find -ls /quay
                ls -la /quay/*
                """
            }
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
    }
}