/*
 * Copyright 2021 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

// postcommit-tests.groovy

import com.forgerock.pipeline.reporting.PipelineRunLegacyAdapter

void runStage(PipelineRunLegacyAdapter pipelineRun, Random random, boolean generateSummaryReport) {

    def stageName = 'POSTCOMMIT-ALL-TESTS'
    def clusterConfig = [:]
    clusterConfig['PROJECT'] = cloud_config.commonConfig()['PROJECT']
    clusterConfig['CLUSTER_DOMAIN'] = 'postcommit-forgeops.engineeringpit.com'
    def scaleClusterConfig = [:]
    scaleClusterConfig['SCALE_CLUSTER'] = ['frontend-pool': 4, 'primary-pool': 6]

    try {
        node('forgeops-postcommit-cloud') {
            cloud_utils.scaleClusterUp(clusterConfig + scaleClusterConfig)
        }

        def upgradeImageLevel = 'pit1'
        def parallelTestsMap = [:]

        // **************
        // DEV full tests
        // **************
        if (params.Postcommit_pit1) {
            parallelTestsMap.put('PIT1',
                    {
                        commonLodestarModule.runSpyglaas(pipelineRun, random, 'PIT1', clusterConfig +
                                [TESTS_SCOPE: 'tests/pit1']
                        )
                    }
            )
        }

        if (params.Postcommit_perf_postcommit) {
            parallelTestsMap.put('Perf Postcommit',
                    {
                        commonLodestarModule.runPyrock(pipelineRun, random, 'Perf Postcommit', clusterConfig +
                                [TEST_NAME      : 'postcommit',
                                 PROFILE_NAME   : 'small']
                        )
                    }
            )
        }

        if (params.Postcommit_perf_restore) {
            parallelTestsMap.put('Perf Restore',
                    {
                        commonLodestarModule.runPyrock(pipelineRun, random, 'Perf Restore', clusterConfig +
                                [TEST_NAME                      : 'platform',
                                 PROFILE_NAME                   : 'small',
                                 CONFIGFILE_NAME                : 'conf-postcommit-restore-100k.yaml',
                                 DEPLOYMENT_RESTORE_BUCKET_URL  : 'gs://performance-bucket-us-east1/postcommit/idrepo-100k']
                        )
                    }
            )
        }

        // *************
        // DEV k8s tests
        // *************
        if (params.Postcommit_am_k8s_postcommit) {
            parallelTestsMap.put('AM K8s Postcommit',
                    {
                        commonLodestarModule.runSpyglaas(pipelineRun, random, 'AM K8s Postcommit', clusterConfig +
                                [TESTS_SCOPE: 'tests/k8s/postcommit/am']
                        )
                    }
            )
        }

        if (params.Postcommit_am_k8s_upgrade) {
            parallelTestsMap.put('AM K8s Upgrade',
                    {
                        commonLodestarModule.runSpyglaas(pipelineRun, random, 'AM K8s Upgrade', clusterConfig +
                                [TESTS_SCOPE                           : 'tests/k8s/postcommit/am',
                                 DEPLOYMENT_UPGRADE_FIRST              : true,
                                 COMPONENTS_AM_IMAGE_TAG               : commonLodestarModule.productPostcommitStable,
                                 COMPONENTS_AM_IMAGE_REPOSITORY        : 'gcr.io/engineeringpit/lodestar-images/am',
                                 COMPONENTS_AMSTER_IMAGE_TAG           : commonLodestarModule.productPostcommitStable,
                                 COMPONENTS_AMSTER_IMAGE_REPOSITORY    : 'gcr.io/engineeringpit/lodestar-images/amster',
                                 COMPONENTS_AM_IMAGE_UPGRADE_TAG       : commonLodestarModule.productLatestTag,
                                 COMPONENTS_AM_IMAGE_UPGRADE_REPOSITORY: "gcr.io/forgerock-io/am-base/${upgradeImageLevel}"]
                        )
                    }
            )
        }
        if (params.Postcommit_am_basic_perf) {
            parallelTestsMap.put('AM Basic Perf',
                    {
                        commonLodestarModule.runPyrock(pipelineRun, random, 'AM Basic Perf', clusterConfig +
                                [TEST_NAME      : 'am',
                                 PROFILE_NAME   : 'am-only']
                        )
                    }
            )
        }

        if (params.Postcommit_ds_k8s_postcommit) {
            parallelTestsMap.put('DS K8s Postcommit',
                    {
                        commonLodestarModule.runSpyglaas(pipelineRun, random, 'DS K8s Postcommit', clusterConfig +
                                [TESTS_SCOPE: 'tests/k8s/postcommit/ds']
                        )
                    }
            )
        }
        if (params.Postcommit_ds_k8s_upgrade) {
            parallelTestsMap.put('DS K8s Upgrade',
                    {
                        commonLodestarModule.runSpyglaas(pipelineRun, random, 'DS K8s Upgrade', clusterConfig +
                                [TESTS_SCOPE                                 : 'tests/k8s/postcommit/ds/standard',
                                 DEPLOYMENT_UPGRADE_FIRST                    : true,
                                 COMPONENTS_DSIDREPO_IMAGE_TAG               : commonLodestarModule.productPostcommitStable,
                                 COMPONENTS_DSIDREPO_IMAGE_REPOSITORY        : 'gcr.io/engineeringpit/lodestar-images/ds-idrepo',
                                 COMPONENTS_DSIDREPO_IMAGE_UPGRADE_TAG       : commonLodestarModule.productLatestTag,
                                 COMPONENTS_DSIDREPO_IMAGE_UPGRADE_REPOSITORY: "gcr.io/forgerock-io/ds/${upgradeImageLevel}",
                                 COMPONENTS_DSCTS_IMAGE_TAG                  : commonLodestarModule.productPostcommitStable,
                                 COMPONENTS_DSCTS_IMAGE_REPOSITORY           : 'gcr.io/engineeringpit/lodestar-images/ds-cts',
                                 COMPONENTS_DSCTS_IMAGE_UPGRADE_TAG          : commonLodestarModule.productLatestTag,
                                 COMPONENTS_DSCTS_IMAGE_UPGRADE_REPOSITORY   : "gcr.io/forgerock-io/ds/${upgradeImageLevel}"]
                        )
                    }
            )
        }
        if (params.Postcommit_ds_basic_perf) {
            parallelTestsMap.put('DS Basic Perf',
                    {
                        commonLodestarModule.runPyrock(pipelineRun, random, 'DS Basic Perf', clusterConfig +
                                [TEST_NAME      : 'ds',
                                 PROFILE_NAME   : 'ds-only']
                        )
                    }
            )
        }

        if (params.Postcommit_idm_k8s_postcommit) {
            parallelTestsMap.put('IDM K8s Postcommit',
                    {
                        commonLodestarModule.runSpyglaas(pipelineRun, random, 'IDM K8s Postcommit', clusterConfig +
                                [TESTS_SCOPE: 'tests/k8s/postcommit/idm',]
                        )
                    }
            )
        }
        if (params.Postcommit_idm_k8s_upgrade) {
            parallelTestsMap.put('IDM K8s Upgrade',
                    {
                        commonLodestarModule.runSpyglaas(pipelineRun, random, 'IDM K8s Upgrade', clusterConfig +
                                [TESTS_SCOPE                            : 'tests/k8s/postcommit/idm',
                                 DEPLOYMENT_UPGRADE_FIRST               : true,
                                 COMPONENTS_IDM_IMAGE_TAG               : commonLodestarModule.productPostcommitStable,
                                 COMPONENTS_IDM_IMAGE_REPOSITORY        : 'gcr.io/engineeringpit/lodestar-images/idm',
                                 COMPONENTS_IDM_IMAGE_UPGRADE_TAG       : commonLodestarModule.productLatestTag,
                                 COMPONENTS_IDM_IMAGE_UPGRADE_REPOSITORY: "gcr.io/forgerock-io/idm/${upgradeImageLevel}"]
                        )
                    }
            )
        }
        if (params.Postcommit_idm_basic_perf) {
            parallelTestsMap.put('IDM Basic Perf',
                    {
                        commonLodestarModule.runPyrock(pipelineRun, random, 'IDM Basic Perf', clusterConfig +
                                [TEST_NAME      : 'idm_only',
                                 PROFILE_NAME   : 'idm-only']
                        )
                    }
            )
        }

        if (params.Postcommit_ig_k8s_postcommit) {
            parallelTestsMap.put('IG K8s Postcommit',
                    {
                        commonLodestarModule.runSpyglaas(pipelineRun, random, 'IG K8s Postcommit', clusterConfig +
                                [TESTS_SCOPE: 'tests/k8s/postcommit/ig']
                        )
                    }
            )
        }
        if (params.Postcommit_ig_k8s_upgrade) {
            parallelTestsMap.put('IG K8s Upgrade',
                    {
                        commonLodestarModule.runSpyglaas(pipelineRun, random, 'IG K8s Upgrade', clusterConfig +
                                [TESTS_SCOPE                           : 'tests/k8s/postcommit/ig',
                                 DEPLOYMENT_UPGRADE_FIRST              : true,
                                 COMPONENTS_IG_IMAGE_TAG               : commonLodestarModule.productPostcommitStable,
                                 COMPONENTS_IG_IMAGE_REPOSITORY        : 'gcr.io/engineeringpit/lodestar-images/ig',
                                 COMPONENTS_IG_IMAGE_UPGRADE_TAG       : commonLodestarModule.productLatestTag,
                                 COMPONENTS_IG_IMAGE_UPGRADE_REPOSITORY: "gcr.io/forgerock-io/ig/${upgradeImageLevel}"]
                        )
                    }
            )
        }
        if (params.Postcommit_ig_basic_perf) {
            parallelTestsMap.put('IG Basic Perf',
                    {
                        commonLodestarModule.runPyrock(pipelineRun, random, 'IG Basic Perf', clusterConfig +
                                [TEST_NAME      : 'ig',
                                 PROFILE_NAME   : 'ig-only']
                        )
                    }
            )
        }

        if (params.Postcommit_platform_ui) {
            parallelTestsMap.put('Platform UI',
                    {
                        commonLodestarModule.runPlatformUi(pipelineRun, random, 'Platform UI', clusterConfig +
                                [TESTS_SCOPE: 'tests/k8s/postcommit/platform_ui',
                                 SKIP_TESTS                          : true,
                                 SKIP_CLEANUP                        : true,
                                 DEPLOYMENT_USE_LODESTAR_CERT        : true]
                        )
                    }
            )
        }
        if (params.Postcommit_set_images) {
            parallelTestsMap.put('Set Images',
                    {
                        commonLodestarModule.runSpyglaas(pipelineRun, random, 'Set Images', clusterConfig +
                                [TESTS_SCOPE                    : 'tests/set_images',
                                 STASH_PLATFORM_IMAGES_BRANCH   : 'postcommit-forgeops']
                        )
                    }
            )
        }

        parallel parallelTestsMap
    } catch (Exception exception) {
        println("Exception during parallel stage: ${exception}")
        throw exception
    } finally {
        if (generateSummaryReport) {
            commonLodestarModule.generateSummaryTestReport(stageName)
        }

        node('forgeops-postcommit-cloud') {
            cloud_utils.scaleClusterDown(clusterConfig + scaleClusterConfig)
        }
    }
}

return this
