
def label = "worker-${UUID.randomUUID().toString()}"
def image

openshift.withCluster() {
    openshift.withProject() {
        echo "Using project ${openshift.project()} in cluster with url ${openshift.cluster()}"
        // echo openshift.selector("istag", "jenkins-slave-base-rhel7:v3.9").object().image.dockerImageReference
        image = openshift.selector("istag", "jenkins-slave-base-rhel7:latest").object().image.dockerImageReference
    }
}


podTemplate(
    cloud: "openshift",
    label: label,
    containers: [
        containerTemplate(
            name: 'worker',
            image: image,
            resourceRequestMemory: "512Mi",
            resourceLimitMemory: "1Gi"
        )
    ],
    volumes: [ 
        secretSecretVolumeVolume(secretName: 'rbo-demo-demo-auth', mountPath: '/quay/')
    ]
){node(label){
    stages {
        stage('Playground') {
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
}}