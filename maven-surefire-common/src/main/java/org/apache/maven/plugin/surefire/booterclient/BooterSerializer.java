package org.apache.maven.plugin.surefire.booterclient;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.maven.surefire.booter.BooterConstants;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SystemPropertyManager;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.RunOrder;

/**
 * Knows how to serialize and deserialize the booter configuration.
 * <p/>
 * The internal serialization format is through a properties file. The long-term goal of this
 * class is not to expose this implementation information to its clients. This still leaks somewhat,
 * and there are some cases where properties are being accessed as "Properties" instead of
 * more representative domain objects.
 * <p/>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Brett Porter
 * @author Dan Fabulich
 * @author Kristian Rosenvold
 * @version $Id$
 */
class BooterSerializer
{
    private final ForkConfiguration forkConfiguration;

    private final PropertiesWrapper properties;

    public BooterSerializer( ForkConfiguration forkConfiguration, Properties properties )
    {
        this.forkConfiguration = forkConfiguration;
        this.properties = new PropertiesWrapper( properties );
    }


    public File serialize( ProviderConfiguration booterConfiguration, StartupConfiguration providerConfiguration,
                           Object testSet, String forkMode )
        throws IOException
    {
        providerConfiguration.getClasspathConfiguration().setForkProperties( properties );

        TestArtifactInfo testNg = booterConfiguration.getTestArtifact();
        if ( testNg != null )
        {
            properties.setProperty( BooterConstants.TESTARTIFACT_VERSION, testNg.getVersion() );
            properties.setProperty( BooterConstants.TESTARTIFACT_CLASSIFIER, testNg.getClassifier() );
        }

        properties.setProperty( BooterConstants.FORKTESTSET, getTypeEncoded( testSet ) );
        TestRequest testSuiteDefinition = booterConfiguration.getTestSuiteDefinition();
        if ( testSuiteDefinition != null )
        {
            properties.setProperty( BooterConstants.SOURCE_DIRECTORY, testSuiteDefinition.getTestSourceDirectory() );
            properties.addList( testSuiteDefinition.getSuiteXmlFiles(), BooterConstants.TEST_SUITE_XML_FILES );
            properties.setProperty( BooterConstants.REQUESTEDTEST, testSuiteDefinition.getRequestedTest() );
            properties.setProperty( BooterConstants.REQUESTEDTESTMETHOD, testSuiteDefinition.getRequestedTestMethod() );
        }

        DirectoryScannerParameters directoryScannerParameters = booterConfiguration.getDirScannerParams();
        if ( directoryScannerParameters != null )
        {
            properties.setProperty( BooterConstants.FAILIFNOTESTS,
                                    String.valueOf( directoryScannerParameters.isFailIfNoTests() ) );
            properties.addList( directoryScannerParameters.getIncludes(), BooterConstants.INCLUDES_PROPERTY_PREFIX );
            properties.addList( directoryScannerParameters.getExcludes(), BooterConstants.EXCLUDES_PROPERTY_PREFIX );
            properties.setProperty( BooterConstants.TEST_CLASSES_DIRECTORY,
                                    directoryScannerParameters.getTestClassesDirectory() );
        }

        final RunOrderParameters runOrderParameters = booterConfiguration.getRunOrderParameters();
        if ( runOrderParameters != null )
        {
            properties.setProperty( BooterConstants.RUN_ORDER, RunOrder.asString( runOrderParameters.getRunOrder() ) );
            properties.setProperty( BooterConstants.RUN_STATISTICS_FILE, runOrderParameters.getRunStatisticsFile() );
        }

        ReporterConfiguration reporterConfiguration = booterConfiguration.getReporterConfiguration();

        Boolean rep = reporterConfiguration.isTrimStackTrace();
        properties.setProperty( BooterConstants.ISTRIMSTACKTRACE, rep );
        properties.setProperty( BooterConstants.REPORTSDIRECTORY, reporterConfiguration.getReportsDirectory() );
        properties.setProperty( BooterConstants.FORKMODE, forkMode );
        ClassLoaderConfiguration classLoaderConfiguration = providerConfiguration.getClassLoaderConfiguration();
        properties.setProperty( BooterConstants.USESYSTEMCLASSLOADER,
                                String.valueOf( classLoaderConfiguration.isUseSystemClassLoader() ) );
        properties.setProperty( BooterConstants.USEMANIFESTONLYJAR,
                                String.valueOf( classLoaderConfiguration.isUseManifestOnlyJar() ) );
        properties.setProperty( BooterConstants.FAILIFNOTESTS,
                                String.valueOf( booterConfiguration.isFailIfNoTests() ) );
        properties.setProperty( BooterConstants.PROVIDER_CONFIGURATION, providerConfiguration.getProviderClassName() );

        return SystemPropertyManager.writePropertiesFile( properties.getProperties(),
                                                          forkConfiguration.getTempDirectory(), "surefire",
                                                          forkConfiguration.isDebug() );
    }


    private String getTypeEncoded( Object value )
    {
        if ( value == null )
        {
            return null;
        }
        String valueToUse;
        if ( value instanceof Class )
        {
            valueToUse = ( (Class) value ).getName();
        }
        else
        {
            valueToUse = value.toString();
        }
        return value.getClass().getName() + "|" + valueToUse;
    }

}
