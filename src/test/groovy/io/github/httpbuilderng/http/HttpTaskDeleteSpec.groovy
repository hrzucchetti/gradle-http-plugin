/*
 * Copyright (C) 2021 HttpBuilder-NG Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.httpbuilderng.http


import com.stehno.gradle.testing.GradleBuild
import io.github.cjstehno.ersatz.ErsatzServer
import org.gradle.testkit.runner.BuildResult
import org.junit.Rule
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Unroll

class HttpTaskDeleteSpec extends Specification {

    @Rule
    GradleBuild gradle = new GradleBuild(
            template: '''
            plugins {
                id 'io.github.http-builder-ng.http-plugin'
            }
            repositories {
                jcenter()
            }
            
            import groovyx.net.http.HttpConfig
            import java.util.function.Consumer
            
            ${config.globalConfig ?: ''}
            
            task makeRequest(type:io.github.httpbuilderng.http.HttpTask){
                ${config.taskConfig ?: ''}
            }
        '''
    )

    @AutoCleanup(value = 'stop')
    private ErsatzServer ersatz = new ErsatzServer()

    def 'single DELETE request'() {
        setup:
        ersatz.expectations {
            delete('/notify').called(1).responds().code(204)
        }

        gradle.buildFile(taskConfig: """
            config {
                request.uri = '${ersatz.httpUrl}'
            }
            delete {
                request.uri.path = '/notify'
                response.when(204) { 
                    println 'I have arrived!'
                }
            }
        """)

        when:
        BuildResult result = gradle.runner('makeRequest').build()

        then:
        GradleBuild.totalSuccess result

        and:
        GradleBuild.textContainsLines result.output, ['I have arrived!']

        and:
        ersatz.verify()
    }

    def 'multiple DELETE requests'() {
        setup:
        ersatz.expectations {
            delete('/notify').called(3).responds().code(204)
        }

        gradle.buildFile(taskConfig: """
            config {
                request.uri = '${ersatz.httpUrl}'
                response.when(204){ 
                    println 'I have arrived!' 
                }
            }
            deleteAsync {
                request.uri.path = '/notify'
            }
            delete {
                request.uri.path = '/notify'
            }
            delete {
                request.uri.path = '/notify'
            }
        """)

        when:
        BuildResult result = gradle.runner('makeRequest').build()

        then:
        GradleBuild.totalSuccess result

        and:
        GradleBuild.textContainsLines result.output, ['I have arrived!']

        and:
        ersatz.verify()
    }

    def 'multiple DELETE requests (consumer)'() {
        setup:
        ersatz.expectations {
            delete('/notify').called(2).responds().code(204)
        }

        gradle.buildFile(taskConfig: """
            config {
                request.uri = '${ersatz.httpUrl}'
                response.when(204){ 
                    println 'I have arrived!' 
                }
            }
            deleteAsync(new Consumer<HttpConfig>() {
                @Override void accept(HttpConfig cfg) {
                    cfg.request.uri.path = '/notify'
                }
            })
            delete(new Consumer<HttpConfig>() {
                @Override void accept(HttpConfig cfg) {
                    cfg.request.uri.path = '/notify'
                }
            })
        """)

        when:
        BuildResult result = gradle.runner('makeRequest').build()

        then:
        GradleBuild.totalSuccess result

        and:
        GradleBuild.textContainsLines result.output, ['I have arrived!']

        and:
        ersatz.verify()
    }

    @Unroll
    'single DELETE request (external config with #library)'() {
        setup:
        ersatz.expectations {
            delete('/notify').called(1).responds().code(204)
        }

        gradle.buildFile(
                globalConfig: """
                http {
                    library = $library
                    config {
                        request.uri = '${ersatz.httpUrl}'
                    }
                }
            """,
                taskConfig: """
            delete {
                request.uri.path = '/notify'
                response.when(204){ 
                    println 'I have arrived!' 
                }
            }
        """)

        when:
        BuildResult result = gradle.runner('makeRequest').build()

        then:
        GradleBuild.totalSuccess result

        and:
        GradleBuild.textContainsLines result.output, ['I have arrived!']

        and:
        ersatz.verify()

        where:
        library << [
                "io.github.httpbuilderng.http.HttpLibrary.CORE",
                "io.github.httpbuilderng.http.HttpLibrary.APACHE",
                "io.github.httpbuilderng.http.HttpLibrary.OKHTTP",
                "'core'",
                "'apache'",
                "'okhttp'",
        ]
    }
}
