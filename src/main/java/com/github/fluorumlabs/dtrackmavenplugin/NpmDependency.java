/*
 * Copyright 2021 Artem Godin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.fluorumlabs.dtrackmavenplugin;

import com.github.fluorumlabs.dtrackmavenplugin.engine.NpmReactor;
import io.github.classgraph.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

@Data
@NoArgsConstructor
public class NpmDependency {
    /**
     * Explicit NPM dependency package name
     */
    @Parameter
    private String packageName;

    /**
     * Explicit NPM dependency version
     */
    @Parameter
    private String version;

    /**
     * Annotation class name to extract NPM dependency information.
     */
    @Parameter
    private String annotationClassName;

    /**
     * Annotation field name holding NPM package name.
     */
    @Parameter
    private String annotationPackageNameField;

    /**
     * Annotation field name holding NPM package version.
     */
    @Parameter
    private String annotationVersionField;

    /**
     * Declaring class name of a static method without parameters returning
     * Map&lt;String,String&gt; with package names as a keys and versions as a
     * values.
     */
    @Parameter
    private String staticMethodClassName;

    /**
     * Name of static method without parameters returning Map&lt;String,String&gt;
     * with package names as a keys and versions as a values.
     */
    @Parameter
    private String staticMethodName;

    public boolean addToReactor(URL[] classLoaderURLs, Log logger, NpmReactor reactor) {
        if (isPackageReference()) {
            reactor.addDependency(packageName, version);
            return true;
        } else if (isAnnotationReference()) {
            return scanAnnotations(classLoaderURLs, reactor);
        } else if (isStaticMethodReference()) {
            return invokeMethod(classLoaderURLs, logger, reactor);
        }
        return false;
    }

    private boolean isPackageReference() {
        return packageName != null && version != null;
    }

    private boolean isAnnotationReference() {
        return annotationClassName != null && annotationPackageNameField != null && annotationVersionField != null;
    }

    private boolean isStaticMethodReference() {
        return staticMethodClassName != null && staticMethodName != null;
    }

    @SuppressWarnings("unchecked")
    private boolean invokeMethod(URL[] classLoaderURLs, Log logger, NpmReactor npmReactor) {
        boolean wasAdded = false;
        try (URLClassLoader classLoader = new URLClassLoader(classLoaderURLs, getClass().getClassLoader())) {
            Class<?> aClass = classLoader.loadClass(staticMethodClassName);
            Method declaredMethod = aClass.getDeclaredMethod(staticMethodName);
            declaredMethod.setAccessible(true);
            Object result = declaredMethod.invoke(null);
            if (result == null) {
                logger.error("Method returned null: " + staticMethodClassName + "." + staticMethodName + "()");
                return false;
            }
            if (result instanceof Map) {
                Map<String, String> mappedResult = (Map<String, String>) result;
                for (Map.Entry<String, String> stringStringEntry : mappedResult.entrySet()) {
                    if (stringStringEntry.getKey() instanceof String
                            && stringStringEntry.getValue() instanceof String) {
                        npmReactor.addDependency(stringStringEntry.getKey(), stringStringEntry.getValue());
                        wasAdded = true;
                    } else {
                        logger.error("Method " + staticMethodClassName + "." + staticMethodName
                                + " returned unsupported value " + stringStringEntry.getKey() + " -> "
                                + stringStringEntry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error invoking " + staticMethodClassName + "." + staticMethodName + "()", e);
        }
        return wasAdded;
    }

    private boolean scanAnnotations(URL[] classLoaderURLs, NpmReactor npmReactor) {
        boolean wasAdded = false;
        try (ScanResult scanResult = new ClassGraph().enableAllInfo().acceptPackages("*")
                .overrideClassLoaders(new URLClassLoader(classLoaderURLs)).scan()) {
            ClassInfoList classesWithAnnotation = scanResult.getClassesWithAnnotation(annotationClassName);
            for (ClassInfo classInfo : classesWithAnnotation) {
                for (AnnotationInfo annotationInfo : classInfo.getAnnotationInfoRepeatable(annotationClassName)) {
                    AnnotationParameterValueList parameterValues = annotationInfo.getParameterValues();
                    String actualPackageName = (String) parameterValues.getValue(annotationPackageNameField);
                    String actualVersion = (String) parameterValues.getValue(annotationVersionField);
                    if (actualPackageName != null && actualVersion != null && !actualPackageName.isEmpty()
                            && !actualVersion.isEmpty()) {
                        npmReactor.addDependency(actualPackageName, actualVersion);
                        wasAdded = true;
                    }
                }
            }
        }
        return wasAdded;
    }

}
